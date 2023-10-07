package com.darcangel.tcamViewer.ui.library;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;
import com.darcangel.tcamViewer.constants.Constants;
import com.darcangel.tcamViewer.databinding.FragmentPlaybackBinding;
import com.darcangel.tcamViewer.factory.PaletteFactory;
import com.darcangel.tcamViewer.model.ImageDto;
import com.darcangel.tcamViewer.model.LibraryViewModel;
import com.darcangel.tcamViewer.model.RecordingDto;
import com.darcangel.tcamViewer.model.RecordingFooterDto;
import com.darcangel.tcamViewer.model.Settings;
import com.darcangel.tcamViewer.utils.BitmapToVideoEncoder;
import com.darcangel.tcamViewer.utils.CameraUtils;

import org.jcodec.api.android.AndroidSequenceEncoder;
import org.jcodec.common.io.SeekableByteChannel;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;

import io.sentry.Sentry;
import timber.log.Timber;

public class PlaybackFragment extends Fragment implements
        MenuProvider,
        View.OnTouchListener {
    private String filename;
    private ImageDto[] imageDtos;
    private int frameIndex;
    private long bytesRead;
    private RecordingFooterDto recordingFooterDto;
    private Timer imageTimer;
    private Handler imageTimerHandler;
    private Scanner scanner;
    private BufferedReader bufferedReader;
    private BufferedReader bufferedInfoReader;
    private InputStreamReader recodingFile;
    private ObjectInputStream infoInputStream;
    private MainActivity mainActivity;
    private CameraUtils cameraUtils;
    private FragmentPlaybackBinding binding;
    private View root;
    private LibraryViewModel libraryViewModel;
    private Settings settings;
    private PaletteFactory paletteFactory;
    private RecordingDto recordingDto;
    private Integer numFrames;
    private long fileSize;
    private final char[] buffer = new char[64767];
    private ArrayList<Pair<Bitmap, Integer>> movieInfoArray;
    private SeekableByteChannel out = null;
    private AndroidSequenceEncoder encoder;
    private File videoFile = null;
    private String videoFilename = null;
    private Integer action;
    private BitmapToVideoEncoder videoEncoder;
    private ArrayList<Pair<Bitmap, Integer>> videoFrameArray;
    private String currentPalette = "Rainbow";
    private boolean remapNeeded = false;
    private AtomicBoolean abortPlayback;

    private Runnable playImage = new Runnable() {
        @Override
        public void run() {
            try {
                int nFrames;
                if (videoFrameArray == null) {
                    numFrames = libraryViewModel.getRecordingDto().getNumFrames();
                    videoFrameArray = new ArrayList<Pair<Bitmap, Integer>>(numFrames);
                }
                int len = libraryViewModel.getFrameSize().get(frameIndex);
                int bytesRead = recodingFile.read(buffer, 0, len);
                assert bytesRead == len;
                ImageDto imageDto = new ImageDto(new JSONObject(String.copyValueOf(buffer, 0, len)),
                        currentPalette);
                if (remapNeeded) {
                    int[][] palette = paletteFactory.getPaletteByName(currentPalette);
                    imageDto.setPalette(palette);
                    imageDto.remapImage();
                }
                if (action == Constants.PLAYBACK_ACTION_PLAY) {
                    displayImage(imageDto);
                } else if (action == Constants.PLAYBACK_ACTION_ANALYZE) {

                } else if (action == Constants.PLAYBACK_ACTION_SAVE && videoEncoder != null) {
                    Long delay = libraryViewModel.getFrameDelay().get(frameIndex);
                    nFrames = (int) (((float) delay / 1000.0) * 30.0);
                    nFrames = nFrames == 0 ? 1 : nFrames;
                    Bitmap bitmap = imageDto.getBitmap();
                    Timber.d("encoding nFrames = %d", nFrames);
                    videoFrameArray.add(new Pair<Bitmap, Integer>(bitmap, nFrames));
                }
                recodingFile.skip(1); //skip over \03
                frameIndex = frameIndex + 1;
                if (frameIndex < numFrames) {
                    if (abortPlayback.get()) {
                        abortPlayback.set(false);
                        return;
                    }
                    if (action == Constants.PLAYBACK_ACTION_PLAY) {
                        imageTimerHandler.postDelayed(this, libraryViewModel.getFrameDelay().get(frameIndex - 1));
                    } else {
                        imageTimerHandler.postDelayed(this, 10);
                    }
                } else {
                    imageTimerHandler.removeCallbacks(this);
                    if (action == Constants.PLAYBACK_ACTION_SAVE && videoEncoder != null) {
                        for (Pair<Bitmap, Integer> pair : videoFrameArray) {
                            for (int i = 0; i < pair.second; i++) {
                                videoEncoder.queueFrame(pair.first);
                            }
                        }
                        videoEncoder.stopEncoding();
                    }
                    if (recodingFile != null) {
                        recodingFile.close();
                        recodingFile = null;
                    }
                }
            } catch (JSONException | IOException e) {
                e.printStackTrace();
                Sentry.captureException(e);
                if (action == Constants.PLAYBACK_ACTION_SAVE && videoEncoder != null) {
                    videoEncoder.abortEncoding();
                }
            }
        }
    };


    //N. B. The navigation bar is hidden in MainActivity

    public PlaybackFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle == null) {
            action = Constants.PLAYBACK_ACTION_PLAY;
        } else {
            action = bundle.getInt(Constants.PLAYBACK_ACTION, Constants.PLAYBACK_ACTION_PLAY);
        }
        mainActivity = MainActivity.getInstance();
        libraryViewModel = mainActivity.getLibraryViewModel();
        settings = mainActivity.getSettings();
        cameraUtils = mainActivity.getCameraUtils();
        frameIndex = 0;
        bytesRead = 0;
        //prime the pump
        filename = libraryViewModel.getPlaybackImageDto().getFilename();
        imageTimer = new Timer("imageTimer");
        imageTimerHandler = new Handler();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentPlaybackBinding.inflate(inflater, container, false);
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        paletteFactory = mainActivity.getPaletteFactory();
        abortPlayback = new AtomicBoolean(false);
        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.ivColorBar.setOnTouchListener(this);
        try {
            openRecordingFile();
            openInfoFile();
            if (recordingDto == null) {
                analyzeRecording();
                recordingFooterDto = new RecordingFooterDto(getFooterInfo());
                numFrames = recordingFooterDto.getNumFrames();
            } else {
                libraryViewModel.setRecordingDto(recordingDto);
            }
            if (action == Constants.PLAYBACK_ACTION_PLAY) {
                playRecording();
            } else if (action == Constants.PLAYBACK_ACTION_ANALYZE) {

            } else if (action == Constants.PLAYBACK_ACTION_SAVE) {
                saveRecording();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Sentry.captureException(e);
        } catch (IOException e) {
            e.printStackTrace();
            Sentry.captureException(e);
        } catch (JSONException e) {
            e.printStackTrace();
            Sentry.captureException(e);
        }
    }

    private void openRecordingFile() throws IOException {
        File file = new File(filename);
        fileSize = file.length();
        recodingFile = new InputStreamReader(new FileInputStream(file));
    }

    private void openInfoFile() {
        String infoFilename = filename.substring(0, filename.lastIndexOf(".")) + ".info";
        File file = new File(infoFilename);
        try {
            if (file.exists()) {
                String filename = file.getName();
                FileInputStream fos = new FileInputStream(infoFilename);
                infoInputStream = new ObjectInputStream(fos);
                recordingDto = (RecordingDto) infoInputStream.readObject();
                libraryViewModel.setRecordingDto(recordingDto);
                fos.close();
            } else {
                recordingDto = null;
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            Sentry.captureException(e);
        }
    }

    private void playRecording() throws IOException {
        frameIndex = 0;
        openRecordingFile();
        openInfoFile();
        imageTimerHandler.postDelayed(playImage, 500);
    }

    private void saveRecording() throws IOException {
        String videoFilename = filename.substring(filename.lastIndexOf("/") + 1);
        videoFilename = videoFilename.replace(".tmjsn", "");
        String videoOutputPath =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + "/" + videoFilename + ".mp4";
        mainActivity.showProgressDialog(getString(R.string.saving_movie) + " " + videoFilename + ".mp4");
        videoEncoder = new BitmapToVideoEncoder(new BitmapToVideoEncoder.IBitmapToVideoEncoderCallback() {
            @Override
            public void onEncodingComplete(File outputFile) {
                numFrames = null;
                for (Pair<Bitmap, Integer> pair : videoFrameArray) {
                    pair.first.recycle();
                }
                videoFrameArray = null;
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.dismissProgressDialog();
                        Navigation.findNavController(getView()).popBackStack();
                    }
                });
            }
        });

        videoEncoder.startEncoding(160, 120, new File(videoOutputPath));
        imageTimerHandler.postDelayed(playImage, 500);
    }

    private void analyzeRecording() throws IOException, JSONException {
        int c, bufferPos = 0;
        frameIndex = 0;
        bytesRead = 0;
        long currTime;
        libraryViewModel.setRecordingDto(new RecordingDto());
        libraryViewModel.getFrameOffset().add(frameIndex, bytesRead);
        //TODO use Scanner here
        while ((c = recodingFile.read()) != -1) {
            if (c == 3 && bytesRead < fileSize - 1) {
                ImageDto imageDto = new ImageDto(new JSONObject(String.copyValueOf(buffer, 0, bufferPos)),
                        "Rainbow");
                if (frameIndex == 0) {
                    libraryViewModel.getFrameDelay().add(frameIndex, imageDto.getCreationDate().getTime());
                } else {
                    if (imageDto.getCreationDate().getTime() <= libraryViewModel.getFrameDelay().get(frameIndex - 1)) {
                        Timber.d("creation time is less than last frame + delay");
                    }
                    libraryViewModel.getFrameDelay().add(frameIndex, imageDto.getCreationDate().getTime());
                    libraryViewModel.getFrameDelay().set(frameIndex - 1,
                            libraryViewModel.getFrameDelay().get(frameIndex) -
                                    libraryViewModel.getFrameDelay().get(frameIndex - 1));
                }

                imageDto = null;
                frameIndex = frameIndex + 1;
                libraryViewModel.getFrameOffset().add(frameIndex, bytesRead + 1); //skip over \03
                libraryViewModel.getFrameSize().add(frameIndex - 1, bufferPos);
                assert bufferPos == (int) (bytesRead -
                        (frameIndex == 0 ? 0 : (libraryViewModel.getFrameOffset().get(frameIndex - 1))));
                bufferPos = 0;
            } else {
                buffer[bufferPos] = (char) c;
                bufferPos = bufferPos + 1;
            }
            bytesRead = bytesRead + 1;
        }
        libraryViewModel.getFrameSize().add(frameIndex,
                (int) (bytesRead - libraryViewModel.getFrameOffset().get(frameIndex - 1)));
        numFrames = libraryViewModel.getFrameOffset().size() - 1; // less footer and final \03
        libraryViewModel.getFrameDelay().remove(numFrames - 1); //last value is for the footer
        Timber.d("read %d frames", frameIndex);
    }

    private JSONObject getFooterInfo() {
        JSONObject result = null;
        try {
            int footerIndex = numFrames;
            char[] footer = new char[libraryViewModel.getFrameSize().get(footerIndex)];
            File file = new File(filename);
            bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            bufferedReader.skip(libraryViewModel.getFrameOffset().get(footerIndex));
            bufferedReader.read(footer, 0, libraryViewModel.getFrameSize().get(footerIndex));
            String footerString = String.valueOf(footer);
            result = new JSONObject(footerString);
        } catch (IOException e) {
            e.printStackTrace();
            Sentry.captureException(e);
            result = null;
        } catch (JSONException e) {
            e.printStackTrace();
            Sentry.captureException(e);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                    bufferedReader = null;
                } catch (IOException e) {
                    e.printStackTrace();
                    Sentry.captureException(e);
                }
            }
        }
        return result;
    }

    private void displayImage(final ImageDto imageDto) {
        assert imageDto != null;
        Bitmap bitmap = imageDto.drawHotspot();
        imageDto.remapImage();
        binding.ivCamera.setImageBitmap(bitmap);
        binding.ivCamera.setTag(this);
        if (settings.getDisplaySpotmeter().getValue()) {
            binding.tvSpotmeterTemperature.setText(cameraUtils.createTemperatureString(imageDto.
                    getMeanTemperatureAtSpotmeter()));
        } else {
            binding.tvSpotmeterTemperature.setText("");
        }
        Bitmap colorbar = imageDto.createColorBar();
        binding.ivColorBar.setImageBitmap(colorbar);
        Pair<Float, Float> temps = imageDto.getTemperatures();
        if (imageDto.isAGC()) {
            binding.tvMaxTemperature.setText("AGC");
            binding.tvMinTemperature.setText("AGC");
        } else {
            binding.tvMaxTemperature.setText(cameraUtils.createTemperatureString(temps.second));
            binding.tvMinTemperature.setText(cameraUtils.createTemperatureString(temps.first));
        }
    }

    @Override
    public void onPrepareMenu(@NonNull Menu menu) {
        MenuProvider.super.onPrepareMenu(menu);
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {

    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();
        NavDirections navDirections;

        if (id == android.R.id.home) {
            abortPlayback.set(true);
            navDirections = PlaybackFragmentDirections.actionPlaybackFragmentToNavigationLibrarySlideShowFragment();
            mainActivity.getNavController().navigate(navDirections);
            return true;
        } else {
            return false;
        }
    }

    private static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    private static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        imageTimerHandler.removeCallbacks(playImage);
        if (videoEncoder != null && videoEncoder.isEncodingStarted()) {
            mainActivity.dismissProgressDialog();
            videoEncoder.abortEncoding();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int id = v.getId();
        if (id == R.id.ivColorBar) {
            int h = binding.ivColorBar.getHeight();
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getY() > (h / 2)) {
                    currentPalette = cameraUtils.getNextPalette(currentPalette, Constants.ROTATE_FORWARD);
                } else {
                    currentPalette = cameraUtils.getNextPalette(currentPalette, Constants.ROTATE_BACKWARD);
                }
                remapNeeded = true;
            }
        }
        return true;
    }
}
