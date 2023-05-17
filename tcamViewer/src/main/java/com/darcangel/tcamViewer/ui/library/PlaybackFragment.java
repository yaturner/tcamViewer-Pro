package com.darcangel.tcamViewer.ui.library;

import android.graphics.Bitmap;
import android.os.Bundle;
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
import com.darcangel.tcamViewer.utils.CameraUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
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
    private RecordingDto recordingDto;
    private int numFrames;
    private long fileSize;
    private static final char[] buffer = new char[64767];

    private final Runnable imagePlayer = new Runnable() {
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

    //N. B. The navigation bar is hidden in MainActivity

    public PlaybackFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = MainActivity.getInstance();
        libraryViewModel = mainActivity.getLibraryViewModel();
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
        binding.tvSpotmeterTemperature.setText(cameraUtils.createTemperatureString(imageDto.
                getMeanTemperatureAtSpotmeter()));
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

    @Override
    public void onPause() {
        super.onPause();
        imageTimerHandler.removeCallbacks(imagePlayer);
    }
}
