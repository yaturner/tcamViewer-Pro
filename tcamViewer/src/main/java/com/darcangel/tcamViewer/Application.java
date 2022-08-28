package com.darcangel.tcamViewer;

import java.io.IOException;

import dagger.hilt.android.HiltAndroidApp;
import io.reactivex.rxjava3.exceptions.UndeliverableException;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import timber.log.Timber;

@HiltAndroidApp
public class Application extends android.app.Application {
    public void onCreate() {
        super.onCreate();
        initTimber();

        RxJavaPlugins.setErrorHandler(e -> {
            if (e instanceof UndeliverableException) {
                e = e.getCause();
            }
            if (e instanceof IOException) {
                // fine, irrelevant network problem or API that throws on cancellation
                Timber.e(e);
                return;
            }
            if (e instanceof InterruptedException) {
                // fine, some blocking code was interrupted by a dispose call
                Timber.e(e);
                return;
            }
            if ((e instanceof NullPointerException) || (e instanceof IllegalArgumentException)) {
                // that's likely a bug in the application
                Thread.currentThread().getUncaughtExceptionHandler().
                        uncaughtException(Thread.currentThread(), e);
                Timber.e(e);
                return;
            }
            if (e instanceof IllegalStateException) {
                // that's a bug in RxJava or in a custom operator
                Thread.currentThread().getUncaughtExceptionHandler()
                        .uncaughtException(Thread.currentThread(), e);
                Timber.e(e);
                return;
            }
            Timber.d(e, "Undeliverable exception received, not sure what to do");
        });
    }

    private void initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }
}