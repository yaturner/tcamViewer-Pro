package com.darcangel.acam;

import com.darcangel.acam.constants.Constants;

import java.net.Socket;
import java.net.URISyntaxException;

import dagger.hilt.android.HiltAndroidApp;
import timber.log.Timber;

@HiltAndroidApp
public class Application extends android.app.Application {
    //private DaggerController mDaggerController;

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
}