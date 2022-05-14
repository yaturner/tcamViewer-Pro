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

import com.darcangel.acam.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        SettingsViewModel settingsViewModel =
                new ViewModelProvider(this).get(SettingsViewModel.class);

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.emissivity.setText("98.6");
        binding.setModel(settingsViewModel);

        //Create the observer
        final Observer<String> emissivityObserver = new Observer<String>() {
            @Override
            public void onChanged(String value) {
                Log.d("SettingsFragment", "Emissivity is " + value);
            }
        };
        settingsViewModel.getEmissivity().observe(this, emissivityObserver);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}