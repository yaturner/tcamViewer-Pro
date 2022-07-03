package com.darcangel.acam.ui.settings;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.MainActivity.CameraCallback;
import com.darcangel.acam.R;
import com.darcangel.acam.adapters.EmissivityDialogListAdapter;
import com.darcangel.acam.constants.Constants;
import com.darcangel.acam.container.Settings;
import com.darcangel.acam.databinding.FragmentSettingsBinding;

import org.json.JSONException;
import org.json.JSONObject;

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

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.container = container;

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (mainActivity == null) {
            mainActivity = MainActivity.getInstance();
        }

        settingsViewModel = mainActivity.getSettingsViewModel();

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        settings = mainActivity.getSettings();
        binding.setSettings(settings);

        binding.btnCancelSave.btnCancel.setOnClickListener(this);
        binding.btnCancelSave.btnSave.setOnClickListener(this);

        emValues = mainActivity.getResources().getIntArray(R.array.emissivity_values);


        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mainActivity.getCameraService().isConnected()) {
            binding.btnNavWiFiSettings.setEnabled(true);
            binding.btnNavWiFiSettings.setOnClickListener(this);
            binding.btnCancelSave.btnSave.setEnabled(true);
        } else {
            binding.btnNavWiFiSettings.setEnabled(false);
            binding.btnCancelSave.btnSave.setEnabled(false);
        }
        binding.btnEmissivityHint.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnNavWiFiSettings:
                navDirections = SettingsFragmentDirections.actionNavigationSettingsToWiFiSettingsFragment();
                mainActivity.getNavController().navigate(navDirections);
                break;
            case R.id.btnCancel:
                navDirections = SettingsFragmentDirections.actionNavigationSettingsToNavigationCamera();
                mainActivity.getNavController().navigate(navDirections);
                break;
            case R.id.btnSave:
                settings.persist();
                setConfig();
                navDirections = SettingsFragmentDirections.actionNavigationSettingsToNavigationCamera();
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

    private void setConfig() {
        String args = String.format(Constants.ARGS_SET_CONFIG,
                (settings.getAGC() ? 1 : 0),
                settings.getEmissivity(),
                (settings.getGainAuto() ? 2 : settings.getGainLow() ? 1 : 0));
        String cmd = String.format(Constants.CMD_SET_CONFIG, args);
        CameraCallback callback = new CameraCallback() {
            @Override
            public void callback(JSONObject response) {
                try {
                    if (response.has("result")) {
                        if (!response.getString("result").equals("OK")) {
                            //TODO handle error
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        try {
            ((MainActivity) mainActivity).getCameraService().sendCmd(cmd, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}