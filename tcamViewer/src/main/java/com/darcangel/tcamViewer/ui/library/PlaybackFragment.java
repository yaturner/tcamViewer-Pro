package com.darcangel.tcamViewer.ui.library;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;
import com.darcangel.tcamViewer.constants.Constants;
import com.darcangel.tcamViewer.databinding.FragmentLibrarySlideshowBinding;
import com.darcangel.tcamViewer.databinding.FragmentPlaybackBinding;
import com.darcangel.tcamViewer.model.ImageDto;
import com.darcangel.tcamViewer.model.RecordingFooterDto;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Timer;

import io.sentry.Sentry;

public class PlaybackFragment extends Fragment {
    private String filename;
    private ImageDto[] imageDtos;
    private Timer imageTimer;
    private BufferedReader bufferedReader;
    private MainActivity mainActivity;
    private FragmentPlaybackBinding binding;
    private View root;
    private BottomNavigationView navBar;
    private LibraryViewModel libraryViewModel;
    private ImageDto[] playbackImageArray = new ImageDto[2];
    private int numFrames;
    private long fileSize;

    public PlaybackFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = MainActivity.getInstance();
        libraryViewModel = mainActivity.getLibraryViewModel();
        //prime the pump
        playbackImageArray[0] = libraryViewModel.getPlaybackImageDto();
        filename = playbackImageArray[0].getFilename();
        imageTimer = new Timer("imageTimer");
        try {
            File file = new File(filename);
            fileSize = file.length();
            bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Sentry.captureException(e);
            return;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        binding = FragmentPlaybackBinding.inflate(inflater, container, false);
        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navBar = getActivity().findViewById(R.id.nav_view);
        if (navBar != null) {
            navBar.setVisibility(View.GONE);
        }
        RecordingFooterDto recordingFooterDto = new RecordingFooterDto(getFooterInfo());
        numFrames = recordingFooterDto.getNumFrames();
    }


    private ImageDto readImageDtoFromFile() {
        return null;
    }

    private void showImage() {

    }

    private JSONObject getFooterInfo() {
        JSONObject result = null;
        try {
            bufferedReader.skip(fileSize - Constants.RECORDING_FOOTER_LENGTH);
            StringBuilder sb = new StringBuilder();
            int c = 0;
            while((c = bufferedReader.read()) != 3 && c != -1) {
                sb.append((char)c);
            }
            result = new JSONObject(sb.toString());
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
