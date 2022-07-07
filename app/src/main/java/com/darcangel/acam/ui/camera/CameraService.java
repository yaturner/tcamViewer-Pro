package com.darcangel.acam.ui.camera;

import androidx.lifecycle.MutableLiveData;

import com.darcangel.acam.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

import io.reactivex.rxjava3.subjects.PublishSubject;

public class CameraService {

    private Socket cameraSocket;
    private PrintStream printStream;
    private byte[] buffer = new byte[4096];
    private String response;
    private String command;
    private BufferedInputStream bufferedInputStream;
    private String ipAddress;
    private final PublishSubject<JSONObject> imageChannel = PublishSubject.create();
    private MainActivity mainActivity;

    public CameraService() {
        cameraSocket = new Socket();
        mainActivity = MainActivity.getInstance();
        //Listen for changes in ipAddress
        MutableLiveData<String> camera = mainActivity.getSettings().getLiveDataCameraAddress();
        camera.observe(mainActivity, s -> setIpAddress(s));
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
            disconnect();
            connect();
        }
        ipAddress = address;
    }

    /**
     * connect
     *
     */
    public void connect() {
        Runnable connect = new ConnectSocket();
        new Thread(connect).start();
    }

    /**
     * disconnect
     *
     */
    public void disconnect() {
        Runnable disconnect = new DisconnectSocket();
        new Thread(disconnect).start();
    }

    /**
     * sendCmd
     *
     * @param cmd
     * @throws IOException
     */
    public void sendCmd(final String cmd) throws IOException {
        command = cmd;
        Runnable sendCmd = new SendCmd();
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
        @Override
        public void run() {
            SocketAddress socketAddress = new InetSocketAddress(ipAddress, 5001);
            try {
                cameraSocket.connect(socketAddress);
                imageChannel.onNext(parseResponse("\2{\"connected\":\"true\"}\3"));
            } catch (IOException e) {
                e.printStackTrace();
                imageChannel.onError(e);
                return;
            }
        }
    }

    /**
     * DisconnectSocket
     * Thread to disconnect socket
     */
    class DisconnectSocket implements Runnable {
        @Override
        public void run() {
            try {
                cameraSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                imageChannel.onError(e);
                return;
            }
        }
    }

    /**
     * SendCmd
     * Thread to send a command and receive the response
     */
    class SendCmd implements Runnable {
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
                imageChannel.onNext(parseResponse(response));
            } catch (IOException e) {
                e.printStackTrace();
                imageChannel.onError(e);
            }
        }
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
                //strip out start/stop bytes
                response = response.substring(1, response.length() - 1);
                JSONObject object = new JSONObject(response);
                return object;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null; //TODO fix this
        }
        return null;
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

    public PublishSubject<JSONObject> getImageChannel() {
        return imageChannel;
    }
}