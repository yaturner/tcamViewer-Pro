package com.darcangel.acam;

import android.app.Application;

import com.darcangel.acam.constants.Constants;

import java.net.Socket;
import java.net.URISyntaxException;

import dagger.hilt.android.HiltAndroidApp;
import timber.log.Timber;

@HiltAndroidApp
public class AcamApplication extends Application {
    //private DaggerController mDaggerController;
    private Socket cameraSocket;
    {
        try {
            cameraSocket = new Socket("http://192.168.0.42", 5001);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    public void onCreate() {
        super.onCreate();
        //mDaggerController = new DaggerController(this);
        initTimber();
    }

    private void initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    public Socket getCameraSocket() {
        return cameraSocket;
    }
}