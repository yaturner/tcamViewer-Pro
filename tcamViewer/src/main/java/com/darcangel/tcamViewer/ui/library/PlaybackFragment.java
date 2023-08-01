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
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavDirections;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.databinding.FragmentPlaybackBinding;
import com.darcangel.tcamViewer.model.ImageDto;
import com.darcangel.tcamViewer.model.RecordingDto;
import com.darcangel.tcamViewer.model.RecordingFooterDto;
import com.darcangel.tcamViewer.model.Settings;
import com.darcangel.tcamViewer.utils.CameraUtils;
import com.darcangel.tcamViewer.utils.FileUtils;

import org.jcodec.api.android.AndroidSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;
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
import java.util.Timer;

import io.sentry.Sentry;
import timber.log.Timber;

public class PlaybackFragment extends Fragment implements MenuProvider {
    private String filename;
    private ImageDto[] imageDtos;
    private int frameIndex;
    private long bytesRead;
    private RecordingFooterDto recordingFooterDto;
    private Timer imageTimer;
    private Handler imageTimerHandler;
    private BufferedReader bufferedReader;
    private BufferedReader bufferedInfoReader;
    private ObjectInputStream infoInputStream;
    private MainActivity mainActivity;
    private CameraUtils cameraUtils;
    private FragmentPlaybackBinding binding;
    private View root;
    private LibraryViewModel libraryViewModel;
    private Settings settings;
    private RecordingDto recordingDto;
    private int numFrames;
    private long fileSize;
    private final char[] buffer = new char[64767];
    private ArrayList<Pair<Bitmap, Integer>> movieInfoArray;
    private SeekableByteChannel out = null;
    private AndroidSequenceEncoder encoder;
    private File videoFile = null;
    private String videoFilename = null;

    private Runnable imagePlayer = new Runnable() {
        @Override
        public void run() {
            try {
                numFrames = libraryViewModel.getRecordingDto().getNumFrames();
                int len = libraryViewModel.getFrameSize().get(frameIndex);
                int bytesRead = bufferedReader.read(buffer, 0, len);
                assert bytesRead == len;
                ImageDto imageDto = new ImageDto(new JSONObject(String.copyValueOf(buffer, 0, len)),
                        "Rainbow");
                displayImage(imageDto);
                bufferedReader.skip(1); //skip over \03
                frameIndex = frameIndex + 1;
                if (frameIndex < numFrames) {
                    imageTimerHandler.postDelayed(this, libraryViewModel.getFrameDelay().get(frameIndex-1));
                } else {
                    imageTimerHandler.removeCallbacks(this);
                    if(bufferedReader != null) {
                        bufferedReader.close();
                        bufferedReader = null;
                    }
                }
            } catch (JSONException | IOException e) {
                e.printStackTrace();
                Sentry.captureException(e);
            }
        }
    };

    private final Runnable videoGenerator = new Runnable() {
        @Override
        public void run() {
            try {
                int len, bytesRead;
                JSONObject obj = null;
                ImageDto imageDto = null;
                String jsonString = null;
                movieInfoArray = new ArrayList<>();
                String filename = FileUtils.generateNewFilename(true);
                numFrames = libraryViewModel.getRecordingDto().getNumFrames();
                ArrayList<Bitmap> bitmaps = new ArrayList<>(numFrames);
                ArrayList<Integer> frameRepeat = new ArrayList<>(numFrames);
                for(int frameIndex = 0; frameIndex < numFrames; frameIndex++) {
                    len = libraryViewModel.getFrameSize().get(frameIndex);
                    bytesRead = bufferedReader.read(buffer, 0, len);
                    assert bytesRead == len;
                    buffer[bytesRead] = 0;
                    jsonString = new String(buffer, 0, bytesRead);
                    try {
                        obj = new JSONObject(jsonString);
                    } catch (StackOverflowError ee) {
                        ee.printStackTrace();
                    }
                    imageDto = new ImageDto(obj,"Rainbow");
                    bitmaps.add(imageDto.getBitmap());
                    int delay = (int) (libraryViewModel.getFrameDelay().get(frameIndex)/30);
                    frameRepeat.add(delay==0?1:delay);
                    bufferedReader.skip(1L); //skip over '\03'
                    movieInfoArray.add(new Pair(bitmaps.get(frameIndex), delay));
                }
//                ContentValues contentValues = new ContentValues();
//                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "test.mp4");
//                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "VIDEO/MP4");
//                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
//                ContentResolver resolver = getContext().getContentResolver();
//                Uri uri = resolver.insert()
                // Check if the external storage is writable
                if (!FileUtils.isExternalStorageWritable()) {
                    throw new IOException("External storage is not writable.");
                }
                // Get the directory for public storage
                File publicDir = FileUtils.getPublicStorageDir();
                if (publicDir == null) {
                    throw new IOException("Failed to get public storage directory.");
                }

                // Create a new file in the public directory
                videoFile = new File(Environment.getExternalStoragePublicDirectory(Environment.MEDIA_SHARED), filename);
                writeFrameToMovie();


            } catch (Exception e) {
                e.printStackTrace();
                Sentry.captureException(e);
            }
        }
    };

    //N. B. The navigation bar is hidden in MainActivity

    public PlaybackFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            openRecordingFile();
            openInfoFile();
            if(recordingDto == null) {
                analyzeRecording();
                recordingFooterDto = new RecordingFooterDto(getFooterInfo());
                numFrames = recordingFooterDto.getNumFrames();
            } else {
                libraryViewModel.setRecordingDto(recordingDto);
            }
            generateMovie();
            playRecording();
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
        bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
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

    private void generateMovie() {
        new Thread(videoGenerator).start();
    }

    private void playRecording() throws IOException{
        frameIndex = 0;
        openRecordingFile();
        openInfoFile();
        imageTimerHandler.postDelayed(imagePlayer, 500);
    }


    private void analyzeRecording() throws IOException, JSONException {
        int c, bufferPos = 0;
        frameIndex = 0;
        bytesRead = 0;
        long currTime;
        libraryViewModel.setRecordingDto(new RecordingDto());
        libraryViewModel.getFrameOffset().add(frameIndex, bytesRead);
        while ((c = bufferedReader.read()) != -1) {
            if (c == 3 && bytesRead < fileSize - 1) {
                ImageDto imageDto = new ImageDto(new JSONObject(String.copyValueOf(buffer, 0, bufferPos)),
                        "Rainbow");
                if(frameIndex == 0) {
                    libraryViewModel.getFrameDelay().add(frameIndex, imageDto.getCreationDate().getTime());
                } else {
                    if(imageDto.getCreationDate().getTime() <= libraryViewModel.getFrameDelay().get(frameIndex-1)) {
                        Timber.d("oopppsssss");
                    }
                    libraryViewModel.getFrameDelay().add(frameIndex, imageDto.getCreationDate().getTime());
                    libraryViewModel.getFrameDelay().set(frameIndex-1,
                            libraryViewModel.getFrameDelay().get(frameIndex) -
                    libraryViewModel.getFrameDelay().get(frameIndex-1));
                }

                imageDto = null;
                frameIndex = frameIndex + 1;
                libraryViewModel.getFrameOffset().add(frameIndex, bytesRead + 1); //skip over \03
                libraryViewModel.getFrameSize().add(frameIndex - 1, bufferPos);
                assert bufferPos == (int) (bytesRead -
                        (frameIndex == 0 ? 0 : (libraryViewModel.getFrameOffset().get(frameIndex - 1))));
                bufferPos = 0;
            } else {
                buffer[bufferPos] = (char)c;
                bufferPos = bufferPos + 1;
            }
            bytesRead = bytesRead + 1;
        }
        libraryViewModel.getFrameSize().add(frameIndex,
                (int) (bytesRead - libraryViewModel.getFrameOffset().get(frameIndex - 1)));
        numFrames = libraryViewModel.getFrameOffset().size() - 1; // less footer and final \03
        libraryViewModel.getFrameDelay().remove(numFrames-1); //last value is for the footer
        Timber.d("read %d frames", frameIndex);
        generateMovie();
    }

    private void writeFrameToMovie() {
        try {
            if(out == null) {
                videoFilename = FileUtils.generateNewFilename(true);
                out = NIOUtils.writableFileChannel("/tmp/output.mp4");
                encoder = new AndroidSequenceEncoder(out, Rational.R(25, 1));
            }
            // for Android use: AndroidSequenceEncoder
            for (int nFrame = 0; nFrame < movieInfoArray.size(); nFrame++) {
                // Generate the image, for Android use Bitmap
                //BufferedImage image = ...;
                // Encode the image
                encoder.encodeImage(movieInfoArray.get(nFrame).first);
            }
            // Finalize the encoding, i.e. clear the buffers, write the header, etc.
            encoder.finish();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            NIOUtils.closeQuietly(out);
        }
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
            if(bufferedReader != null) {
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
        imageTimerHandler.removeCallbacks(imagePlayer);
    }
}
