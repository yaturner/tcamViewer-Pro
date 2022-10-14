package com.darcangel.tcamViewer.ui.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

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
import com.darcangel.tcamViewer.model.Settings;
import com.darcangel.tcamViewer.databinding.FragmentSettingsBinding;
import com.darcangel.tcamViewer.ui.camera.CameraService;
import com.darcangel.tcamViewer.ui.camera.CameraViewModel;
import com.darcangel.tcamViewer.utils.CameraUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsFragment extends Fragment implements View.OnClickListener,
        RadioGroup.OnCheckedChangeListener,
        CompoundButton.OnCheckedChangeListener
{

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
    private CameraUtils cameraUtils;
    private View root;
    private Bundle snapshot;
    private String selectedPalette;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.container = container;
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        if (mainActivity == null) {
            mainActivity = MainActivity.getInstance();
        }
        cameraService = mainActivity.getCameraService();
        cameraUtils = mainActivity.getCameraUtils();
        settingsViewModel = mainActivity.getSettingsViewModel();
        cameraViewModel = mainActivity.getCameraViewModel();

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        //get the settings model
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
                hadFocus = false;
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
        binding.switchManualRange.setChecked(settings.getManualRange().getValue());
        binding.rbUnitsF.setChecked(settings.getUnitsF().getValue());
        binding.rbUnitsC.setChecked(settings.getUnitsC().getValue());

        //If the user does not save the settings, isRemapNeeded is set to false
        settings.getManualRangeMax().observe(mainActivity, v -> {
            if (v != null
                    && !v.toString().isEmpty()
                    && !binding.etManualRangeMax.getText().toString().isEmpty()
                    && !settings.getManualRangeMax().getValue().toString()
                    .equalsIgnoreCase(v.toString())) {
                cameraViewModel.setRemapNeeded(true);
            }
        });
        settings.getManualRangeMin().observe(mainActivity, v -> {
            if (v != null
                    && !v.toString().isEmpty()
                    && !binding.etManualRangeMax.getText().toString().isEmpty()
                    && !settings.getManualRangeMax().getValue().toString()
                    .equalsIgnoreCase(v.toString())) {
                cameraViewModel.setRemapNeeded(true);
            }
        });
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
        binding.switchManualRange.setOnCheckedChangeListener(this);
        binding.rgUnits.setOnCheckedChangeListener(this);

        //If Manual Range is checked show the values
        if(settings.getManualRange().getValue()) {
            binding.layoutManualRange.setVisibility(View.VISIBLE);
        }

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
                    if(!cameraService.getIpAddress().
                            equalsIgnoreCase(settings.getCameraAddress().getValue())) {
                        cameraService.setIpAddress(settings.getCameraAddress().getValue());
                    }
                    //if AGC or Manual Range changed, we need a remap
                    //we wait till now to persist these settings so we can tell if they have changed
                    if(binding.switchManualRange.isChecked() != settings.getManualRange().getValue()
                            || binding.switchAGC.isChecked() != settings.getAGC().getValue()
                    ) {
                        cameraViewModel.setRemapNeeded(true);
                        settings.setAGC(binding.switchAGC.isChecked());
                        settings.setManualRange(binding.switchManualRange.isChecked());
                    }
                    //If ManualRange is checked do the same for it's values
                    if(settings.getManualRange().getValue()) {
                        if (!binding.etManualRangeMax.getText().toString()
                                .equalsIgnoreCase(settings.getManualRangeMax().getValue().toString())
                                || !binding.etManualRangeMin.getText().toString()
                                .equalsIgnoreCase(settings.getManualRangeMin().getValue().toString())) {
                            cameraViewModel.setRemapNeeded(true);
                            try {
                                settings.setManualRangeMin(
                                        Float.parseFloat(binding.etManualRangeMin.getText().toString()));
                                settings.setManualRangeMax(
                                        Float.parseFloat(binding.etManualRangeMax.getText().toString()));
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    settings.persist();
                    dialog.dismiss();
                    onBackPressedCallback.setEnabled(false);
                    Navigation.findNavController(root).popBackStack();
                })
                .setNegativeButton(R.string.no, (dialog, which) -> {
                    dialog.dismiss();
                    settings.restore(snapshot);
                    cameraViewModel.setRemapNeeded(false);
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
    }

    @Override
    public void onClick(View v) {
        int selectedItem;
        AlertDialog.Builder builder;
        AlertDialog dialog;
        String[] paletteList = mainActivity.getPaletteFactory().getPaletteNames();
        int checkedItem;
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
            // setup the alert builder
            builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Select Palette");
            for (checkedItem = 0; checkedItem < paletteList.length; checkedItem++) {
                if (settings.getPalette().getValue().equalsIgnoreCase(paletteList[checkedItem])) {
                    break;
                }
            }
            // add a radio button list
            builder.setSingleChoiceItems(paletteList, checkedItem, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    selectedPalette = mainActivity.getPaletteFactory().getPaletteName(which);
                }
            });
            // add OK and Cancel buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    settings.setPalette(selectedPalette);
                }
            });
            builder.setNegativeButton("Cancel", null);
            // create and show the alert dialog
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
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        //cameraViewModel.isRemapNeeded is set only if the user saves the settings, not here
        //same for settings
        int id = buttonView.getId();
        if (id == R.id.switchAGC) {
            settings.setAGC(isChecked);
            Toast.makeText(getContext(), R.string.agc_changes_info, Toast.LENGTH_LONG).show();
        } else if(id == R.id.switchManualRange) {
            settings.setManualRange(isChecked);
            cameraViewModel.setRemapNeeded(true);
            if(cameraViewModel.getImageDto().getValue().getBitmap() != null) {
                Pair<Float, Float> temps = cameraViewModel.getImageDto().getValue().getTemperatures();
                settings.setManualRangeMin(temps.first);
                settings.setManualRangeMax(temps.second);
//            } else {
//                settings.setManualRangeMax(100.0f);
//                settings.setManualRangeMin(0f);
            }
            if(isChecked) {
                binding.layoutManualRange.setVisibility(View.VISIBLE);
            } else {
                binding.layoutManualRange.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        String min;
        String max;
        if(group.getId() == R.id.rgUnits) {
            if(checkedId == R.id.rbUnitsC) {
                //convert from F to C
                if(settings.getUnitsF().getValue()) {
                    settings.setUnitsC(true);
                    settings.setUnitsF(false);
                    if(settings.getManualRange().getValue()) {
                        min = convertFtoC(settings.getManualRangeMin().getValue());
                        max = convertFtoC(settings.getManualRangeMax().getValue());
                        binding.etManualRangeMin.setText(min);
                        binding.etManualRangeMax.setText(max);
                        settings.setManualRangeMin(Float.parseFloat(min));
                        settings.setManualRangeMax(Float.parseFloat(max));
                    }
                }
            }
            if(checkedId == R.id.rbUnitsF) {
                if(settings.getUnitsC().getValue()) {
                    settings.setUnitsC(false);
                    settings.setUnitsF(true);
                    if(settings.getManualRange().getValue()) {
                        min = convertCtoF(settings.getManualRangeMin().getValue());
                        max = convertCtoF(settings.getManualRangeMax().getValue());
                        binding.etManualRangeMin.setText(min);
                        binding.etManualRangeMax.setText(max);
                        settings.setManualRangeMin(Float.parseFloat(min));
                        settings.setManualRangeMax(Float.parseFloat(max));
                    }
                }
            }
//            cameraUtils.setUnitsCelsius(settings.getUnitsC().getValue());
        }
    }

    public String convertFtoC(float value) {
        float c = (value - 32f) * (5f/9f);
        String s = String.format("%3.1f", c);
        return s;
    }

    public String convertCtoF(float value) {
        float c = (value * (9f/5f)) + 32f;
        String s = String.format("%3.1f", c);
        return s;
    }
}