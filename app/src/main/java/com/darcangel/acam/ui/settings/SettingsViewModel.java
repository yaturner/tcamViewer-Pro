package com.darcangel.acam.ui.settings;

import android.util.Log;
import android.widget.Button;
import android.widget.CompoundButton;
import android.view.View;

import androidx.databinding.adapters.TextViewBindingAdapter;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.darcangel.acam.R;

import timber.log.Timber;

public class SettingsViewModel extends ViewModel  {

    public MutableLiveData<String> emissivity;

    public SettingsViewModel() {
        emissivity = new MutableLiveData<String>();
        emissivity.setValue("98.6");
    }

    public MutableLiveData<String> getEmissivity() {
        if(emissivity == null) {
            emissivity = new MutableLiveData<String>();
        }
        return emissivity;
    }

    public void setEmissivity(String value) {
        emissivity.setValue(value);
    }


    public void onEmissivityTextChanged(CharSequence text) {
        Log.d("SettingsViewModel", "Emissivity is " + text);
        setEmissivity(text.toString());
    }

    public void onGainChanged(int checkedId) {
        Log.d("SettingsViewModel", "Gain is " + checkedId);
        switch(checkedId) {
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
    }

    public void onCameraIPAddressChanged(CharSequence text) {
        Timber.d("Camera Ip Address is " + text);
    }

    public void onSwitchChanged(CompoundButton button, Boolean isChecked) {
        switch(button.getId()) {
            case R.id.switchSavePictureOnSave:
                Timber.d("Picture on Save is %s checked", (isChecked?"":"not"));
                break;
        }
    }

    public void onButtonClicked(View buttonView) {
        switch(buttonView.getId()) {
            case R.id.btnExportResolution:
                Timber.d("Export Resolution button was clicked");
                break;
        }
    }
}