package com.darcangel.tcamViewer.container;

import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.TextView;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.BindingAdapter;
import androidx.databinding.InverseBindingAdapter;
import androidx.lifecycle.MutableLiveData;

import com.darcangel.tcamViewer.BR;
import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.constants.Constants;

public class Settings extends BaseObservable implements Parcelable {
    private final SharedPreferences sharedPreferences = MainActivity.getInstance().getSharedPreferences();

    //Settings Fragment
    private MutableLiveData<Boolean> AGC;
    private MutableLiveData<Integer> emissivity;
    private MutableLiveData<Boolean> gainAuto;
    private MutableLiveData<Boolean> gainHigh;
    private MutableLiveData<Boolean> gainLow;
    private MutableLiveData<String> cameraAddress;                //IP address of camera
    private MutableLiveData<Boolean> exportOnSave;
    private MutableLiveData<Boolean> exportMetaData;
    private MutableLiveData<Integer> exportResolution;  // HxW for exporting image
    private MutableLiveData<Boolean> autoRange;         //if the Manual Range btn is clicked this false
    private MutableLiveData<Integer> manualRangeMin;
    private MutableLiveData<Integer> manualRangeMax;
    private MutableLiveData<String> palette;
    private MutableLiveData<Boolean> shutterSound;
    private MutableLiveData<Boolean> displaySpotmeter;
    private MutableLiveData<Boolean> unitsF;
    private MutableLiveData<Boolean> unitsC;

    //WiFi Settings
    private MutableLiveData<Boolean> accessPoint;
    private MutableLiveData<String> SSID;
    private MutableLiveData<String> password;
    private MutableLiveData<Boolean> staticIP;
    private MutableLiveData<String> staticIPAddress;
    private MutableLiveData<String> staticNetmask;


    private MutableLiveData<Integer> gain;
    private MutableLiveData<Float> streamRate;
    private MutableLiveData<Boolean> updateCameraClock;       // update camera clock when connected
    private MutableLiveData<Boolean> scaleDisplay;
    private MutableLiveData<String> downloadFolder;
    private MutableLiveData<Integer> streamDelay;

    public Settings() {
        init();
    }

    protected Settings(Parcel in) {
        setAGC(in.readInt()==1);
        setEmissivity(in.readInt());
        setGainAuto(in.readInt()==1);
        setGainHigh(in.readInt()==1);
        setGainLow(in.readInt()==1);
        setCameraAddress(in.readString());
        setExportOnSave(in.readInt()==1);
        setExportMetaData(in.readInt()==1);
        setExportResolution(in.readInt());
        setAutoRange(in.readInt()==1);
        setManualRangeMax(in.readInt());
        setManualRangeMin(in.readInt());
        setPalette(in.readString());
        setShutterSound(in.readInt()==1);
        setDisplaySpotmeter(in.readInt()==1);
        setUnitsF(in.readInt()==1);
        setUnitsC(in.readInt()==1);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(getAGC()?1:0);
        dest.writeInt(getEmissivity());
        dest.writeInt(getGainAuto()?1:0);
        dest.writeInt(getGainHigh()?1:0);
        dest.writeInt(getGainLow()?1:0);
        dest.writeString(getCameraAddress());
        dest.writeInt(getExportOnSave()?1:0);
        dest.writeInt(getExportMetaData()?1:0);
        dest.writeInt(getExportResolution());
        dest.writeInt(getAutoRange()?1:0);
        dest.writeInt(getManualRangeMax());
        dest.writeInt(getManualRangeMin());
        dest.writeString(getPalette());
        dest.writeInt(getShutterSound()?1:0);
        dest.writeInt(getDisplaySpotmeter()?1:0);
        dest.writeInt(getUnitsF()?1:0);
        dest.writeInt(getUnitsC()?1:0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Settings> CREATOR = new Creator<Settings>() {
        @Override
        public Settings createFromParcel(Parcel in) {
            return new Settings(in);
        }

        @Override
        public Settings[] newArray(int size) {
            return new Settings[size];
        }
    };

    private void init() {
        setAGC(sharedPreferences.getBoolean(Constants.KEY_AGC, false));
        setEmissivity(sharedPreferences.getInt(Constants.KEY_EMISSIVITY, 45));
        setGainAuto(sharedPreferences.getBoolean(Constants.KEY_GAIN_AUTO, false));
        setGainHigh(sharedPreferences.getBoolean(Constants.KEY_GAIN_HIGH, false));
        setGainLow(sharedPreferences.getBoolean(Constants.KEY_GAIN_LOW, true));
        setCameraAddress(sharedPreferences.getString(Constants.KEY_CAMERA_IP_ADDRESS, "10.0.1.74"));
        setExportOnSave(sharedPreferences.getBoolean(Constants.KEY_EXPORT_PICTURE_ON_SAVE, false));
        setExportMetaData((sharedPreferences.getBoolean(Constants.KEY_EXPORT_METADATA, true)));
        setExportResolution(sharedPreferences.getInt(Constants.KEY_EXPORT_RESOLUTION, 1));
        setAutoRange(sharedPreferences.getBoolean(Constants.KEY_AUTORANGE, false));
        setManualRangeMax(sharedPreferences.getInt(Constants.KEY_MANUAL_RANGE_MAX, 100));
        setManualRangeMin(sharedPreferences.getInt(Constants.KEY_MANUAL_RANGE_MIN, 0));
        setPalette(sharedPreferences.getString(Constants.KEY_PALETTE, "Fusion"));
        setShutterSound(sharedPreferences.getBoolean(Constants.KEY_SHUTTER_SOUND, true));
        setDisplaySpotmeter(sharedPreferences.getBoolean(Constants.KEY_SPOTMETER, true));
        setUnitsF(sharedPreferences.getBoolean(Constants.KEY_UNITS_F, false));
        setUnitsC(sharedPreferences.getBoolean(Constants.KEY_UNITS_C, true));

        //Wifi settings are always pulled from the camera
//        setAccessPoint(sharedPreferences.getBoolean(Constants.KEY_WIFI_ACCESSPOINT, false));
//        setSSID(sharedPreferences.getString(Constants.KEY_WIFI_SSID, ""));
//        setPassword(sharedPreferences.getString(Constants.KEY_WIFI_PASSWORD, ""));
//        setStaticIP(sharedPreferences.getBoolean(Constants.KEY_WIFI_STATICIP,false));
//        setStaticIPAddress(sharedPreferences.getString(Constants.KEY_WIFI_STATICIPADDRESS, ""));
//        setStaticNetmask(sharedPreferences.getString(Constants.KEY_WIFI_STATICNETMASK, ""));
    }

    public void persist() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(Constants.KEY_AGC, getAGC());
        editor.putInt(Constants.KEY_EMISSIVITY, getEmissivity());
        editor.putBoolean(Constants.KEY_GAIN_AUTO,getGainAuto());
        editor.putBoolean(Constants.KEY_GAIN_HIGH,getGainHigh());
        editor.putBoolean(Constants.KEY_GAIN_LOW,getGainLow());
        editor.putString(Constants.KEY_CAMERA_IP_ADDRESS, getCameraAddress());
        editor.putBoolean(Constants.KEY_EXPORT_PICTURE_ON_SAVE, getExportOnSave());
        editor.putBoolean(Constants.KEY_EXPORT_METADATA, getExportMetaData());
        editor.putInt(Constants.KEY_EXPORT_RESOLUTION, getExportResolution());
        editor.putBoolean(Constants.KEY_AUTORANGE, getAutoRange());
        editor.putInt(Constants.KEY_MANUAL_RANGE_MAX, getManualRangeMax());
        editor.putInt(Constants.KEY_MANUAL_RANGE_MIN, getManualRangeMin());
        editor.putString(Constants.KEY_PALETTE, getPalette());
        editor.putBoolean(Constants.KEY_SHUTTER_SOUND, getShutterSound());
        editor.putBoolean(Constants.KEY_SPOTMETER, getDisplaySpotmeter());
        editor.putBoolean(Constants.KEY_UNITS_F, getUnitsF());
        editor.putBoolean(Constants.KEY_UNITS_C, getUnitsC());
        editor.apply();
    }


    @BindingAdapter("android:text")
    public static void setText(TextView view, Integer value) {
        try {
            if (view.getText() != null && value != null) {
                //If the editText is empty, just set the value
                if (view.getText().toString().isEmpty()) {
                    view.setText(Integer.toString(value));
                    //See if the value changed to prevent infinite loop
                } else if (Integer.parseInt(view.getText().toString()) != value) {
                    view.setText(Integer.toString(value));
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    @InverseBindingAdapter(attribute = "android:text")
    public static int getText(TextView view) {
        try {
            return Integer.parseInt(view.getText().toString());
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 0;
        }
    }

    //Getters and Setters

    /**
     * AGC
     */
    @Bindable
    public Boolean getAGC() {
        if (AGC == null) {
            AGC = new MutableLiveData<>();
        }
        return AGC.getValue();
    }

    public void setAGC(Boolean value) {
        if (AGC == null) {
            AGC = new MutableLiveData<>();
        }
        if (value != AGC.getValue()) {
            AGC.setValue(value);
            notifyPropertyChanged(BR.aGC);
        }
    }

    /**
     * Gain Auto
     */
    @Bindable
    public Boolean getGainAuto() {
        if (gainAuto == null) {
            gainAuto = new MutableLiveData<Boolean>();
        }
        return gainAuto.getValue();
    }

    public void setGainAuto(Boolean value) {
        if (gainAuto == null) {
            gainAuto = new MutableLiveData<Boolean>();
        }
        if (value != gainAuto.getValue()) {
            gainAuto.setValue(value);
            notifyPropertyChanged(BR.gainAuto);
        }
    }

    /**
     * Gain High
     */
    @Bindable
    public Boolean getGainHigh() {
        if (gainHigh == null) {
            gainHigh = new MutableLiveData<Boolean>();
        }
        return gainHigh.getValue();
    }

    public void setGainHigh(Boolean value) {
        if (gainHigh == null) {
            gainHigh = new MutableLiveData<Boolean>();
        }
        if (value != gainHigh.getValue()) {
            gainHigh.setValue(value);
            notifyPropertyChanged(BR.gainHigh);
        }
    }

    /**
     * Gain Low
     */
    @Bindable
    public Boolean getGainLow() {
        if (gainLow == null) {
            gainLow = new MutableLiveData<Boolean>();
        }
        return gainLow.getValue();
    }

    public void setGainLow(Boolean value) {
        if (gainLow == null) {
            gainLow = new MutableLiveData<Boolean>();
        }
        if (value != gainLow.getValue()) {
            gainLow.setValue(value);
            notifyPropertyChanged(BR.gainLow);
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
            cameraAddress.setValue(address);
            notifyPropertyChanged(BR.cameraAddress);
        }
    }

    public MutableLiveData<String> getLiveDataCameraAddress() {
        if (cameraAddress == null) {
            cameraAddress = new MutableLiveData<>();
        }
        return cameraAddress;
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
            emissivity.setValue(value);
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
            exportOnSave.setValue(value);
            notifyPropertyChanged(BR.exportOnSave);
        }
    }

    /**
     * Auto Range, if manual range is selected, this is false
     */
    @Bindable
    public Boolean getAutoRange() {
        if (autoRange == null) {
            autoRange = new MutableLiveData<>();
        }
        return autoRange.getValue();
    }

    public void setAutoRange(Boolean value) {
        if (autoRange == null) {
            autoRange = new MutableLiveData<>();
        }
        if (value != autoRange.getValue()) {
            autoRange.setValue(value);
            notifyPropertyChanged(BR.autoRange);
        }
    }

    /**
     * manual range min, max
     */
    @Bindable
    public Integer getManualRangeMin() {
        if (manualRangeMin == null) {
            manualRangeMin = new MutableLiveData<>();
        }
        return manualRangeMin.getValue();
    }

    public void setManualRangeMin(Integer value) {
        if (manualRangeMin == null) {
            manualRangeMin = new MutableLiveData<>();
        }
        if (value != manualRangeMin.getValue()) {
            manualRangeMin.setValue(value);
            notifyPropertyChanged(BR.manualRangeMin);
        }
    }

    @Bindable
    public Integer getManualRangeMax() {
        if (manualRangeMax == null) {
            manualRangeMax = new MutableLiveData<>();
        }
        return manualRangeMax.getValue();
    }

    public void setManualRangeMax(Integer value) {
        if (manualRangeMax == null) {
            manualRangeMax = new MutableLiveData<>();
        }
        if (value != manualRangeMax.getValue()) {
            manualRangeMax.setValue(value);
            notifyPropertyChanged(BR.manualRangeMax);
        }
    }

    /**
     * display units in Fahrenheit
     */
    @Bindable
    public Boolean getUnitsF() {
        if (unitsF == null) {
            unitsF = new MutableLiveData<>();
        }
        return unitsF.getValue();
    }

    public void setUnitsF(Boolean value) {
        if (unitsF == null) {
            unitsF = new MutableLiveData<>();
        }
        if (value != unitsF.getValue()) {
            unitsF.setValue(value);
            notifyPropertyChanged(BR.unitsF);
        }
    }

    /**
     * display units in Celsius
     */
    @Bindable
    public Boolean getUnitsC() {
        if (unitsC == null) {
            unitsC = new MutableLiveData<>();
        }
        return unitsC.getValue();
    }

    public void setUnitsC(Boolean value) {
        if (unitsC == null) {
            unitsC = new MutableLiveData<>();
        }
        if (value != unitsC.getValue()) {
            unitsC.setValue(value);
            notifyPropertyChanged(BR.unitsC);
        }
    }

    /**
     * stream rate
     */
    @Bindable
    public Float getStreamRate() {
        if (streamRate == null) {
            streamRate = new MutableLiveData<>();
        }
        return streamRate.getValue();
    }

    public void setStreamRate(Float value) {
        if (streamRate == null) {
            streamRate = new MutableLiveData<>();
        }
        if (value != streamRate.getValue()) {
            streamRate.setValue(value);
            notifyPropertyChanged(BR.streamRate);
        }
    }

    /**
     * update camera clock
     */
    @Bindable
    public Boolean getUpdateCameraClock() {
        if (updateCameraClock == null) {
            updateCameraClock = new MutableLiveData<>();
        }
        return updateCameraClock.getValue();
    }

    public void setUpdateCameraClock(Boolean value) {
        if (updateCameraClock == null) {
            updateCameraClock = new MutableLiveData<>();
        }
        if (value != updateCameraClock.getValue()) {
            updateCameraClock.setValue(value);
            notifyPropertyChanged(BR.updateCameraClock);
        }
    }

    /**
     * scale display
     */
    @Bindable
    public Boolean getScaleDisplay() {
        if (scaleDisplay == null) {
            scaleDisplay = new MutableLiveData<>();
        }
        return scaleDisplay.getValue();
    }

    public void setScaleDisplay(Boolean value) {
        if (scaleDisplay == null) {
            scaleDisplay = new MutableLiveData<>();
        }
        if (value != scaleDisplay.getValue()) {
            scaleDisplay.setValue(value);
            notifyPropertyChanged(BR.scaleDisplay);
        }
    }

    /**
     * export resolution
     */
    @Bindable
    public Integer getExportResolution() {
        if (exportResolution == null) {
            exportResolution = new MutableLiveData<>();
        }
        return exportResolution.getValue();
    }

    public void setExportResolution(Integer value) {
        if (exportResolution == null) {
            exportResolution = new MutableLiveData<>();
        }
        if (value != exportResolution.getValue()) {
            exportResolution.setValue(value);
            notifyPropertyChanged(BR.exportOnSave);
        }
    }

    /**
     * download folder
     */
    @Bindable
    public String getDownloadFolder() {
        if (downloadFolder == null) {
            downloadFolder = new MutableLiveData<>();
        }
        return downloadFolder.getValue();
    }

    public void setDownloadFolder(String value) {
        if (downloadFolder == null) {
            downloadFolder = new MutableLiveData<>();
        }
        if (!value.equals(downloadFolder.getValue())) {
            downloadFolder.setValue(value);
            notifyPropertyChanged(BR.downloadFolder);
        }
    }

    /**
     * display spot meter
     */
    @Bindable
    public Boolean getDisplaySpotmeter() {
        if (displaySpotmeter == null) {
            displaySpotmeter = new MutableLiveData<>();
        }
        return displaySpotmeter.getValue();
    }

    public void setDisplaySpotmeter(Boolean value) {
        if (displaySpotmeter == null) {
            displaySpotmeter = new MutableLiveData<>();
        }
        if (value != displaySpotmeter.getValue()) {
            displaySpotmeter.setValue(value);
            notifyPropertyChanged(BR.displaySpotmeter);
        }
    }

    /**
     * export metadata
     */
    @Bindable
    public Boolean getExportMetaData() {
        if (exportMetaData == null) {
            exportMetaData = new MutableLiveData<>();
        }
        return exportMetaData.getValue();
    }

    public void setExportMetaData(Boolean value) {
        if (exportMetaData == null) {
            exportMetaData = new MutableLiveData<>();
        }
        if (value != exportMetaData.getValue()) {
            exportMetaData.setValue(value);
            notifyPropertyChanged(BR.exportMetaData);
        }
    }

    /**
     * palette
     */
    @Bindable
    public String getPalette() {
        if (palette == null) {
            palette = new MutableLiveData<>();
        }
        return palette.getValue();
    }

    public MutableLiveData<String> getLiveDataPalette() {
        if (palette == null) {
            palette = new MutableLiveData<>();
        }
        return palette;
    }

    public void setPalette(String value) {
        if (palette == null) {
            palette = new MutableLiveData<String>();
        }
        if (!value.equals(palette.getValue())) {
            palette.setValue(value);
            notifyPropertyChanged(BR.palette);
        }
    }

    /**
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
        if (value != shutterSound.getValue()) {
            shutterSound.setValue(value);
            notifyPropertyChanged(BR.shutterSound);
        }
    }

    /**
     * stream delay
     */
    @Bindable
    public Integer getStreamDelay() {
        if (streamDelay == null) {
            streamDelay = new MutableLiveData<>();
        }
        return streamDelay.getValue();
    }

    public void setStreamDelay(Integer value) {
        if (streamDelay == null) {
            streamDelay = new MutableLiveData<>();
        }
        if (value != streamDelay.getValue()) {
            streamDelay.setValue(value);
            notifyPropertyChanged(BR.streamDelay);
        }
    }

    /**
     * WiFi Settings
     */

    @Bindable
    public Boolean getAccessPoint() {
        if (accessPoint == null) {
            accessPoint = new MutableLiveData<>(false);
        }
        return accessPoint.getValue();
    }

    public void setAccessPoint(Boolean value) {
        if (accessPoint == null) {
            accessPoint = new MutableLiveData<>(false);
        }
        if (accessPoint.getValue() != value) {
            accessPoint.setValue(value);
            notifyPropertyChanged(BR.accessPoint);
        }
    }

    @Bindable
    public String getSSID() {
        if (SSID == null) {
            SSID = new MutableLiveData<>("");
        }
        return SSID.getValue();
    }

    public void setSSID(String value) {
        if (SSID == null) {
            SSID = new MutableLiveData<>("");
        }
        if (!SSID.getValue().equals(value)) {
            SSID.setValue(value);
            notifyPropertyChanged(BR.sSID);
        }
    }

    /**
     * password
     * the password is write only, it can be set from the fragment but is never persisted or read
     * password must be >=8 && <=32
     *
     * @return
     */
    @Bindable
    public String getPassword() {
        if (password == null) {
            password = new MutableLiveData<>("");
        }
        return password.getValue();
    }

    public void setPassword(String value) {
        if (password == null) {
            password = new MutableLiveData<>("");
        }
        if (!password.getValue().equals(value)) {
            password.setValue(value);
            notifyPropertyChanged(BR.password);
        }
    }

    @Bindable
    public Boolean getStaticIP() {
        if (staticIP == null) {
            staticIP = new MutableLiveData<>(false);
        }
        return staticIP.getValue();
    }

    public void setStaticIP(Boolean value) {
        if (staticIP == null) {
            accessPoint = new MutableLiveData<>(false);
        }
        if (staticIP.getValue() != value) {
            staticIP.setValue(value);
            notifyPropertyChanged(BR.staticIP);
        }
    }

    @Bindable
    public String getStaticIPAddress() {
        if (staticIPAddress == null) {
            staticIPAddress = new MutableLiveData<>("");
        }
        return staticIPAddress.getValue();
    }

    public void setStaticIPAddress(String value) {
        if (staticIPAddress == null) {
            staticIPAddress = new MutableLiveData<>("");
        }
        if (!staticIPAddress.getValue().equals(value)) {
            staticIPAddress.setValue(value);
            notifyPropertyChanged(BR.staticIPAddress);
        }
    }

    @Bindable
    public String getStaticNetmask() {
        if (staticNetmask == null) {
            staticNetmask = new MutableLiveData<>("");
        }
        return staticNetmask.getValue();
    }

    public void setStaticNetmask(String value) {
        if (staticNetmask == null) {
            staticNetmask = new MutableLiveData<>("");
        }
        if (!staticNetmask.getValue().equals(value)) {
            staticNetmask.setValue(value);
            notifyPropertyChanged(BR.staticNetmask);
        }
    }
}