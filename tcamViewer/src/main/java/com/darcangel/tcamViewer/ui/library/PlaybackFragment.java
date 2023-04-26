package com.darcangel.tcamViewer.ui.library;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.databinding.FragmentPlaybackBinding;
import com.darcangel.tcamViewer.model.ImageDto;
import com.darcangel.tcamViewer.model.RecordingFooterDto;

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

public class PlaybackFragment extends Fragment {
    private String filename;
    private ImageDto[] imageDtos;
    private int frameIndex;
    private long bytesRead;
    private RecordingFooterDto recordingFooterDto;
    private Timer imageTimer;
    private BufferedReader bufferedReader;
    private MainActivity mainActivity;
    private FragmentPlaybackBinding binding;
    private View root;
    private LibraryViewModel libraryViewModel;
    private ImageDto[] playbackImageArray = new ImageDto[2];
    private int numFrames;
    private long fileSize;

    //N. B. The navigation bar is hidden in MainActivity

    public PlaybackFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        mainActivity = MainActivity.getInstance();
        libraryViewModel = mainActivity.getLibraryViewModel();
        frameIndex = 0;
        bytesRead = 0;
        //prime the pump
        playbackImageArray[0] = libraryViewModel.getPlaybackImageDto();
        filename = playbackImageArray[0].getFilename();
        imageTimer = new Timer("imageTimer");
        if(libraryViewModel.getFrameOffset().size() == 0) {
            try {
                File file = new File(filename);
                fileSize = file.length();
                bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                analyzeRecording();
                recordingFooterDto = new RecordingFooterDto(getFooterInfo());
                numFrames = recordingFooterDto.getNumFrames();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Sentry.captureException(e);
            } catch (IOException e) {
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
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentPlaybackBinding.inflate(inflater, container, false);
        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void analyzeRecording() throws IOException {
        int c;
        frameIndex = 0;
        bytesRead = 0;
        libraryViewModel.getFrameOffset().add(frameIndex, bytesRead);
        while((c = bufferedReader.read()) != -1) {
            if(c == 3 && bytesRead < fileSize - 1) {
                //exclude \03
                frameIndex = frameIndex + 1;
                libraryViewModel.getFrameOffset().add(frameIndex, bytesRead+1); //skip over \03
                libraryViewModel.getFrameSize().add(frameIndex-1, (int)(bytesRead -
                        (frameIndex==0?0:(libraryViewModel.getFrameOffset().get(frameIndex-1)+1))));
            }
            bytesRead = bytesRead + 1;
        }
        libraryViewModel.getFrameSize().add(frameIndex,
                (int)(bytesRead - libraryViewModel.getFrameOffset().get(frameIndex-1)));
        numFrames = libraryViewModel.getFrameOffset().size() - 1; // less footer and final \03
        Timber.d("read %d frames", frameIndex);
        //verify
//        char buffer[] = new char[647568];
//        bufferedReader.close();
//        filename = playbackImageArray[0].getFilename();
//        int s = size.get(1);
//        File file = new File(filename);
//        bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
//        bufferedReader.skip(offset.get(1));
//        bufferedReader.read(buffer, 0, s);
//        buffer[size.get(1)+1] = 0;
//        Timber.d("read '%s'", String.valueOf(buffer, 0, s));
    }

    private ImageDto readImageDtoFromFile() {
        return null;
    }

    private void showImage() {

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
}
