package com.darcangel.acam.ui.settings;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.databinding.FragmentWifiSettingsBinding;


    @AndroidEntryPoint
    public class WiFiSettingsFragment extends Fragment {

        private FragmentWifiSettingsBinding binding;
        private ViewGroup container;
        private SettingsViewModel settingsViewModel;
        private MainActivity mainActivity;

        public View onCreateView(@NonNull LayoutInflater inflater,
                                 ViewGroup container, Bundle savedInstanceState) {
            this.container = container;

            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            settingsViewModel =
                    new ViewModelProvider(this).get(SettingsViewModel.class);

            binding = FragmentWifiSettingsBinding.inflate(inflater, container, false);
            View root = binding.getRoot();

            binding.setListener(settingsViewModel.getSettingsListener());

            if(mainActivity == null) {
                mainActivity = MainActivity.getInstance();
            }

            return root;
        }
}