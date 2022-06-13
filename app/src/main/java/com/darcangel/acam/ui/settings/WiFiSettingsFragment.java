package com.darcangel.acam.ui.settings;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.R;
import com.darcangel.acam.constants.Constants;
import com.darcangel.acam.container.Settings;
import com.darcangel.acam.databinding.FragmentWifiSettingsBinding;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class WiFiSettingsFragment extends Fragment implements OnClickListener {

    private FragmentWifiSettingsBinding binding;
    private ViewGroup container;
    private SettingsViewModel settingsViewModel;
    private MainActivity mainActivity;
    private Settings settings;
    private NavDirections navDirections;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.container = container;

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        settingsViewModel =
                new ViewModelProvider(this).get(SettingsViewModel.class);

        binding = FragmentWifiSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        if (mainActivity == null) {
            mainActivity = MainActivity.getInstance();
        }

        if(settings == null) {
            settings = mainActivity.getSettings();
        }
        binding.setSettings(settings);

        binding.btnCancelSave.btnCancel.setOnClickListener(this);
        binding.btnCancelSave.btnSave.setOnClickListener(this);


        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        //get the wifi settings
        String cmd = new String(Constants.CMD_GET_WIFI);
        Gson gson = new Gson();
        MainActivity.CameraCallback callback = new MainActivity.CameraCallback() {
            @Override
            public void callback(JSONObject jsonObject) {
                try {
                    if(jsonObject.has("result") && (jsonObject.getString("result").equals("OK"))
                    && jsonObject.has("response") && jsonObject.getJSONObject("response").has("wifi")) {
                        JSONObject wifi = jsonObject.getJSONObject("response").getJSONObject("wifi");
                        Map<String, Object> map = gson.fromJson(wifi.toString(), Map.class);
                        for(Map.Entry<String, Object> entry : map.entrySet()) {
                            if(entry.getKey().equals("flags")) {
                                int value = ((Double) entry.getValue()).intValue();
                                //process flags
                                settings.setAccessPoint((value & 0x80) != 0x80);
                                settings.setStaticIP((value & 0x01) == 0x01);
                            } else {
                                String value = (String) entry.getValue();
                                if(entry.getKey().equals("sta_ssid")) {
                                    settings.setSSID(value);
                                }
                                if(entry.getKey().equals("sta_ip_addr")) {
                                    settings.setStaticIPAddress(value);
                                }
                                if(entry.getKey().equals("sta_netmask")) {
                                    settings.setStaticNetmask(value);
                                }
                            }
                        }
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        };
        try {
            ((MainActivity) mainActivity).getCameraService().sendCmd(cmd, callback);
        } catch (Exception e) {

        }
    }

    private void sendWiFI() {
        StringBuilder stringBuilder = new StringBuilder();
        
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnCancel:
                navDirections = WiFiSettingsFragmentDirections.actionWiFiSettingsFragmentToNavigationSettings();
                mainActivity.getNavController().navigate(navDirections);
                break;
            case R.id.btnSave:
                //TODO send config settings to camera and persist SharedPreferences
                sendWiFI();
                navDirections = WiFiSettingsFragmentDirections.actionWiFiSettingsFragmentToNavigationSettings();
                mainActivity.getNavController().navigate(navDirections);
                break;
        }
    }
}