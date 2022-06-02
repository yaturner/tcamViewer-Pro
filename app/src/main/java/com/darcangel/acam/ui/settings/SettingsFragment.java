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
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.R;
import com.darcangel.acam.databinding.FragmentSettingsBinding;

import org.json.JSONException;
import org.json.JSONObject;

@AndroidEntryPoint
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private ViewGroup container;
    private SettingsViewModel settingsViewModel;
    private MainActivity mainActivity;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.container = container;

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        settingsViewModel =
                new ViewModelProvider(this).get(SettingsViewModel.class);

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.setListener(settingsViewModel.getSettingsListener());

        if(mainActivity == null) {
            mainActivity = MainActivity.getInstance();
        }

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //get the latest config from the camera
        if(mainActivity.getCameraService().isConnected()) {
            getConfig();
            binding.btnNavWiFiSettings.setEnabled(true);
        } else {
            binding.btnNavWiFiSettings.setEnabled(false);
        }
    }

    private void getConfig() {
        MainActivity.CameraCallback callback = new MainActivity.CameraCallback() {
            @Override
            public void callback(JSONObject response) {
                Integer agc;
                Integer emissivity;
                Integer gain;
                try {
                    if (response.has("result")) {
                        if (response.getString("result").equals("OK")) {
                            JSONObject object = new JSONObject(response.getString("response"));
                            if(object.has("config")) {
                                JSONObject configObj = object.getJSONObject("config");
                                if (configObj.has("agc_enabled")) {
                                    agc = configObj.getInt("agc_enabled");
                                    settingsViewModel.setAGC(agc == 1);
                                }
                                if (configObj.has("emissivity")) {
                                    emissivity = configObj.getInt("emissivity");
                                    settingsViewModel.setEmissivity(emissivity);
                                }
                                if (configObj.has("gain_mode")) {
                                    gain = configObj.getInt("gain_mode");
                                    settingsViewModel.setGain(gain);
                                }
                            }
                        } else {
                            //TODO handle error
                        }
                    } else {
                        //TODO handle error
                    }
                } catch (JSONException e1) {
                    //TODO handle error
                }
            }
        };
        mainActivity.getCameraUtils().getConfig(callback);
    }
}