package com.darcangel.acam.ui.camera;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.constants.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.internal.observers.BlockingObserver;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class CameraService {

    private Socket cameraSocket;
    private PrintStream printStream;
    private byte[] buffer = new byte[4096];
    private String response;
    private String command;
    private BufferedInputStream bufferedInputStream;
    private String ipAddress;
    private final PublishSubject<String> imageChannel = PublishSubject.create();

    public CameraService() {
        cameraSocket = new Socket();
        //Listen for changes in ipAddress
        MutableLiveData<String> camera = MainActivity.getInstance().getSettings().getLiveDataCameraAddress();
        camera.observe(MainActivity.getInstance(), s -> setIpAddress(s));
        //For streaming images from the camera
    }

    class StreamImages implements Runnable {
        @Override
        public void run() {

        }
    }

    /***************User APi methods***************/
    /**
     * Must be called before any other methods
     *
     * @param address
     */
    public void setIpAddress(final String address) {

        if (isConnected()) {
            disconnect(null);
            connect(null);
        }
        ipAddress = address;
    }

    /**
     * connect
     *
     * @param callback
     */
    public void connect(MainActivity.CameraCallback callback) {
        Runnable connect = new ConnectSocket(callback);
        new Thread(connect).start();
    }

    /**
     * disconnect
     *
     * @param callback
     */
    public void disconnect(MainActivity.CameraCallback callback) {
        Runnable disconnect = new DisconnectSocket(callback);
        new Thread(disconnect).start();
    }

    /**
     * sendCmd
     *
     * @param cmd
     * @param callback
     * @throws IOException
     */
    public void sendCmd(final String cmd, MainActivity.CameraCallback callback) throws IOException {
        command = cmd;
        Runnable sendCmd = new SendCmd(callback);
        new Thread(sendCmd).start();
    }

    /**
     * isConnected
     *
     * @return
     */
    public boolean isConnected() {
        if (cameraSocket != null) {
            return cameraSocket.isConnected();
        } else {
            return false;
        }
    }

    /**
     * connectSocket
     * Thread to connect to the camera
     */
    class ConnectSocket implements Runnable {

        MainActivity.CameraCallback callback;

        public ConnectSocket(MainActivity.CameraCallback callback) {
            this.callback = callback;
        }

        @Override
        public void run() {
            SocketAddress socketAddress = new InetSocketAddress(ipAddress, 5001);
            try {
                cameraSocket.connect(socketAddress);
            } catch (IOException e) {
                e.printStackTrace();
                callback.callback(parseResponse(Constants.ERROR, null));
                return;
            }

            if (callback != null) {
                callback.callback(parseResponse(Constants.SUCCESS, null));
            }
        }
    }

    /**
     * DisconnectSocket
     * Thread to disconnect socket
     */
    class DisconnectSocket implements Runnable {

        MainActivity.CameraCallback callback;

        public DisconnectSocket(MainActivity.CameraCallback callback) {
            this.callback = callback;
        }

        @Override
        public void run() {
            try {
                cameraSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                callback.callback(parseResponse(Constants.ERROR, null));
                return;
            }
            if (callback != null) {
                callback.callback(parseResponse(Constants.SUCCESS, null));
            }
        }
    }

    /**
     * SendCmd
     * Thread to send a command and receive the response
     */
    class SendCmd implements Runnable {
        MainActivity.CameraCallback callback;

        public SendCmd(MainActivity.CameraCallback callback) {
            this.callback = callback;
        }

        @Override
        public void run() {
            boolean eof = false;
            int bytesRead = 0;
            response = "";

            try {
                bufferedInputStream = new BufferedInputStream(cameraSocket.getInputStream());
                cameraSocket.getOutputStream().write(command.getBytes(StandardCharsets.UTF_8));
                while (!eof) {
                    bytesRead = bufferedInputStream.read(buffer, 0, buffer.length);
                    response = response += new String(buffer, 0, bytesRead);
                    if (response.substring(response.length() - 1).equals("\3")) {
                        eof = true;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                imageChannel.onError(e);
                callback.callback(parseResponse(Constants.ERROR, null));
            }
            imageChannel.onNext(response);
            callback.callback(parseResponse(Constants.SUCCESS, response));
        }
    }

    /**
     * parseResponse
     *
     * @param resultCode
     * @param response
     * @return
     */
    JSONObject parseResponse(final String resultCode, String response) {
        try {
            JSONObject object = new JSONObject(resultCode);
            if (response != null) {
                //strip out start/stop bytes
                response = response.substring(1, response.length() - 1);
                JSONObject responseObj = new JSONObject(response);
                object.put("response", responseObj);
            }
            return object;
        } catch (JSONException e) {
            e.printStackTrace();
            return null; //TODO fix this
        }
    }

    /**
     * onDestroy
     */
    public void onDestroy() {
        try {
            cameraSocket.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        cameraSocket = null;
    }

    public PublishSubject<String> getImageChannel() {
        return imageChannel;
    }
}