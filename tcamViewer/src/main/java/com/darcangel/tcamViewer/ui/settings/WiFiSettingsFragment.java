package com.darcangel.tcamViewer.ui.settings;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;
import com.darcangel.tcamViewer.constants.Constants;
import com.darcangel.tcamViewer.container.Settings;
import com.darcangel.tcamViewer.databinding.FragmentWifiSettingsBinding;
import com.darcangel.tcamViewer.ui.camera.CameraService;

import org.json.JSONException;
import org.json.JSONObject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.disposables.Disposable;
import timber.log.Timber;

@AndroidEntryPoint
public class WiFiSettingsFragment extends Fragment implements OnClickListener {

    private FragmentWifiSettingsBinding binding;
    private ViewGroup container;
    private SettingsViewModel settingsViewModel;
    private MainActivity mainActivity;
    private Settings settings;
    private NavDirections navDirections;
    private CameraService cameraService;
    private Disposable disposable;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.container = container;
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (mainActivity == null) {
            mainActivity = MainActivity.getInstance();
        }

        settingsViewModel = mainActivity.getSettingsViewModel();
        binding = FragmentWifiSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        if(settings == null) {
            settings = mainActivity.getSettings();
        }
        cameraService = mainActivity.getCameraService();
        binding.setSettings(settings);
        binding.btnCancelSave.btnCancel.setOnClickListener(this);
        binding.btnCancelSave.btnSave.setOnClickListener(this);

        disposable = cameraService.getImageChannel().
                subscribe(t -> {
                            Timber.d("String is %s", t);
                        },
                        e -> {});

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        //get the wifi settings
        String cmd = new String(Constants.CMD_GET_WIFI);
        try {
            mainActivity.getCameraService().sendCmd(cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendWiFi() {
        StringBuilder stringBuilder = new StringBuilder();
        
    }

    private void handleGetWifiResponse(JSONObject response) {
        try {
            JSONObject wifi = response.getJSONObject("wifi");
//            Iterator<String> keys = wifi.keys();
//            while (keys.hasNext()) {
//                String key = keys.next();
//                if (wifi.get(key).equals("flags")) {
//                    int value = ((Double) wifi.get(key)).intValue();
//                    //process flags
//                    settings.setAccessPoint((value & 0x80) != 0x80);
//                    settings.setStaticIP((value & 0x01) == 0x01);
//                } else {
//                    String value = (String) wifi.getKey(key).getValue();
//                    if (entry.get().equals("sta_ssid")) {
//                        settings.setSSID(value);
//                    }
//                    if (wifi.get(key).equals("sta_ip_addr")) {
//                        settings.setStaticIPAddress(value);
//                    }
//                    if (entry.get(key).equals("sta_netmask")) {
//                        settings.setStaticNetmask(value);
//                    }
//                }
//            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
                sendWiFi();
                navDirections = WiFiSettingsFragmentDirections.actionWiFiSettingsFragmentToNavigationSettings();
                mainActivity.getNavController().navigate(navDirections);
                break;
        }
    }
}