package com.darcangel.acam.ui.camera;

import androidx.lifecycle.MutableLiveData;

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.constants.Constants;
import com.google.gson.internal.bind.TreeTypeAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

import io.reactivex.rxjava3.subjects.PublishSubject;
import timber.log.Timber;

public class CameraService {

    private Socket cameraSocket;
    private byte[] buffer = new byte[4096];
    private String response;
    private String command;
    private BufferedInputStream bufferedInputStream;
    private boolean isStreaming = false;
    private TreeTypeAdapter streamThread;
    private String ipAddress;
    private final PublishSubject<JSONObject> imageChannel = PublishSubject.create();
    private final MainActivity mainActivity;

    public CameraService() {
        cameraSocket = new Socket();
        mainActivity = MainActivity.getInstance();
        //Listen for changes in ipAddress
        MutableLiveData<String> camera = mainActivity.getSettings().getLiveDataCameraAddress();
        camera.observe(mainActivity, this::setIpAddress);
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
     * startStreaming
     */
    public void startStreaming() {
        String args = String.format(Constants.ARGS_SET_STREAM_ON, 0, 0);
        command = String.format(Constants.CMD_SET_STREAM_ON, args);
        Runnable streamer = new Stream();
        isStreaming = true;
        new Thread(streamer).start();
    }

    public void stopStreaming() {
        isStreaming = false;
//        command = Constants.CMD_SET_STREAM_OFF;
//        Runnable sco = new SendCmdOnly();
//        new Thread(sco).start();
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
                cameraSocket.setKeepAlive(true);
                imageChannel.onNext(parseResponse("\2{\"connected\":\"true\"}\3"));
            } catch (IOException e) {
                e.printStackTrace();
                imageChannel.onError(e);
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
     * SendCmdOnly
     * Thread to send a command and receive the response
     */
    class SendCmdOnly implements Runnable {
        @Override
        public void run() {
            boolean eof = false;
            int bytesRead = 0;
            response = "";

            try {
                cameraSocket.getOutputStream().write(command.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {

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
     * Stream
     * Thread to send a command and receive the response
     */
    class Stream implements Runnable {
        @Override
        public void run() {
            int bytesRead = 0;
            int threePos = -1;
            response = "";

            try {
                if(bufferedInputStream == null) {
                    bufferedInputStream = new BufferedInputStream(cameraSocket.getInputStream());
                }
                cameraSocket.getOutputStream().write(command.getBytes(StandardCharsets.UTF_8));
                //Timber.d("Sent cmd: '%s' to camera", command);
                while (isStreaming) {
                    bytesRead = bufferedInputStream.read(buffer, 0, buffer.length);
                    //Timber.d("Read %d bytes from camera", bytesRead);
                    for (threePos = 0; threePos < bytesRead; threePos++) {
                        if (buffer[threePos] == '\3') {
                            //Timber.d("Found end of image at %d", threePos);
                            break;
                        }
                    }
                    //Timber.d("bytesRead = %d, threePos = %d", bytesRead, threePos);
                    if (bytesRead - threePos > 0) {
                        response += new String(buffer, 0, threePos + 1);
                        JSONObject jsonString = parseResponse(response);

                        //Timber.d("Sending onNext()");
                        imageChannel.onNext(jsonString);
                        int bytesLeft = bytesRead - threePos;
                        if(bytesLeft > 0) {
                            //Timber.d("There were %d bytes left in the buffer", bytesLeft);
                            response = new String(buffer, threePos+1, bytesLeft-1);
                            //Timber.d("New Response is '%s'", response);
                        } else {
                            response = "";
                        }
                    } else {
                        response += new String(buffer, 0, bytesRead);
                    }
                }
                command = Constants.CMD_SET_STREAM_OFF;
                cameraSocket.getOutputStream().write(command.getBytes(StandardCharsets.UTF_8));
                //flush out any unprocessed images
                do {
                    bytesRead = bufferedInputStream.read(buffer, 0, buffer.length);
                    Timber.d("Flushing %d bytes, last = %d", bytesRead, buffer[bytesRead-1]);
                    if(buffer[bytesRead-1] == 3) {
                        bytesRead = -1;
                    }
                } while(bytesRead > 0);
                //Timber.d("Buffer flushed");
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
                //Timber.d("parseResponse('%s')", response);
                //strip out start/stop bytes
                response = response.substring(1, response.length() - 1);
                return new JSONObject(response);
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