package com.darcangel.tcamViewer.ui.camera;

import android.graphics.Bitmap;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.constants.Constants;
import com.darcangel.tcamViewer.model.ImageDto;
import com.darcangel.tcamViewer.model.Settings;
import com.darcangel.tcamViewer.utils.CameraUtils;

import java.util.Calendar;
import java.util.Locale;

import timber.log.Timber;

public class CameraViewModel extends ViewModel {

    private MutableLiveData<ImageDto> imageDto;
    private CameraService cameraService;
    private CameraUtils cameraUtils;
    private MainActivity mainActivity;
    private Settings settings;
    private float manualMaxTemperature;
    private float manualMinTemperature;
    private boolean unitsCelsius;
    private boolean isStreaming = false;
    private boolean isRemapNeeded = false;
    private boolean isManualRange;




    public CameraViewModel() {
        mainActivity = MainActivity.getInstance();
        cameraService = mainActivity.getCameraService();
        cameraUtils = mainActivity.getCameraUtils();
        settings = mainActivity.getSettings();

        //Listen for changes in ipAddress
        MutableLiveData<String> camera = mainActivity.getSettings().getCameraAddress();
        camera.observe(mainActivity, address -> {
            mainActivity.invalidateOptionsMenu();
        });
        //observe any changes from settings for manual range and/or units
        settings.getManualRange().observeForever(v -> {
            isManualRange = v;
            Timber.d("\\\\ManualRange\\\\observe isManualRange = %s", (isManualRange?"true":"false"));
        });
        settings.getManualRangeMin().observeForever(v -> {
            manualMinTemperature = v; //convertToRadiometric(v);
            Timber.d("\\\\ManualRange\\\\observe ManualRangeMin = %f", v);
        });
        settings.getManualRangeMax().observeForever(v -> {
            manualMaxTemperature = v; //convertToRadiometric(v);
            Timber.d("\\\\ManualRange\\\\observe ManualRangeMax = %f", v);
        });
        settings.getUnitsC().observeForever(v -> {
            unitsCelsius = v;
        });
    }

    public MutableLiveData<ImageDto> getImageDto() {
        if(imageDto == null) {
            imageDto = new MutableLiveData<ImageDto>(null);
        }
        return imageDto;
    }

    public void setImageDto(ImageDto imageDto) {
        if(this.imageDto == null) {
            this.imageDto = new MutableLiveData<ImageDto>(null);
        }
        this.imageDto.setValue(imageDto);
    }

    //Camera operations
    /**
     * connectToCamera
     * this is called when the camera is connected
     */
    public void connectToCamera(MainActivity.JNIListener jniListener) {
        try {
            mainActivity.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    boolean result = mainActivity.getCameraServiceJNI().init(jniListener);
                    Timber.d("CameraServiceJNI returned %s", result?"true":"false");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * disconnectFromCamera
     */
    public void disconnectFromCamera() {
        try {
            mainActivity.getCameraService().disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * setTime
     *
     * set_time argument	Description
     *          sec	        Seconds 0-59
     *          min	        Minutes 0-59
     *          hour	    Hour 0-23
     *          dow	        Day of Week starting with Sunday 1-7
     *          day	        Day of Month 1-28 to 1-31 depending
     *          mon	        Month 1-12
     *          year	    Year offset from 1970
     */
    public void setTime() {
        Calendar now = Calendar.getInstance();

        String args = String.format(Locale.US, Constants.ARGS_SET_TIME,
                now.get(Calendar.SECOND),
                now.get(Calendar.MINUTE),
                now.get(Calendar.HOUR),
                now.get(Calendar.DAY_OF_WEEK),
                now.get(Calendar.DAY_OF_MONTH),
                now.get(Calendar.MONTH) + 1,
                now.get(Calendar.YEAR) - 1970);

        String cmd = String.format(Constants.CMD_SET_TIME, args);
        try {
            mainActivity.getCameraService().sendCmd(cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * setConfig
     */
    public void setConfig() {
        if (cameraService.isConnected()) {

            String args = String.format(Locale.US, Constants.ARGS_SET_CONFIG,
                    Boolean.TRUE.equals(settings.getAGC().getValue()) ? 1 : 0,
                    settings.getEmissivity().getValue(),
                    Boolean.TRUE.equals(settings.getGainHigh().getValue()) ? 0 :
                            Boolean.TRUE.equals(settings.getGainLow().getValue()) ? 1 : 2);
            String cmd = String.format(Constants.CMD_SET_CONFIG, args);
            //isConnectingToCamera = false;
            try {
                mainActivity.getCameraService().sendCmd(cmd);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void getConfig() {
        if(cameraService.isConnected()) {
            String cmd = Constants.CMD_GET_CONFIG;
            try {
                mainActivity.getCameraService().sendCmd(cmd);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void getWifi() {
        if(cameraService.isConnected()) {
            String cmd = Constants.CMD_GET_WIFI;
            try {
                mainActivity.getCameraService().sendCmd(cmd);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * getImage
     */
    public void getImageFromCamera() {
        try {
//            mainActivity.showProgressDialog(mainActivity.getResources().getString(R.string.get_image),
//                    mainActivity.getResources().getString(R.string.acquiring));
            mainActivity.getCameraService().sendCmd(Constants.CMD_GET_IMAGE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startStreaming(Boolean flag) {
        try {
            if(flag) {
                mainActivity.getCameraService().startStreaming();
            } else {
                mainActivity.getCameraService().stopStreaming();
            }
            isStreaming = flag;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Boolean getStreaming() {
        return isStreaming;
    }

    public void setStreaming(Boolean streaming) {
        isStreaming = streaming;
    }

    public boolean isRemapNeeded() {
        return isRemapNeeded;
    }

    public void setRemapNeeded(boolean remapNeeded) {
        isRemapNeeded = remapNeeded;
    }

    public float getManualMaxTemperature() {
        return manualMaxTemperature;
    }

    public void setManualMaxTemperature(float manualMaxTemperature) {
        this.manualMaxTemperature = manualMaxTemperature;
    }

    public float getManualMinTemperature() {
        return manualMinTemperature;
    }

    public void setManualMinTemperature(float manualMinTemperature) {
        this.manualMinTemperature = manualMinTemperature;
    }

    public boolean isUnitsCelsius() {
        return unitsCelsius;
    }

    public void setUnitsCelsius(boolean unitsCelsius) {
        this.unitsCelsius = unitsCelsius;
    }

    public boolean isManualRange() {
        return isManualRange;
    }

    public void setManualRange(boolean manualRange) {
        isManualRange = manualRange;
    }
}