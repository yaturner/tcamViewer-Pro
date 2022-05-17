package com.darcangel.acam.Settings;

import android.content.SharedPreferences;
import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.net.URL;

public class Settings {
    private enum UNITS {
        FAHRENHEIT,
        CELSIUS
    }

    private enum PALETTE {
        WHITE_HOT,
        BLACK_HOT,
        ARTIC,
        FUSION,
        IRON_BLACK,
        RAINBOW,
        RAINBOW_HC,
        SEPIA,
        BLEND
    }

    public enum EMISSIVITY {

    }

    private MutableLiveData<String> cameraAddress;                //remote address of camera
    private MutableLiveData<Integer> emissivity;
    private MutableLiveData<Pair<Integer, Integer>> manualRange;      //min, max range
    private MutableLiveData<UNITS> displayUnits;              // F or C
    private MutableLiveData<Float> streamRate;
    private MutableLiveData<Boolean> updateCameraClock;       // update camera clock when connected
    private MutableLiveData<Boolean> scaleDisplay;
    private MutableLiveData<Pair<Integer, Integer>> exportResolution; // HxW for exporting image
    private MutableLiveData<String> downloadFolder;
    private MutableLiveData<Boolean> autoRange;
    private MutableLiveData<Boolean> displaySpotmeter;
    private MutableLiveData<Boolean> exportMetadata;
    private MutableLiveData<PALETTE> palette;
    private MutableLiveData<Integer> streamDelay;




    public Settings(SharedPreferences prefs) {
        if(prefs == null || prefs.getAll() == null) {
            //initailize the prefs
            cameraAddress = new MutableLiveData<String>("");
        }
    }

    public LiveData<String> getCameraAddress() {
        if(cameraAddress == null) {
            cameraAddress = new MutableLiveData<>();
        }
        return cameraAddress;
    }

    public void setCameraAddress(String address) {
        cameraAddress.setValue(address);
    }


    public MutableLiveData<Integer> getEmissivity() {
        if (emissivity == null) {
            emissivity = new MutableLiveData<Integer>();
        }
        return emissivity;
    }

    public void setEmissivity(Integer value) {
        emissivity.setValue(value);
    }

}
