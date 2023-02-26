package com.darcangel.tcamViewer.services;

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

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.BackpressureOverflowStrategy;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.subjects.PublishSubject;
import timber.log.Timber;

public class CameraService extends Service {
    private final IBinder binder = new CameraServiceBinder();
    private Socket cameraSocket;
    private String response;
    private String command;
    private Boolean isStreaming = false;
    private String ipAddress;
    private PublishSubject<JSONObject> imageChannel;
    private final MainActivity mainActivity;
    private JSONObject jsonObject;
    private Thread listenerThread;
    private boolean running = false;
    private int totalBytesRead = 0;
    private int bytes_read = 0;
    private BufferedReader inFromSocket;
    private DataOutputStream outToSocket;
    private char[] readBuffer;
    private boolean startFound, endFound;
    
    public class CameraServiceBinder extends Binder {
        public CameraService getService() {
            return CameraService.this;
        }
    }

    public CameraService() {
        mainActivity = MainActivity.getInstance();
        imageChannel = PublishSubject.create();
        imageChannel.observeOn(AndroidSchedulers.mainThread())
                .toFlowable(BackpressureStrategy.BUFFER).onBackpressureBuffer(256, () -> {},
                        BackpressureOverflowStrategy.DROP_LATEST);

        readBuffer = new char[Constants.BUFFER_LENGTH];
        resetBuffers();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
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
     *
     * TODO add timeout
     */
    public Boolean connect() throws IOException {
        cameraSocket = new Socket(ipAddress, 5001);
        inFromSocket = new BufferedReader(new InputStreamReader(cameraSocket.getInputStream()));
        outToSocket = new DataOutputStream(cameraSocket.getOutputStream());
        if(isConnected()) {
            startListening();
        } else {
            return false;
        }
        return true;
    }

    public void stopListening() {
        running = false;
    }

    /**
     * disconnect
     */
    public void disconnect() {
        stopStreaming();
        stopListening();
        try {
            cameraSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * sendCmd
     *
     * @param cmd
     *
     * TODO handle error
     */
    public void sendCmd(final String cmd) {
        try {
            outToSocket.writeUTF(cmd);
            outToSocket.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * isConnected
     *
     * @return
     */
    public boolean isConnected() {
        if (cameraSocket == null) {
            return false;
        } else {
            return cameraSocket.isConnected();
        }
    }

    /**
     * startStreaming
     */
    public void startStreaming() {
        String args = String.format(Constants.ARGS_SET_STREAM_ON, 0, 0);
        command = String.format(Constants.CMD_SET_STREAM_ON, args);
        sendCmd(command);
    }

    public void stopStreaming() {
        isStreaming = false;
        sendCmd(Constants.CMD_SET_STREAM_OFF);
    }

    private void startListening() {
        running = true;
        totalBytesRead = 0;
        bytes_read = 0;

        listenerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isConnected() && running) {
                    try {
                        bytes_read = inFromSocket.read(readBuffer);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (bytes_read == 0) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                    for (int index = 0; index < bytes_read; index++) {
                        if (readBuffer[index] == '\02') {
                            if (startFound) {
                                //second in a row, we lost the '03', start over
                                response = "";
                            } else {
                                startFound = true;
                            }
                            Timber.d("found start readBuffer[%d] = %c", index, readBuffer[index]);
                        } else if (startFound && !endFound && readBuffer[index] == '\03') {
                            endFound = true;
                            resetBuffers();
                        } else {
                            if (startFound && !endFound) {
                                response += readBuffer[index];
                            }
                            totalBytesRead++;
                        }
                    }
                }
            }
        });
        listenerThread.run();
    }

    void resetBuffers() {
        response = "";
        endFound = false;
        startFound = false;
        totalBytesRead = 0;
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
}

