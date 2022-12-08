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

import io.reactivex.rxjava3.subjects.PublishSubject;
import timber.log.Timber;

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


    public CameraService() {
        mainActivity = MainActivity.getInstance();
        jni = mainActivity.getCameraServiceJNI();
        imageChannel = PublishSubject.create();
        jniListener = new MainActivity.JNIListener() {
            @Override
            public void onAcceptResponse(String response) {
                imageChannel.onNext(parseResponse(response));
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
    public void connect() {
        jni.connect(jniListener);
        if(jni.isConnected()) {
            mainActivity.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    jni.startListening();
                }
            });
        }
    }

    /**
     * disconnect
     */
    public void disconnect() {
        //TODO jni.disconnect();
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
        String args = String.format(Constants.ARGS_SET_STREAM_ON, 250, 0);
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
                Timber.d("parseResponse('%s')", response);
                //strip out start/stop bytes
                response = response.substring(1, response.length() - 1);
                return new JSONObject(response);
            }
        } catch (JSONException e) {
            handleError(e);
        }
        return new JSONObject();
    }

    /**
     * onDestroy
     */
    public void onDestroy() {
    }

    private void handleError(Exception e) {
        e.printStackTrace();
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

