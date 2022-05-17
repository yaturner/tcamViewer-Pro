package com.darcangel.acam;

import android.app.Application;

import timber.log.Timber;

public class AcamApplication extends Application {
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

