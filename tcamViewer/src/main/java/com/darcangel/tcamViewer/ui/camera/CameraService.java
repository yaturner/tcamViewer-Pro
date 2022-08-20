package com.darcangel.tcamViewer.ui.camera;

import android.os.Parcel;
import android.os.Parcelable;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.constants.Constants;
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

public class CameraService implements Parcelable {

    private Socket cameraSocket;
    private byte[] buffer;
    private String response;
    private String command;
    private BufferedInputStream bufferedInputStream;
    private Boolean isStreaming = false;
    private TreeTypeAdapter streamThread;
    private String ipAddress;
    private final PublishSubject<JSONObject> imageChannel = PublishSubject.create();
    private MainActivity mainActivity;
    private Thread streamingThread;

    public CameraService() {
        cameraSocket = new Socket();
        mainActivity = MainActivity.getInstance();
        buffer = new byte[4096];
    }

    public CameraService(Parcel in) {
        cameraSocket = new Socket();
        mainActivity = MainActivity.getInstance();
        buffer = new byte[4096];
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
        dest.writeInt(isStreaming?1:0);
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
        try {
            Runnable connect = new ConnectSocket();
            Thread thread = new Thread(connect);
            thread.start();
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * disconnect
     */
    public void disconnect() {
        try {
            Runnable disconnect = new DisconnectSocket();
            Thread thread = new Thread(disconnect);
            thread.start();
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
        Thread cmdThread = new Thread(sendCmd);
        cmdThread.start();
        try {
            cmdThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * sendCmdNoResponse
     *
     * @param cmd
     * @throws IOException
     */
    public void sendCmdNoResponse(final String cmd) throws IOException {
        command = cmd;
        Runnable sendCmd = new SendCmdNoResponse();
        new Thread(sendCmd).start();
    }

    /**
     * isConnected
     *
     * @return
     */
    public boolean isConnected() {
        if (cameraSocket != null && !cameraSocket.isClosed() && cameraSocket.isConnected()) {
            return true;
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
        streamingThread = new Thread(streamer);
        streamingThread.start();
    }

    public void stopStreaming() {
        isStreaming = false;
        try {
            streamingThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * connectSocket
     * Thread to connect to the camera
     */
    class ConnectSocket implements Runnable {
        @Override
        public void run() {
            try {
                if (cameraSocket != null && (cameraSocket.isClosed() || !cameraSocket.isConnected())) {
                    cameraSocket = new Socket();
                }
                SocketAddress socketAddress = new InetSocketAddress(ipAddress,5001);
                cameraSocket.connect(socketAddress, 5000);
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
                imageChannel.onNext(parseResponse("\2{\"connected\":\"false\"}\3"));
            } catch (IOException e) {
                e.printStackTrace();
                imageChannel.onError(e);
                return;
            }
        }
    }

    /**
     * SendCmdOnly
     * Thread to send a command with no response
     */
    class SendCmdNoResponse implements Runnable {
        @Override
        public void run() {
            boolean eof = false;
            int bytesRead = 0;
            response = "";

            try {
                cameraSocket.getOutputStream().write(command.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
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
                if (bufferedInputStream == null) {
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
                        if (bytesLeft > 0) {
                            //Timber.d("There were %d bytes left in the buffer", bytesLeft);
                            Timber.d("buffer is %s null, threePos = %d, bytesLeft = %d",
                                    (buffer==null?"":"not"), threePos, bytesLeft);
                            response = new String(buffer, threePos + 1, bytesLeft - 1);
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
                    Timber.d("Flushing %d bytes, last = %d", bytesRead, buffer[bytesRead - 1]);
                    if (buffer[bytesRead - 1] == 3) {
                        bytesRead = -1;
                    }
                } while (bytesRead > 0);
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
            imageChannel.onError(e);
        }
        return new JSONObject();
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