package com.darcangel.acam.ui.settings;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import dagger.hilt.android.AndroidEntryPoint;

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.databinding.FragmentSettingsBinding;
import com.darcangel.acam.container.Settings;

import org.json.JSONException;
import org.json.JSONObject;

@AndroidEntryPoint
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private ViewGroup container;
    private SettingsViewModel settingsViewModel;
    private MainActivity mainActivity;
    private Settings settings;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.container = container;

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        settingsViewModel =
                new ViewModelProvider(this).get(SettingsViewModel.class);

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        if (mainActivity == null) {
            mainActivity = MainActivity.getInstance();
        }
        settings = mainActivity.getSettings();
        binding.setSettings(settings);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //get the latest config from the camera
        if (mainActivity.getCameraService().isConnected()) {
            binding.btnNavWiFiSettings.setEnabled(true);
        } else {
            binding.btnNavWiFiSettings.setEnabled(false);
        }
    }
}