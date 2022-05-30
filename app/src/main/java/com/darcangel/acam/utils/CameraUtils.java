package com.darcangel.acam.utils;

import android.content.res.Resources;

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.R;
import com.darcangel.acam.constants.Constants;

public class CameraUtils {
    private MainActivity mainActivity;

    public CameraUtils() {
        if(mainActivity == null) {
            mainActivity = MainActivity.getInstance();
        }
    }

    /**
     * getConfig
     */
    public void getConfig(MainActivity.CameraCallback callback) {
        Resources resources = mainActivity.getResources();
            try {
                mainActivity.getCameraService().sendCmd(Constants.CMD_GET_CONFIG, callback);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }


}