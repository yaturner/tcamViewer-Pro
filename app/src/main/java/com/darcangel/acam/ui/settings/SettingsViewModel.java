package com.darcangel.acam.ui.settings;

import android.util.Pair;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.darcangel.acam.BuildConfig;
import com.darcangel.acam.MainActivity;
import com.darcangel.acam.R;

public class SettingsViewModel extends ViewModel {

    private SettingsListener settingsListener;
    //Main Setting
    private String[] emissivityString;
    private int[] emissivityValue;
    private MutableLiveData<Boolean> AGC;
    private MutableLiveData<String> cameraAddress;                //remote address of camera
    private MutableLiveData<Integer> emissivity;
    private MutableLiveData<Boolean> exportOnSave;
    private MutableLiveData<Boolean> exportMetaData;
    private MutableLiveData<Boolean> exportResolution;
    private MutableLiveData<Pair<Integer, Integer>> exportResolutionValues; // HxW for exporting image
    private MutableLiveData<Boolean> manualRange;
    private MutableLiveData<Pair<Integer, Integer>> manualRangeValues;      //min, max range
    private MutableLiveData<String> palette;
    private MutableLiveData<Boolean> shutterSound;
    private MutableLiveData<Boolean> displaySpotmeter;
    private MutableLiveData<UNITS> displayUnits;              // F or C


    private MutableLiveData<Integer> gain;
    private MutableLiveData<Float> streamRate;
    private MutableLiveData<Boolean> updateCameraClock;       // update camera clock when connected
    private MutableLiveData<Boolean> scaleDisplay;
    private MutableLiveData<String> downloadFolder;
    private MutableLiveData<Boolean> autoRange;
    private MutableLiveData<Integer> streamDelay;

    public SettingsViewModel() {
        if (settingsListener == null) {
            settingsListener = new SettingsListener();
        }
        init();
    }

    //Getters and Setters
    public MutableLiveData<Boolean> getAGC() {
        return AGC;
    }

    public void setAGC(Boolean value) {
        this.AGC.postValue(value);
    }


    public MutableLiveData<Integer> getGain() {
        return gain;
    }

    public void setGain(Integer value) {
        this.gain.postValue(value);
    }

    public MutableLiveData<String> getCameraAddress() {
        return cameraAddress;
    }

    public void setCameraAddress(String address) {
        cameraAddress.setValue(address);
    }

    public MutableLiveData<Integer> getEmissivity() {
        return emissivity;
    }

    public void setEmissivity(Integer value) {
        emissivity.setValue(value);
    }

//    public MutableLiveData<Pair<Integer, Integer>> getManualRange() {
//        return manualRange.getValue();
//    }
//
//    public void setManualRange(MutableLiveData<Pair<Integer, Integer>> manualRange) {
//        this.manualRange = manualRange;
//    }

    public MutableLiveData<UNITS> getDisplayUnits() {
        return displayUnits;
    }

    public void setDisplayUnits(MutableLiveData<UNITS> displayUnits) {
        this.displayUnits = displayUnits;
    }

    public MutableLiveData<Float> getStreamRate() {
        return streamRate;
    }

    public void setStreamRate(MutableLiveData<Float> streamRate) {
        this.streamRate = streamRate;
    }

    public MutableLiveData<Boolean> getUpdateCameraClock() {
        return updateCameraClock;
    }

    public void setUpdateCameraClock(MutableLiveData<Boolean> updateCameraClock) {
        this.updateCameraClock = updateCameraClock;
    }

    public MutableLiveData<Boolean> getScaleDisplay() {
        return scaleDisplay;
    }

    public void setScaleDisplay(MutableLiveData<Boolean> scaleDisplay) {
        this.scaleDisplay = scaleDisplay;
    }

//    public MutableLiveData<Pair<Integer, Integer>> getExportResolution() {
//        return exportResolution;
//    }
//
//    public void setExportResolution(MutableLiveData<Pair<Integer, Integer>> exportResolution) {
//        this.exportResolution = exportResolution;
//    }

    public MutableLiveData<String> getDownloadFolder() {
        return downloadFolder;
    }

    public void setDownloadFolder(MutableLiveData<String> downloadFolder) {
        this.downloadFolder = downloadFolder;
    }

    public MutableLiveData<Boolean> getAutoRange() {
        return autoRange;
    }

    public void setAutoRange(MutableLiveData<Boolean> autoRange) {
        this.autoRange = autoRange;
    }

    public MutableLiveData<Boolean> getDisplaySpotmeter() {
        return displaySpotmeter;
    }

    public void setDisplaySpotmeter(MutableLiveData<Boolean> displaySpotmeter) {
        this.displaySpotmeter = displaySpotmeter;
    }

    public MutableLiveData<Boolean> getExportMetaData() {
        return exportMetaData;
    }

    public void setExportMetaData(Boolean value) {
        this.exportMetaData.postValue(value);
    }

    public MutableLiveData<String> getPalette() {
        return palette;
    }

    public void setPalette(String palette) {
        this.palette.postValue(palette);
    }

    public MutableLiveData<Integer> getStreamDelay() {
        return streamDelay;
    }

    public void setStreamDelay(MutableLiveData<Integer> streamDelay) {
        this.streamDelay = streamDelay;
    }

    public SettingsListener getSettingsListener() {
        return settingsListener;
    }

    public String getEmissivityString(final int index) {
        return emissivityString[index];
    }

    public int getEmissivityValue(final int index) {
        return emissivityValue[index];
    }

    public void setAGC(MutableLiveData<Boolean> AGC) {
        this.AGC = AGC;
    }

    public void setCameraAddress(MutableLiveData<String> cameraAddress) {
        this.cameraAddress = cameraAddress;
    }

    public void setEmissivity(MutableLiveData<Integer> emissivity) {
        this.emissivity = emissivity;
    }

    private void init() {
        //set the default values
        if (BuildConfig.DEBUG) {
            cameraAddress = new MutableLiveData<String>("192.168.0.42");
        } else {
            cameraAddress = new MutableLiveData<String>();
        }
        if (emissivityString == null || emissivityString.length == 0) {
            emissivityString = MainActivity.getInstance().getResources().getStringArray(R.array.emissivity_strings);
        }
        if (emissivityValue == null || emissivityValue.length == 0) {
            emissivityValue = MainActivity.getInstance().getResources().getIntArray(R.array.emissivity_values);
        }

        AGC = new MutableLiveData<Boolean>();
        emissivity = new MutableLiveData<Integer>();
        manualRange = new MutableLiveData<Boolean>();
        manualRangeValues = new MutableLiveData<Pair<Integer, Integer>>();
        displayUnits = new MutableLiveData<UNITS>();
        streamRate = new MutableLiveData<Float>();
        updateCameraClock = new MutableLiveData<Boolean>();
        scaleDisplay = new MutableLiveData<Boolean>();
        exportResolution = new MutableLiveData<Boolean>();
        exportResolutionValues = new MutableLiveData<Pair<Integer, Integer>>();
        downloadFolder = new MutableLiveData<String>();
        autoRange = new MutableLiveData<Boolean>();
        displaySpotmeter = new MutableLiveData<Boolean>();
        exportMetaData = new MutableLiveData<Boolean>();
        palette = new MutableLiveData<String>();
        streamDelay = new MutableLiveData<Integer>();
    }

    //misc
    @Override
    public void onCleared() {
        // Dispose All your Subscriptions to avoid memory leaks
        super.onCleared();
    }

    //Misc
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
        BANDED
    }
}