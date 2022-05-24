package com.darcangel.acam.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

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
import java.security.Provider;

import timber.log.Timber;

public class CameraService extends Service {

    private Socket cameraSocket;
    private PrintStream printStream;
    private byte[] buffer = new byte[4096];
    private String response;
    private String command;
    private final IBinder cameraBinder = new LocalBinder();
    private BufferedInputStream bufferedInputStream;

    @Override
    public void onCreate() {
        super.onCreate();
        cameraSocket = new Socket();
    }

    /**
     *
     * @param arg0
     * @return the binder
     */
    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return cameraBinder;
    }

    /**
     * Binder class
     */
    public class LocalBinder extends Binder {
        public CameraService getService() {
            return CameraService.this;
        }
    }

    /***************User APi methods***************/
    public void connect(MainActivity.CameraCallback callback) {
        Runnable connect = new ConnectSocket(callback);
        new Thread(connect).start();
    }

    public void disconnect(MainActivity.CameraCallback callback) {
        Runnable disconnect = new DisconnectSocket(callback);
        new Thread(disconnect).start();
    }

    public void sendCmd(final String cmd, MainActivity.CameraCallback callback) throws IOException {
        command = cmd;
        Runnable sendCmd = new SendCmd(callback);
        new Thread(sendCmd).start();
    }

    public boolean isConnected() {
        return cameraSocket.isConnected();
    }

    /**
     * Thread to connect to the camera
     */
    class ConnectSocket implements Runnable {

        MainActivity.CameraCallback callback;

        public ConnectSocket(MainActivity.CameraCallback callback) {
            this.callback = callback;
        }

        @Override
        public void run() {
            SocketAddress socketAddress = new InetSocketAddress("192.168.0.42", 5001);
            try {
                cameraSocket.connect(socketAddress);
            } catch (IOException e) {
                e.printStackTrace();
                callback.callback(parseResponse(Constants.ERROR, null));
                return;
            }

            callback.callback(parseResponse(Constants.SUCCESS, null));
        }
    }

    /**
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
            callback.callback(parseResponse(Constants.SUCCESS, null));
        }
    }

    /**
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
                while(!eof) {
                    bytesRead = bufferedInputStream.read(buffer, 0, buffer.length);
                    response = response += new String(buffer, 0, bytesRead);
                    if (response.substring(response.length() -1).equals("\3")) {
                        eof = true;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                callback.callback(parseResponse(Constants.ERROR, null));
            }

            callback.callback(parseResponse(Constants.SUCCESS, response));
        }
    }

    JSONObject parseResponse(final String resultCode, String response) {
        try {
            JSONObject object = new JSONObject(resultCode);
            if (response != null) {
                //strip out start/stop bytes
                response = response.substring(1, response.length() -1);
                JSONObject responseObj = new JSONObject(response);
                object.put("response", responseObj);
            }
            return object;
        } catch (JSONException e) {
            e.printStackTrace();
            return null; //TODO fix this
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            cameraSocket.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        cameraSocket = null;
    }
}