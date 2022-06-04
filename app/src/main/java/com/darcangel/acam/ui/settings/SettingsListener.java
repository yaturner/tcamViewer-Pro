package com.darcangel.acam.ui.settings;

import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.RadioGroup;

import androidx.databinding.BaseObservable;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.R;

import java.util.regex.Pattern;

import timber.log.Timber;

public class SettingsListener extends BaseObservable {
    private SettingsViewModel settingsViewModel;
    private String editText = "";
    private boolean hadFocus = false;

    private static final Pattern IP_PATTERN = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");


    public SettingsListener() {
        settingsViewModel = MainActivity.getInstance().getSettingsViewModel();
    }

    // UI callbacks
//    public void onCameraIPAddressChanged(CharSequence text) {
//        String address = text.toString();
//        Timber.d("Camera Ip Address is " + address);
//        //don't set the value until we lose focus
//        this.cameraAddress = address;
//    }

    public void onSwitchChanged(CompoundButton button, Boolean isChecked) {
        switch (button.getId()) {
            case R.id.switchSavePictureOnSave:
                Timber.d("Picture on Save is %s checked", (isChecked ? "" : "not"));
                break;
            case R.id.switchManualRange:
                Timber.d("Manual Range is %s checked", (isChecked ? "" : "not"));
                break;
            case R.id.switchAGC:
                Timber.d("AGC is %s checked", (isChecked ? "" : "not"));
                settingsViewModel.setAGC(isChecked);
                break;
                //WiFi Settings
            case R.id.swCameraIsAccessPoint:
                break;
        }
    }

    public void onButtonClicked(View buttonView) {
        switch (buttonView.getId()) {
            case R.id.btnNavWiFiSettings:
                NavDirections navDirections = SettingsFragmentDirections.actionNavigationSettingsToWiFiSettingsFragment();
                MainActivity.getInstance().getNavController().navigate(navDirections);
                break;
        }
    }

    public void onEditTextChanged(CharSequence text) {
        Timber.d("editText is " + text);
        editText = text.toString();
    }

//    public void onMinRangeTextChanged(CharSequence text) {
//        Timber.d("Min Range is " + text);
//    }

 //   public void onMaxRangeTextChanged(CharSequence text) {
 //       Timber.d("Max Range is " + text);
 //   }

    public void onRadioGroupChanged(RadioGroup group, int checkedId) {
        Timber.d("Gain is " + checkedId);
        switch (group.getId()) {
            case R.id.rbGainAuto:
                switch (checkedId) {
                    case R.id.rbGainAuto:
                        Timber.d("Gain is Auto");
                        break;
                    case R.id.rbGainLow:
                        Timber.d("Gain is Low");
                        break;
                    case R.id.rbGainHigh:
                        Timber.d("Gain is High");
                        break;
                }
                break;
            case R.id.rgUnits:
                switch (checkedId) {
                    case R.id.rbUnitsC:
                        break;
                    case R.id.rbUnitsF:
                        break;
                }
                break;
        }
    }

    ;

    public void onFocusChange(View view, Boolean hasFocus) {
        switch (view.getId()) {
            case R.id.cameraIPAddress:
                Timber.d("Camera address does %s have focus", (hasFocus ? "" : "not"));
                //if the cameraAddress had focus and is now losing it, update the address
                if (hadFocus && !hasFocus) {
                    if (isValidIPAddress(editText)) {
                        settingsViewModel.getCameraAddress().setValue(editText);
                        editText = "";
                        break;
                    }
                }
                hadFocus = hasFocus;
                break;
            case R.id.etEmissivity:
                Timber.d("Emissivity does %s have focus", (hasFocus ? "" : "not"));
                if (hadFocus && !hasFocus) {
                    try {
                        if(editText != null && !editText.isEmpty()) {
                            settingsViewModel.getEmissivity().setValue(Integer.parseInt(editText));
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    editText = "";
                    break;
                }
                hadFocus = hasFocus;
                break;
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        if (view == null) {
            return;
        }
        switch(parent.getId()) {
            case R.id.spExportResolution:
                settingsViewModel.getExportResolution().setValue(pos);
                break;
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {

    }

    private Boolean isValidIPAddress(String address) {
        return IP_PATTERN.matcher(address).matches();
    }
}