package com.darcangel.tcamViewer.ui.camera;

import android.os.Parcel;
import android.os.Parcelable;

import com.darcangel.tcamViewer.JNI.CameraServiceJNI;
import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.constants.Constants;
import com.google.gson.internal.bind.TreeTypeAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.util.concurrent.Future;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.BackpressureOverflowStrategy;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class CameraService implements Parcelable {

    ////private Socket cameraSocket;
    private String response;
    private String command;
    private BufferedInputStream bufferedInputStream;
    private Boolean isStreaming = false;
    private TreeTypeAdapter streamThread;
    private String ipAddress;
    private PublishSubject<JSONObject> imageChannel;
    private MainActivity mainActivity;
    private CameraServiceJNI jni;
    private MainActivity.JNIListener jniListener;
    private Runnable cameraTask;
    private Future<?> jniTask;

    public CameraService() {
        mainActivity = MainActivity.getInstance();
        jni = mainActivity.getCameraServiceJNI();
        imageChannel = PublishSubject.create();
        imageChannel.observeOn(AndroidSchedulers.mainThread())
                .toFlowable(BackpressureStrategy.BUFFER).onBackpressureBuffer(256, () -> {}, BackpressureOverflowStrategy.DROP_OLDEST);
        jniListener = new MainActivity.JNIListener() {
            @Override
            public void onAcceptResponse(String response) {
                JSONObject obj = parseResponse(response);
                response = null;
                imageChannel.onNext(obj);
            }
        };

    }

    public CameraService(Parcel in) {
        mainActivity = MainActivity.getInstance();
        jni = mainActivity.getCameraServiceJNI();
        jniListener = new MainActivity.JNIListener() {
            @Override
            public void onAcceptResponse(String response) {
                imageChannel.onNext(parseResponse(response));
            }
        };
    }

    public static final Creator<CameraService> CREATOR = new Creator<CameraService>() {
        @Override
        public CameraService createFromParcel(Parcel in) {
            return new CameraService(in);
        }

        @Override
        public CameraService[] newArray(int size) {
            return new CameraService[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(ipAddress);
        dest.writeInt(isStreaming ? 1 : 0);
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
        if(!jni.connect(jniListener, ipAddress)) {
            return false;
        }
        if(jni.isConnected()) {
            jni.startListening();
        } else {
            return false;
        }
        return true;
    }

    public void stopListening() {
        jni.stopListening();
    }

    /**
     * disconnect
     */
    public void disconnect() {
        stopStreaming();
        stopListening();
        jni.disconnect();
    }

    /**
     * sendCmd
     *
     * @param cmd
     */
    public void sendCmd(final String cmd) {
        jni.sendCommand(cmd);
    }

    /**
     * isConnected
     *
     * @return
     */
    public boolean isConnected() {
        return jni.isConnected();
    }

    /**
     * startStreaming
     */
    public void startStreaming() {
        String args = String.format(Constants.ARGS_SET_STREAM_ON, 0, 0);
        command = String.format(Constants.CMD_SET_STREAM_ON, args);
        mainActivity.getCameraServiceJNI().sendCommand(command);
    }

    public void stopStreaming() {
        isStreaming = false;
        sendCmd(Constants.CMD_SET_STREAM_OFF);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * parseResponse
     *
     * @param response
     * @return
     */
    JSONObject parseResponse(String response) {
        try {
            if (response != null) {
//                Timber.d("parseResponse starts with %s and ends with %s",
//                        response.substring(0, 1), response.substring(response.length()-1));
                //strip out start/stop bytes
                //response = response.substring(1, response.length() - 1);
                return new JSONObject(response);
            }
        } catch (JSONException e) {
            handleError(e);
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
}

