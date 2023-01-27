package com.darcangel.tcamViewer.ui.camera;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.constants.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.BackpressureOverflowStrategy;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.subjects.PublishSubject;
import timber.log.Timber;

public class CameraService extends Service {

    private Socket cameraSocket;
    private String response;
    private String command;
    private BufferedReader inFromCamera;
    private DataOutputStream outToCamera;
    private IBinder mBinder = new MyBinder();
    private Boolean isStreaming = false;
    private Boolean isListening = false;
    private String ipAddress;
    private PublishSubject<JSONObject> imageChannel;
    private MainActivity mainActivity;
    private JSONObject jsonObject;

    @Override
    public void onCreate() {
        mainActivity = MainActivity.getInstance();
        imageChannel = PublishSubject.create();
        imageChannel.observeOn(AndroidSchedulers.mainThread())
                .toFlowable(BackpressureStrategy.BUFFER).onBackpressureBuffer(256, () -> {
                }, BackpressureOverflowStrategy.DROP_LATEST);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Timber.v("in onRebind");
        super.onRebind(intent);
    }
    @Override
    public boolean onUnbind(Intent intent) {
        Timber.v("in onUnbind");
        return true;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.v("in onDestroy");
        stopStreaming();
        stopListening();
    }

    /***************User APi methods***************/
    /**
     * Must be called before any other methods
     *
     * @param address
     */
    public void setIpAddress(final String address) {
        if (isConnected()) {
            disconnect();
        }
        ipAddress = address;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * connect
     */
    public Boolean connect() {
        try {
            cameraSocket = new Socket(ipAddress, 5001);
            if (cameraSocket == null) {
                return false;
            }
            if (cameraSocket.isConnected()) {
                startListening();
            } else {
                return false;
            }

            inFromCamera = new BufferedReader(new InputStreamReader(cameraSocket.getInputStream()));
            outToCamera = new DataOutputStream(cameraSocket.getOutputStream());

            return true;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean startListening() {
        isListening = true;
        return true;
    }

    public void stopListening() {
        isListening = false;
    }

    /**
     * disconnect
     */
    public void disconnect() {
        stopStreaming();
        stopListening();
        disconnect();
    }

    /**
     * sendCommand
     *
     * @param cmd
     */
    public boolean sendCommand(final String cmd) {
        try {
            outToCamera.writeChars(cmd);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * isConnected
     *
     * @return
     */
    public boolean isConnected() {
        return cameraSocket.isConnected();
    }

    /**
     * startStreaming
     */
    public void startStreaming() {
        String args = String.format(Constants.ARGS_SET_STREAM_ON, 0, 0);
        command = String.format(Constants.CMD_SET_STREAM_ON, args);
        sendCommand(command);
    }

    public void stopStreaming() {
        isStreaming = false;
        sendCommand(Constants.CMD_SET_STREAM_OFF);
    }

    /**
     * parseResponse
     *
     * @param response
     * @return
     */
    private JSONObject parseResponse(String response) {
        try {
            if (response != null) {
                return new JSONObject(response);
            }
        } catch (JSONException e) {
            handleError(e);
            return new JSONObject();
        }
        return new JSONObject();
    }

    private void handleError(Exception e) {
        e.printStackTrace();
        mainActivity.getExecutor().shutdown();
//        try {
//            imageChannel.onNext(new JSONObject(String.format(jsonString, e.toString())));
//        } catch (JSONException ex) {
//            ex.printStackTrace();
//        }
    }

    public PublishSubject<JSONObject> getImageChannel() {
        return imageChannel;
    }

    public class MyBinder extends Binder {
        public CameraService getService() {
            return CameraService.this;
        }
    }
}

