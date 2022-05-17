package com.darcangel.acam.ui.settings;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.databinding.adapters.TextViewBindingAdapter;
import androidx.databinding.adapters.TextViewBindingAdapter.OnTextChanged;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import timber.log.Timber;

import com.darcangel.acam.databinding.FragmentSettingsBinding;
import com.darcangel.acam.Settings.Settings;


import javax.inject.Inject;

public class SettingsFragment extends Fragment {

@Inject
Settings settings;

    private FragmentSettingsBinding binding;
    private ViewGroup container;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.container = container;

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        SettingsViewModel settingsViewModel =
                new ViewModelProvider(this).get(SettingsViewModel.class);

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.setModel(settingsViewModel);

        //Observer for Camera IP Address
        settings.getCameraAddress().observe(this, new Observer<String>() {
                    @Override
                    public void onChanged(String s) {
                        Timber.d("Camera Address is no %s", s);
                    }
                }
        );

        //Create the observer
        final Observer<String> emissivityObserver = new Observer<String>() {
            @Override
            public void onChanged(String value) {
                Timber.d("Emissivity is " + value);
            }
        };
        //TODO settingsViewModel.getEmissivity().observe(this, emissivityObserver);

        //Create the observer for the Manual Range Switch
        final Observer<Boolean> manualRangeObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean value) {
                Timber.d("Manual Range Switch is " + value);
                if (value) {
                    binding.layoutManualRange.setVisibility(View.VISIBLE);
                } else {
                    binding.layoutManualRange.setVisibility(View.GONE);
                }
            }
        };
        //TODO settingsViewModel.getEmissivity().observe(this, emissivityObserver);
        //TODO settingsViewModel.getManualRangeSwitch().observe(this, manualRangeObserver);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}