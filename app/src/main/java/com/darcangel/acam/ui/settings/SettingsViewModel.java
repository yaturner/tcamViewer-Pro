package com.darcangel.acam.ui.settings;

import android.util.Pair;

import androidx.databinding.Bindable;
import androidx.databinding.Observable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.navigation.NavController;

import com.darcangel.acam.BuildConfig;
import com.darcangel.acam.MainActivity;
import com.darcangel.acam.R;

public class SettingsViewModel extends ViewModel {

    private SettingsListener settingsListener;

    //Hints
    private String[] emissivityString;
    private int[] emissivityValue;

    //Settings Fragment

    public SettingsViewModel() {
        init();
    }


    private void init() {
        //set the default values

        if (emissivityString == null || emissivityString.length == 0) {
            emissivityString = MainActivity.getInstance().getResources().getStringArray(R.array.emissivity_strings);
        }
        if (emissivityValue == null || emissivityValue.length == 0) {
            emissivityValue = MainActivity.getInstance().getResources().getIntArray(R.array.emissivity_values);
        }

    }

    //misc
    @Override
    public void onCleared() {
        // Dispose All your Subscriptions to avoid memory leaks
        super.onCleared();
    }
}