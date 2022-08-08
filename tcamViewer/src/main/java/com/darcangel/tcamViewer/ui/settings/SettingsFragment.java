package com.darcangel.tcamViewer.ui.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
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
public class SettingsFragment extends Fragment implements View.OnClickListener {

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

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.container = container;
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (mainActivity == null) {
            mainActivity = MainActivity.getInstance();
        }
        cameraService = mainActivity.getCameraService();
        settingsViewModel = mainActivity.getSettingsViewModel();
        cameraViewModel = mainActivity.getCameraViewModel();

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
                                binding.cameraIPAddress.requestFocus();
                            })
                        .setMessage(R.string.warning_disconnect);
                builder.create().show();
            }
        });
        return view;
    }

    @Override
    public void onViewCreated (@NonNull View view, @Nullable Bundle savedInstanceState){
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
        binding.tvVersion.setText(BuildConfig.VERSION_NAME);
    }

    private Dialog createSaveDialog () {
            AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
            builder.setTitle(R.string.title_settings)
                    .setMessage("Do you wish to save your settings")
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        settings.persist();
                        dialog.dismiss();
                        onBackPressedCallback.setEnabled(false);
                        mainActivity.onBackPressed();
                    })
                    .setNegativeButton(R.string.no, (dialog, which) -> {
                        dialog.dismiss();
                        onBackPressedCallback.setEnabled(false);
                        mainActivity.onBackPressed();
                    });
            return builder.create();
        }

        @Override
        public void onClick (View v){
            int selectedItem;
            AlertDialog.Builder builder;
            AlertDialog dialog;
            switch (v.getId()) {
                case R.id.btnNavWiFiSettings:
                    navDirections = SettingsFragmentDirections.actionNavigationSettingsToWiFiSettingsFragment();
                    mainActivity.getNavController().navigate(navDirections);
                    break;
                case R.id.btnEmissivityHint:
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
                    break;
                case R.id.btnPalette:
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

        @Override
        public void onPause () {
            super.onPause();
            //changing the ip address will disconnect the camera
            if(!binding.cameraIPAddress.getText().toString().equals(settings.getCameraAddress())) {
                cameraService.setIpAddress(binding.cameraIPAddress.getText().toString());
            }
            if(cameraService.isConnected()) {
                cameraViewModel.setConfig();
            }
        }
    }