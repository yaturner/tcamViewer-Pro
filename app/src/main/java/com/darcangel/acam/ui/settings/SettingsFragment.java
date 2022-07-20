package com.darcangel.acam.ui.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.R;
import com.darcangel.acam.adapters.EmissivityDialogListAdapter;
import com.darcangel.acam.constants.Constants;
import com.darcangel.acam.container.Settings;
import com.darcangel.acam.databinding.FragmentSettingsBinding;
import com.darcangel.acam.ui.camera.CameraService;
import com.darcangel.acam.utils.CameraUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsFragment extends Fragment implements View.OnClickListener {

    private FragmentSettingsBinding binding;
    private ViewGroup container;
    private SettingsViewModel settingsViewModel;
    private MainActivity mainActivity;
    private Settings settings;
    private NavDirections navDirections;
    private int[] emValues;
    private boolean hadFocus = false;
    private OnBackPressedDispatcher onBackPressedDispatcher;
    private OnBackPressedCallback onBackPressedCallback;
    private CameraService cameraService;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.container = container;
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (mainActivity == null) {
            mainActivity = MainActivity.getInstance();
        }
        cameraService = mainActivity.getCameraService();
        settingsViewModel = mainActivity.getSettingsViewModel();
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        settings = mainActivity.getSettings();
        binding.setSettings(settings);

        String address = settings.getCameraAddress();
        if (address != null && !address.isEmpty()) {
            binding.cameraIPAddress.setText(address, TextView.BufferType.EDITABLE);
        }

        emValues = mainActivity.getResources().getIntArray(R.array.emissivity_values);

        /*
         * only update the camera address in the settings, service when the focus changes
         *   otherwise we get called after each char is typed
         */
        binding.cameraIPAddress.setOnFocusChangeListener((v, hasFocus) -> {
            if (hadFocus && !hasFocus) {
                String etAddress = binding.cameraIPAddress.getText().toString();
                if (CameraUtils.isValidIPAddress(etAddress)) {
                    if (mainActivity.getCameraService() != null) {
                        settings.setCameraAddress(etAddress);
                        hadFocus = false;
                    }
                }
            } else {
                hadFocus = true;
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mainActivity)
                        .setCancelable(true)
                        .setPositiveButton(R.string.ok, (dialog, which) -> {
                                dialog.dismiss();
                            })
                        .setMessage(R.string.warning_disconnect);
                builder.create().show();
            }
        });


        return view;
    }

        private Dialog createSaveDialog () {
            AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
            builder.setTitle(R.string.title_settings)
                    .setMessage("Do you wish to save your settings")
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        settings.persist();
                        dialog.dismiss();
                        onBackPressedCallback.remove();
                        mainActivity.onBackPressed();
                    })
                    .setNegativeButton(R.string.no, (dialog, which) -> {
                        dialog.dismiss();
                        onBackPressedCallback.remove();
                        mainActivity.onBackPressed();
                    });
            return builder.create();
        }

        @Override
        public void onViewCreated (@NonNull View view, @Nullable Bundle savedInstanceState){
            super.onViewCreated(view, savedInstanceState);
            if (mainActivity.getCameraService().isConnected()) {
                binding.btnCancelSave.btnSave.setEnabled(true);
                binding.btnNavWiFiSettings.setEnabled(true);
                binding.btnNavWiFiSettings.setOnClickListener(this);
            } else {
                binding.btnCancelSave.btnSave.setEnabled(false);
                binding.btnNavWiFiSettings.setEnabled(false);
                binding.btnNavWiFiSettings.setOnClickListener(null);
            }
            binding.btnEmissivityHint.setOnClickListener(this);
        }


        @Override
        public void onClick (View v){
            switch (v.getId()) {
                case R.id.btnNavWiFiSettings:
                    navDirections = SettingsFragmentDirections.actionNavigationSettingsToWiFiSettingsFragment();
                    mainActivity.getNavController().navigate(navDirections);
                    break;
                case R.id.btnEmissivityHint:
                    int selectedItem;
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Select Emissivity")
                            .setAdapter(new EmissivityDialogListAdapter(getActivity()),
                                    (dialog, which) -> {
                                        settings.setEmissivity(emValues[which]);
                                    })
                            .setCancelable(true)
                            .setNegativeButton(getString(R.string.cancel), null)
                            .setPositiveButton(getString(R.string.ok), null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    break;
            }
        }

        @Override
        public void onDestroy () {
            super.onDestroy();
            settings.persist();
            //setConfig();
        }
    }