package com.darcangel.acam.ui.settings;

import android.util.Log;

import androidx.databinding.adapters.TextViewBindingAdapter;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

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
        Log.d("SettingsFragment", "Emissivity is " + text);
        setEmissivity(text.toString());
    }
}