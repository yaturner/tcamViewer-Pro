package com.darcangel.tcamViewer.ui.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;
import com.darcangel.tcamViewer.constants.Constants;
import com.darcangel.tcamViewer.container.Settings;
import com.darcangel.tcamViewer.databinding.FragmentWifiSettingsBinding;
import com.darcangel.tcamViewer.ui.camera.CameraService;
import com.darcangel.tcamViewer.ui.camera.CameraViewModel;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.disposables.Disposable;
import timber.log.Timber;

@AndroidEntryPoint
public class WiFiSettingsFragment extends Fragment implements OnClickListener {

    private FragmentWifiSettingsBinding binding;
    private ViewGroup container;
    private SettingsViewModel settingsViewModel;
    private CameraViewModel cameraViewModel;
    private MainActivity mainActivity;
    private Settings settings;
    private NavDirections navDirections;
    private CameraService cameraService;
    private Disposable disposable;
    private OnBackPressedDispatcher onBackPressedDispatcher;
    private OnBackPressedCallback onBackPressedCallback;
    private View root;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.container = container;
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (mainActivity == null) {
            mainActivity = MainActivity.getInstance();
        }

        settingsViewModel = mainActivity.getSettingsViewModel();
        cameraViewModel = mainActivity.getCameraViewModel();
        binding = FragmentWifiSettingsBinding.inflate(inflater, container, false);
        root = binding.getRoot();

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
        ActionBar actionBar = mainActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(false); // disable the button
            actionBar.setDisplayHomeAsUpEnabled(false); // remove the left caret
            actionBar.setDisplayShowHomeEnabled(false); // remove the icon
        }

        getWifi();
    }

    private Dialog createSaveDialog () {
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle(R.string.title_settings)
                .setMessage("Do you wish to save your settings")
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    setWiFi();
                    dialog.dismiss();
                    onBackPressedCallback.setEnabled(false);
                    Navigation.findNavController(root).popBackStack();
                })
                .setNegativeButton(R.string.no, (dialog, which) -> {
                    dialog.dismiss();
                    onBackPressedCallback.setEnabled(false);
                    Navigation.findNavController(root).popBackStack();
                });
        return builder.create();
    }

//    public final static String ARGS_SET_WIFI = "{\n" +
//            "    \"ap_ssid\": \"%s\"\n" +
//            "    \"ap_pw: \"%s\"\n" +
//            "    \"ap_ip_addr\": \"%s\",\n" +
//            "    \"flags\": %d,\n" +
//            "    \"sta_ssid\": \"%s\",\n" +
//            "    \"sta_pw\": \"%s\",\n" +
//            "  }";

    private void setWiFi() {

    }

    private void getWifi() {
        cameraViewModel.getWifi();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnCancel:
                Navigation.findNavController(root).popBackStack();
                break;
            case R.id.btnSave:
                //TODO send config settings to camera
                createSaveDialog().show();
                Navigation.findNavController(root).popBackStack();
                break;
        }
    }
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        onBackPressedCallback = new OnBackPressedCallback(
                true // default to enabled
        ) {
            @Override
            public void handleOnBackPressed() {
                onBackPressedCallback.setEnabled(false);
                createSaveDialog().show();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(
                this, // LifecycleOwner
                onBackPressedCallback);
    }

}