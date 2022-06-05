package com.darcangel.acam.model;

import android.util.Pair;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.lifecycle.MutableLiveData;

import com.darcangel.acam.BR;
import com.darcangel.acam.ui.settings.SettingsViewModel;

import java.nio.channels.NonReadableChannelException;

public class Settings extends BaseObservable {

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
    private MutableLiveData<String> units;              // F or C

    //WiFi Settings
    private MutableLiveData<Boolean> accessPoint;
    private MutableLiveData<Integer> gain;
    private MutableLiveData<Float> streamRate;
    private MutableLiveData<Boolean> updateCameraClock;       // update camera clock when connected
    private MutableLiveData<Boolean> scaleDisplay;
    private MutableLiveData<String> downloadFolder;
    private MutableLiveData<Boolean> autoRange;
    private MutableLiveData<Integer> streamDelay;

    public Settings() {
        setAGC(true);
    }

    //Getters and Setters

    /**
     * AGC
     */
    @Bindable
    public Boolean getAGC() {
        if(AGC == null) {
            AGC = new MutableLiveData<>();
        }
        return AGC.getValue();
    }

    public void setAGC(Boolean value) {
        if(AGC == null) {
            AGC = new MutableLiveData<>();
        }
        if (value != AGC.getValue()) {
            AGC.setValue(value);
        }
        notifyPropertyChanged(BR.aGC);
    }

    /**
     * Gain
     */
    @Bindable
    public Integer getGain() {
        if (gain == null) {
            gain = new MutableLiveData<Integer>();
        }
        return gain.getValue();
    }

    public void setGain(Integer value) {
        if (gain == null) {
            gain = new MutableLiveData<Integer>();
        }
        if (value != gain.getValue()) {
            gain.postValue(value);
            notifyPropertyChanged(BR.gain);
        }
    }

    /**
     * CameraAddress
     */
    @Bindable
    public String getCameraAddress() {
        if (cameraAddress == null) {
            cameraAddress = new MutableLiveData<>();
        }
        return cameraAddress.getValue();
    }

    public void setCameraAddress(String address) {
        if (cameraAddress == null) {
            cameraAddress = new MutableLiveData<>();
        }
        if (!address.equals(cameraAddress.getValue())) {
            cameraAddress.postValue(address);
            notifyPropertyChanged(BR.cameraAddress);
        }
    }

    /**
     * emissivity
     */
    @Bindable
    public Integer getEmissivity() {
        if (emissivity == null) {
            emissivity = new MutableLiveData<Integer>();
        }
        return emissivity.getValue();
    }

    public void setEmissivity(Integer value) {
        if (emissivity == null) {
            emissivity = new MutableLiveData<>();
        }
        if (value != emissivity.getValue()) {
            emissivity.postValue(value);
            notifyPropertyChanged(BR.emissivity);
        }
    }

    /**
     * exportOnSave
     */
    @Bindable
    public Boolean getExportOnSave() {
        if (exportOnSave == null) {
            exportOnSave = new MutableLiveData<>();
        }
        return exportOnSave.getValue();
    }

    public void setExportOnSave(Boolean value) {
        if (exportOnSave == null) {
            exportOnSave = new MutableLiveData<>();
        }
        if (value != exportOnSave.getValue()) {
            exportOnSave.postValue(value);
            notifyPropertyChanged(BR.exportOnSave);
        }
    }

    /**
     *
     * manual range
     */
    @Bindable
    public Pair<Integer, Integer> getManualRange() {
        if(manualRange == null) {
            manualRange = new MutableLiveData<>();
        }
        return manualRange.getValue();
    }

    public void setManualRange(Integer min, Integer max) {
        if(manualRange == null) {
            manualRange = new MutableLiveData<>();
        }
        if(min != manualRange.getValue().first || max != manualRange.getValue().second) {
            manualRange.postValue(new Pair<Integer, Integer>(min, max));
            notifyPropertyChanged(BR.manualRange);
        }
    }

    public void setManualRange(Pair<Integer, Integer> value) {
        if (manualRange == null) {
            manualRange = new MutableLiveData<>();
        }
        if (value.first != manualRange.getValue().first || value.second != manualRange.getValue().second) {
            manualRange.postValue(value);
            notifyPropertyChanged(BR.manualRange);
        }
    }

    /**
     *
     * display units
     */
    @Bindable
    public String getUnits() {
        if(units == null) {
            units = new MutableLiveData<>();
        }
        return units.getValue();
    }

    public void setUnits(String value) {
        if (units == null) {
            units = new MutableLiveData<>();
        }
        if (!value.equals(units.getValue())) {
            units.postValue(value);
            notifyPropertyChanged(BR.units);
        }
    }

    /**
     *
     * stream rate
     */
    @Bindable
    public Float getStreamRate() {
        if(streamRate == null) {
            streamRate = new MutableLiveData<>();
        }
        return streamRate.getValue();
    }

    public void setStreamRate(Float value) {
        if (streamRate == null) {
            streamRate = new MutableLiveData<>();
        }
        if (value != streamRate.getValue()) {
            streamRate.postValue(value);
            notifyPropertyChanged(BR.streamRate);
        }
    }

    /**
     *
     * update camera clock
     */
    @Bindable
    public Boolean getUpdateCameraClock() {
        if(updateCameraClock == null) {
            updateCameraClock = new MutableLiveData<>();
        }
        return updateCameraClock.getValue();
    }

    public void setUpdateCameraClock(Boolean value) {
        if (updateCameraClock == null) {
            updateCameraClock = new MutableLiveData<>();
        }
        if (value != updateCameraClock.getValue()) {
            updateCameraClock.postValue(value);
            notifyPropertyChanged(BR.updateCameraClock);
        }
    }

    /**
     *
     * scale display
     */
    @Bindable
    public Boolean getScaleDisplay() {
        if(scaleDisplay == null) {
            scaleDisplay = new MutableLiveData<>();
        }
        return scaleDisplay.getValue();
    }

    public void setScaleDisplay(Boolean value) {
        if (scaleDisplay == null) {
            scaleDisplay = new MutableLiveData<>();
        }
        if (value != scaleDisplay.getValue()) {
            scaleDisplay.postValue(value);
            notifyPropertyChanged(BR.scaleDisplay);
        }
    }

    /**
     *
     * export resolution
     */
    @Bindable
    public Integer getExportResolution() {
        if(exportResolution == null) {
            exportResolution = new MutableLiveData<>();
        }
        return exportResolution.getValue();
    }

    public void setExportResolution(Integer value) {
        if (exportResolution == null) {
            exportResolution = new MutableLiveData<>();
        }
        if (value != exportResolution.getValue()) {
            exportResolution.postValue(value);
            notifyPropertyChanged(BR.exportOnSave);
        }
    }

    /**
     *
     * download folder
     */
    @Bindable
    public String getDownloadFolder() {
        if(downloadFolder == null) {
            downloadFolder = new MutableLiveData<>();
        }
        return downloadFolder.getValue();
    }

    public void setDownloadFolder(String value) {
        if (downloadFolder == null) {
            downloadFolder = new MutableLiveData<>();
        }
        if (!value.equals(downloadFolder.getValue())) {
            downloadFolder.postValue(value);
            notifyPropertyChanged(BR.downloadFolder);
        }
    }

    /**
     *
     * auto range
     */
    @Bindable
    public Boolean getAutoRange() {
        if(autoRange == null) {
            autoRange = new MutableLiveData<>();
        }
        return autoRange.getValue();
    }

    public void setAutoRange(Boolean value) {
        if (autoRange == null) {
            autoRange = new MutableLiveData<>();
        }
        if (value != autoRange.getValue()) {
            autoRange.postValue(value);
            notifyPropertyChanged(BR.autoRange);
        }
    }

    /**
     *
     * display spot meter
     */
    @Bindable
    public Boolean getDisplaySpotmeter() {
        if(displaySpotmeter == null) {
            displaySpotmeter = new MutableLiveData<>();
        }
        return displaySpotmeter.getValue();
    }

    public void setDisplaySpotmeter(Boolean value) {
        if (displaySpotmeter == null) {
            displaySpotmeter = new MutableLiveData<>();
        }
        if (value != displaySpotmeter.getValue()) {
            displaySpotmeter.postValue(value);
            notifyPropertyChanged(BR.displaySpotmeter);
        }
    }

    /**
     *
     * export metadata
     */
    @Bindable
    public Boolean getExportMetaData() {
        if(exportMetaData == null) {
            exportMetaData = new MutableLiveData<>();
        }
        return exportMetaData.getValue();
    }

    public void setExportMetaData(Boolean value) {
        if(exportMetaData == null) {
            exportMetaData = new MutableLiveData<>();
        }
        if(value != getExportMetaData().booleanValue()) {
            exportMetaData.postValue(value);
            notifyPropertyChanged(BR.exportMetaData);
        }
    }

    /**
     *
     * palette
     */
    @Bindable
    public String getPalette() {
        if(palette == null) {
            palette = new MutableLiveData<>();
        }
        return palette.getValue();
    }

    public void setPalette(String value) {
        if (palette == null) {
            palette = new MutableLiveData<String>();
        }
        if (!value.equals(palette.getValue())) {
            palette.postValue(value);
            notifyPropertyChanged(BR.palette);
        }
    }

    /**
     *
     * shutter sound
     */
    @Bindable
    public Boolean getShutterSound() {
        if (shutterSound == null) {
            shutterSound = new MutableLiveData<>();
        }
        return shutterSound.getValue();
    }

    public void setShutterSound(Boolean value) {
        if (shutterSound == null) {
            shutterSound = new MutableLiveData<>();
        }
        if(value != shutterSound.getValue()) {
            shutterSound.postValue(value);
            notifyPropertyChanged(BR.shutterSound);
        }
    }

    /**
     *
     * stream delay
     */
    @Bindable
    public Integer getStreamDelay() {
        if(streamDelay == null) {
            streamDelay = new MutableLiveData<>();
        }
        return streamDelay.getValue();
    }

    public void setStreamDelay(Integer value) {
        if (streamDelay == null) {
            streamDelay = new MutableLiveData<>();
        }
        if (value != streamDelay.getValue()) {
            streamDelay.postValue(value);
            notifyPropertyChanged(BR.streamDelay);
        }
    }
}