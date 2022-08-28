package com.darcangel.tcamViewer.ui.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.darcangel.tcamViewer.BuildConfig;
import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;
import com.darcangel.tcamViewer.adapters.EmissivityDialogListAdapter;
import com.darcangel.tcamViewer.adapters.PaletteDialogListAdapter;
import com.darcangel.tcamViewer.container.Settings;
import com.darcangel.tcamViewer.databinding.FragmentSettingsBinding;
import com.darcangel.tcamViewer.ui.camera.CameraService;
import com.darcangel.tcamViewer.ui.camera.CameraViewModel;
import com.darcangel.tcamViewer.utils.CameraUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsFragment extends Fragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private FragmentSettingsBinding binding;
    private ViewGroup container;
    private SettingsViewModel settingsViewModel;
    private CameraViewModel cameraViewModel;
    private MainActivity mainActivity;
    private Settings settings;
    private NavDirections navDirections;
    private int[] emValues;
    private boolean hadFocus = false;
    private OnBackPressedDispatcher onBackPressedDispatcher;
    private OnBackPressedCallback onBackPressedCallback;
    private CameraService cameraService;
    private View root;
    private Bundle snapshot;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.container = container;
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        if (mainActivity == null) {
            mainActivity = MainActivity.getInstance();
        }
        cameraService = mainActivity.getCameraService();
        settingsViewModel = mainActivity.getSettingsViewModel();
        cameraViewModel = mainActivity.getCameraViewModel();

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        //get the settings container
        settings = mainActivity.getSettings();
        binding.setSettings(settings);

        String address = settings.getCameraAddress().getValue();
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
                    //Change the camera IP address in cameraService only if the user saves the settings
                    settings.setCameraAddress(etAddress);
                    hadFocus = false;
                }
            } else {
                hadFocus = true;
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mainActivity)
                        .setCancelable(true)
                        .setPositiveButton(R.string.ok, (dialog, which) -> {
                            dialog.dismiss();
                            binding.cameraIPAddress.requestFocus();
                        })
                        .setMessage(R.string.warning_disconnect);
                builder.create().show();
            }
        });

        //initially save to camera is false
        settings.setSaveToCamera(false);

        binding.switchAGC.setChecked(settings.getAGC().getValue());

        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mainActivity.getCameraService().isConnected()) {
            binding.btnNavWiFiSettings.setEnabled(true);
            binding.btnNavWiFiSettings.setOnClickListener(this);
        } else {
            binding.btnNavWiFiSettings.setEnabled(false);
            binding.btnNavWiFiSettings.setOnClickListener(null);
        }
        binding.btnEmissivityHint.setOnClickListener(this);
        binding.btnPalette.setOnClickListener(this);
        binding.btnPrivacy.setOnClickListener(this);
        binding.tvVersion.setText(BuildConfig.VERSION_NAME);

        binding.btnCancelSave.btnSave.setOnClickListener(this);
        binding.btnCancelSave.btnCancel.setOnClickListener(this);

        binding.switchAGC.setOnCheckedChangeListener(this);

        //get the camera settings from the camera if the camera is connected
        //  otherwise hide the camera settings
        if(!cameraService.isConnected()) {
            int refIds[] = binding.groupCameraSettings.getReferencedIds();
            for (int index = 0; index < refIds.length; index++) {
                root.findViewById(refIds[index]).setVisibility(ConstraintLayout.GONE);
            }
        } else {
            cameraViewModel.getConfig();
        }

        //Take a snapshot of the settings so that if the user selects cancel we can restore it
        snapshot = new Bundle();
        settings.snapshot(snapshot);
    }

    private Dialog createSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle(R.string.title_settings)
                .setMessage("Do you wish to save your settings")
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    saveCameraSettings();
                    settings.persist();
                    dialog.dismiss();
                    onBackPressedCallback.setEnabled(false);
                    Navigation.findNavController(root).popBackStack();
                })
                .setNegativeButton(R.string.no, (dialog, which) -> {
                    dialog.dismiss();
                    settings.restore(snapshot);
                    onBackPressedCallback.setEnabled(false);
                    Navigation.findNavController(root).popBackStack();
                });
        return builder.create();
    }

    private void saveCameraSettings() {
        if (cameraService.getIpAddress() == null ||
                (!binding.cameraIPAddress.getText().toString().
                        equals(settings.getCameraAddress().getValue()))) {
            cameraService.setIpAddress(binding.cameraIPAddress.getText().toString());
            settings.setCameraAddress(binding.cameraIPAddress.getText().toString());
        }
        if (settings.getSaveToCamera().getValue()) {
            //save the config
            cameraViewModel.setConfig();
            settings.setSaveToCamera(false);
        }
        if(binding.switchManualRange.isChecked()) {
            try {
                float max = Float.parseFloat(binding.etManualRangeMax.toString());
                float min = Float.parseFloat(binding.etManualRangeMin.toString());
                if (min >= max) {
                    //TODO error dialog
                }
                settings.setAutoRange(false);
                settings.setManualRangeMax(max);
                settings.setManualRangeMin(min);
            } catch (NumberFormatException e) {
                //TODO error dialog
            }
        } else {
            settings.setAutoRange(true);
        }
    }

    @Override
    public void onClick(View v) {
        int selectedItem;
        AlertDialog.Builder builder;
        AlertDialog dialog;
        int id = v.getId();
        if (id == R.id.btn_privacy) {
            navDirections = SettingsFragmentDirections.actionNavigationSettingsToPrivacyDisclosure();
            mainActivity.getNavController().navigate(navDirections);
        } else if (id == R.id.btnNavWiFiSettings) {
            navDirections = SettingsFragmentDirections.actionNavigationSettingsToWiFiSettingsFragment();
            mainActivity.getNavController().navigate(navDirections);
        } else if (id == R.id.btnEmissivityHint) {
            builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Select Emissivity")
                    .setAdapter(new EmissivityDialogListAdapter(getActivity()),
                            (emdialog, which) -> {
                                settings.setEmissivity(emValues[which]);
                            })
                    .setCancelable(true)
                    .setNegativeButton(getString(R.string.cancel), null)
                    .setPositiveButton(getString(R.string.ok), null);
            dialog = builder.create();
            dialog.show();
        } else if (id == R.id.btnPalette) {
            builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Select Palette")
                    .setAdapter(new PaletteDialogListAdapter(getActivity()),
                            (paldialog, which) -> {
                                String palette = mainActivity.getPaletteFactory().getPaletteName(which);
                                settings.setPalette(palette);
                            })
                    .setCancelable(true)
                    .setNegativeButton(getString(R.string.cancel), null)
                    .setPositiveButton(getString(R.string.ok), null);
            dialog = builder.create();
            dialog.show();
        } else if (id == R.id.btnSave) {
            createSaveDialog().show();
        } else if (id == R.id.btnCancel) {
            settings.restore(snapshot);
            Navigation.findNavController(root).popBackStack();
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

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.switchAGC) {
            settings.setAGC(isChecked);
        } else if(buttonView.getId() == R.id.switchAGC) {
            settings.setAutoRange(!isChecked);
            if(isChecked) {
                binding.etManualRangeMax.setVisibility(View.VISIBLE);
                binding.etManualRangeMin.setVisibility(View.VISIBLE);
            } else {
                binding.etManualRangeMax.setVisibility(View.GONE);
                binding.etManualRangeMin.setVisibility(View.GONE);
            }
//            cameraUtils.setManualRange(true);
//            cameraUtils.setMaxManualTemperature(settings.getManualRangeMax().getValue());
//            cameraUtils.setMinManualTemperature(settings.getManualRangeMin().getValue());

        }
    }
}