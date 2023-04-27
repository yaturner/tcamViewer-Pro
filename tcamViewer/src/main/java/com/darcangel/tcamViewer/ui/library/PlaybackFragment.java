package com.darcangel.tcamViewer.ui.library;

import android.content.pm.ActivityInfo;
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
import android.widget.Toast;

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
import com.darcangel.tcamViewer.model.RecordingFooterDto;
import com.darcangel.tcamViewer.utils.CameraUtils;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
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
    private MainActivity mainActivity;
    private CameraUtils cameraUtils;
    private FragmentPlaybackBinding binding;
    private View root;
    private LibraryViewModel libraryViewModel;
    private ImageDto[] playbackImageArray = new ImageDto[2];
    private int numFrames;
    private long fileSize;
    private char[] buffer = new char[64767];

    private Runnable imagePlayer = new Runnable() {
        @Override
        public void run() {
            try {
                int len = libraryViewModel.getFrameSize().get(frameIndex);
                bufferedReader.read(buffer, 0, len);
                ImageDto imageDto = new ImageDto(new JSONObject(String.copyValueOf(buffer, 0, len)),
                        "Rainbow");
                displayImage(imageDto);
                bufferedReader.skip(1); //skip over \03
                frameIndex = frameIndex + 1;
                if (frameIndex < numFrames) {
                    imageTimerHandler.postDelayed(this, 500);
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
        playbackImageArray[0] = libraryViewModel.getPlaybackImageDto();
        filename = playbackImageArray[0].getFilename();
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
            analyzeRecording();
            recordingFooterDto = new RecordingFooterDto(getFooterInfo());
            numFrames = recordingFooterDto.getNumFrames();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Sentry.captureException(e);
        } catch (IOException e) {
            e.printStackTrace();
            Sentry.captureException(e);
        }
        playRecording();
    }

    private void openRecordingFile() throws IOException {
        File file = new File(filename);
        fileSize = file.length();
        bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
    }

    private void playRecording() {
        frameIndex = 0;
        imageTimerHandler.postDelayed(imagePlayer, 500);
    }


    private void analyzeRecording() throws IOException {
        int c;
        frameIndex = 0;
        bytesRead = 0;
        libraryViewModel.getFrameOffset().add(frameIndex, bytesRead);
        while ((c = bufferedReader.read()) != -1) {
            if (c == 3 && bytesRead < fileSize - 1) {
                //exclude \03
                frameIndex = frameIndex + 1;
                libraryViewModel.getFrameOffset().add(frameIndex, bytesRead + 1); //skip over \03
                libraryViewModel.getFrameSize().add(frameIndex - 1, (int) (bytesRead -
                        (frameIndex == 0 ? 0 : (libraryViewModel.getFrameOffset().get(frameIndex - 1)))));
            }
            bytesRead = bytesRead + 1;
        }
        libraryViewModel.getFrameSize().add(frameIndex,
                (int) (bytesRead - libraryViewModel.getFrameOffset().get(frameIndex - 1)));
        numFrames = libraryViewModel.getFrameOffset().size() - 1; // less footer and final \03
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
