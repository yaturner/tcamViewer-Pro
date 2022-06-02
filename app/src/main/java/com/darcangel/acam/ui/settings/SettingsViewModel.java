package com.darcangel.acam.ui.settings;

import android.util.Pair;

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
    private MutableLiveData<Boolean> AGC;
    private MutableLiveData<String> cameraAddress;                //IP address of camera
    private MutableLiveData<Integer> emissivity;
    private MutableLiveData<Boolean> exportOnSave;
    private MutableLiveData<Boolean> exportMetaData;
    private MutableLiveData<Integer> exportResolution;  // HxW for exporting image
    private MutableLiveData<Pair<Integer, Integer>> manualRange;      //min, max range
    private MutableLiveData<String> palette;
    private MutableLiveData<Boolean> shutterSound;
    private MutableLiveData<Boolean> displaySpotmeter;
    private MutableLiveData<UNITS> Units;              // F or C


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

    /**
     * AGC
     */
    public MutableLiveData<Boolean> getAGC() {
        if(AGC == null) {
            AGC = new MutableLiveData<Boolean>();
        }
        return AGC;
    }

    public void setAGC(Boolean value) {
        if(AGC == null) {
            AGC = new MutableLiveData<Boolean>();
        }
        this.AGC.postValue(value);
    }


    /**
     *
     * Gain
     */
    public MutableLiveData<Integer> getGain() {
        if(gain == null) {
            gain = new MutableLiveData<Integer>();
        }
        return gain;
    }

    public void setGain(Integer value) {
        if(gain == null) {
            gain = new MutableLiveData<Integer>();
        }
        this.gain.postValue(value);
    }

    /**
     *
     * CameraAddress
     */
    public MutableLiveData<String> getCameraAddress() {
        if(cameraAddress == null) {
            cameraAddress = new MutableLiveData<>();
        }
        return cameraAddress;
    }

    public void setCameraAddress(String address) {
        if(cameraAddress == null) {
            cameraAddress = new MutableLiveData<>();
        }
        cameraAddress.setValue(address);
    }

    /**
     *
     * emissivity
     */
    public MutableLiveData<Integer> getEmissivity() {
        if(emissivity == null) {
            emissivity = new MutableLiveData<Integer>();
        }
        return emissivity;
    }

    public void setEmissivity(Integer value) {
        if(emissivity == null) {
            emissivity = new MutableLiveData<>();
        }
        emissivity.setValue(value);
    }

    /**
     *
     * exportOnSave
     */
    public MutableLiveData<Boolean> getExportOnSave() {
        if(exportOnSave == null) {
            exportOnSave = new MutableLiveData<>();
        }
        return exportOnSave;
    }

    public void setExportOnSave(Boolean value) {
        if(exportOnSave == null) {
            exportOnSave = new MutableLiveData<>();
        }
        this.exportOnSave.setValue(value);
    }

    /**
     *
     * manual range
     */
    public MutableLiveData<Pair<Integer, Integer>> getManualRange() {
        if(manualRange == null) {
            manualRange = new MutableLiveData<>();
        }
        return manualRange;
    }

    public void setManualRange(Integer min, Integer max) {
        if(manualRange == null) {
            manualRange = new MutableLiveData<>();
        }
        this.manualRange.setValue(new Pair<Integer, Integer>(min, max));
    }

    public void setManualRange(Pair<Integer, Integer> value) {
        if(manualRange == null) {
            manualRange = new MutableLiveData<>();
        }
        this.manualRange.setValue(value);
    }

    /**
     *
     * display units
     */
    public MutableLiveData<UNITS> getUnits() {
        if(Units == null) {
            Units = new MutableLiveData<>();
        }
        return Units;
    }

    public void setUnits(UNITS value) {
        if(Units == null) {
            Units = new MutableLiveData<>();
        }
        this.Units.setValue(value);
    }

    /**
     *
     * stream rate
     */
    public MutableLiveData<Float> getStreamRate() {
        if(streamRate == null) {
            streamRate = new MutableLiveData<>();
        }
        return streamRate;
    }

    public void setStreamRate(Float value) {
        if(streamRate == null) {
            streamRate = new MutableLiveData<>();
        }
        this.streamRate.setValue(value);
    }

    /**
     *
     * update camera clock
     */
    public MutableLiveData<Boolean> getUpdateCameraClock() {
        if(updateCameraClock == null) {
            updateCameraClock = new MutableLiveData<>();
        }
        return updateCameraClock;
    }

    public void setUpdateCameraClock(Boolean value) {
        if(updateCameraClock == null) {
            updateCameraClock = new MutableLiveData<>();
        }
        this.updateCameraClock.setValue(value);
    }

    /**
     *
     * scale display
     */
    public MutableLiveData<Boolean> getScaleDisplay() {
        if(scaleDisplay == null) {
            scaleDisplay = new MutableLiveData<>();
        }
        return scaleDisplay;
    }

    public void setScaleDisplay(Boolean value) {
        if(scaleDisplay == null) {
            scaleDisplay = new MutableLiveData<>();
        }
        this.scaleDisplay.setValue(value);
    }

    /**
     *
     * export resolution
     */
    public MutableLiveData<Integer> getExportResolution() {
        if(exportResolution == null) {
            exportResolution = new MutableLiveData<>();
        }
        return exportResolution;
    }

    public void setExportResolution(Integer value) {
        if(exportResolution == null) {
            exportResolution = new MutableLiveData<>();
        }
        this.exportResolution.setValue(value);
    }

    /**
     *
     * download folder
     */
    public MutableLiveData<String> getDownloadFolder() {
        if(downloadFolder == null) {
            downloadFolder = new MutableLiveData<>();
        }
        return downloadFolder;
    }

    public void setDownloadFolder(String value) {
        if(downloadFolder == null) {
            downloadFolder = new MutableLiveData<>();
        }
        this.downloadFolder.setValue(value);
    }

    /**
     *
     * auto range
     */
    public MutableLiveData<Boolean> getAutoRange() {
        if(autoRange == null) {
            autoRange = new MutableLiveData<>();
        }
        return autoRange;
    }

    public void setAutoRange(Boolean value) {
        if(autoRange == null) {
            autoRange = new MutableLiveData<>();
        }
        this.autoRange.setValue(value);
    }

    /**
     *
     * display spot meter
     */
    public MutableLiveData<Boolean> getDisplaySpotmeter() {
        if(displaySpotmeter == null) {
            displaySpotmeter = new MutableLiveData<>();
        }
        return displaySpotmeter;
    }

    public void setDisplaySpotmeter(Boolean value) {
        if(displaySpotmeter == null) {
            displaySpotmeter = new MutableLiveData<>();
        }
        this.displaySpotmeter.setValue(value);
    }

    /**
     *
     * export metadata
     */
    public MutableLiveData<Boolean> getExportMetaData() {
        if(exportMetaData == null) {
            exportMetaData = new MutableLiveData<>();
        }
        return exportMetaData;
    }

    public void setExportMetaData(Boolean value) {
        if(exportMetaData == null) {
            exportMetaData = new MutableLiveData<>();
        }
        this.exportMetaData.setValue(value);
    }

    /**
     *
     * palette
     */
    public MutableLiveData<String> getPalette() {
        if(palette == null) {
            palette = new MutableLiveData<>();
        }
        return palette;
    }

    public void setPalette(String value) {
        if(palette == null) {
            palette = new MutableLiveData<String>();
        }
        this.palette.setValue(value);
    }

    /**
     *
     * shutter sound
     */
    public MutableLiveData<Boolean> getShutterSound() {
        if (shutterSound == null) {
            shutterSound = new MutableLiveData<>();
        }
        return shutterSound;
    }

    public void setShutterSound(Boolean value) {
        if (shutterSound == null) {
            shutterSound = new MutableLiveData<>();
        }
        shutterSound.setValue(value);
    }

    /**
     *
     * stream delay
     */
    public MutableLiveData<Integer> getStreamDelay() {
        if(streamDelay == null) {
            streamDelay = new MutableLiveData<>();
        }
        return streamDelay;
    }

    public void setStreamDelay(Integer value) {
        if(streamDelay == null) {
            streamDelay = new MutableLiveData<>();
        }
        this.streamDelay.setValue(value);
    }

    /**
     * get the string value for a predefined emissivity value
     * @param index
     * @return
     */
    public String getEmissivityString(final int index) {
        return emissivityString[index];
    }

    public int getEmissivityValue(final int index) {
        return emissivityValue[index];
    }

    /**
     * SettingsListener
     * @return settings listener
     */
    public SettingsListener getSettingsListener() {
        return settingsListener;
    }

    private void init() {
        //set the default values
        if (BuildConfig.DEBUG) {
            setCameraAddress("192.168.0.42");
        }

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

    //Misc
    public enum UNITS {
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