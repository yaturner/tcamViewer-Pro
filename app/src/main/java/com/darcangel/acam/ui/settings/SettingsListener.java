package com.darcangel.acam.ui.settings;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioGroup;

import androidx.lifecycle.ViewModel;

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.R;

import timber.log.Timber;

public class SettingsListener {
    private SettingsViewModel settingsViewModel;
    private String cameraAddress;
    private Boolean cameraFocus = new Boolean(false);

    public SettingsListener() {
        settingsViewModel = MainActivity.getInstance().getSettingsViewModel();
    }

    // UI callbacks
    public void onCameraIPAddressChanged(CharSequence text) {
        String address = text.toString();
        Timber.d("Camera Ip Address is " + address);
        //don't set the value until we lose focus
        this.cameraAddress = address;
    }

    public void onSwitchChanged(CompoundButton button, Boolean isChecked) {
        switch (button.getId()) {
            case R.id.switchSavePictureOnSave:
                Timber.d("Picture on Save is %s checked", (isChecked ? "" : "not"));
                break;
            case R.id.switchManualRange:
                Timber.d("Manual Range is %s checked", (isChecked ? "" : "not"));
                break;
        }
    }

    public void onButtonClicked(View buttonView) {
        switch (buttonView.getId()) {
            case R.id.btnExportResolution:
                Timber.d("Export Resolution button was clicked");
                break;
        }
    }

    public void onEmissivityTextChanged(CharSequence text) {
        Timber.d("Emissivity is " + text);
    }

    public void onMinRangeTextChanged(CharSequence text) {
        Timber.d("Min Range is " + text);
    }

    public void onMaxRangeTextChanged(CharSequence text) {
        Timber.d("Max Range is " + text);
    }

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
    };

    public void onFocusChange(View view,  Boolean hasFocus) {
        switch(view.getId()) {
            case R.id.cameraIPAddress:
                Timber.d("Camera address does %s have focus", (hasFocus?"":"not"));
                //if the cameraAddress had focus and is now losing it, update the address
                if(cameraFocus && !hasFocus) {
                    settingsViewModel.setCameraAddress(cameraAddress);
                }
                cameraFocus = hasFocus;
                break;
        }
    }
}

