package com.darcangel.tcamViewer.ui.camera;

import android.os.Parcel;
import android.os.Parcelable;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.constants.Constants;
import com.google.gson.internal.bind.TreeTypeAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

import io.reactivex.rxjava3.subjects.PublishSubject;
import timber.log.Timber;

public class CameraService implements Parcelable {

    private Socket cameraSocket;
    private char[] buffer;
    private String command;
    private BufferedReader bufferedReader;
    private Boolean isStreaming = false;
    private TreeTypeAdapter streamThread;
    private String ipAddress;
    private final PublishSubject<JSONObject> imageChannel = PublishSubject.create();
    private MainActivity mainActivity;
    private Thread streamingThread;
    private static enum STATE
    {
        STATE_FIND_START,
        STATE_FIND_END,
        STATE_GET_NEXT_BUFFER
    }
    private STATE readState = STATE.STATE_GET_NEXT_BUFFER;
    private STATE prevReadState = STATE.STATE_FIND_START;
    private String cmd;
    private int lastPos = 0;
    private int nextPos = 0;
    private int bytesRead = 0;
    private int endPos = -1;
    private byte[] response;

    public CameraService() {
        cameraSocket = new Socket();
        mainActivity = MainActivity.getInstance();
        buffer = new char[Constants.READ_BUFFER_SIZE];
        response = new byte[Constants.READ_BUFFER_SIZE];
    }

    public CameraService(Parcel in) {
        cameraSocket = new Socket();
        mainActivity = MainActivity.getInstance();
        buffer = new char[Constants.READ_BUFFER_SIZE];
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
            Runnable readResponse = new ReadResponse();
            Thread thread1 = new Thread(readResponse);
            thread1.start();
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
        String args = String.format(Constants.ARGS_SET_STREAM_ON, 125, 0);
        command = String.format(Constants.CMD_SET_STREAM_ON, args);
        try {
            lastPos = 0;
            nextPos = 0; //TODO make a method out of this
            sendCmd(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
        isStreaming = true;
    }

    public void stopStreaming() {
        isStreaming = false;
        lastPos = 0;
        nextPos = 0;
        try {
            sendCmd(Constants.CMD_SET_STREAM_OFF);
        } catch (IOException e) {
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
            response = new byte[Constants.READ_BUFFER_SIZE];
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
            response = new byte[Constants.READ_BUFFER_SIZE];

            try {
                bufferedReader = new BufferedReader(
                        new InputStreamReader(cameraSocket.getInputStream()));
                cameraSocket.getOutputStream().write(command.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
                imageChannel.onError(e);
            }
        }
    }

    /**
     * ReadResponse
     * continuously read responses from the camera and pass them to the parser
     */
    class ReadResponse implements Runnable {
        @Override
        public void run() {
            boolean eof = false;
            nextPos = 0;
            lastPos = 0;

            while(isConnected()) {
                try {
                    int pos;
                     bufferedReader = new BufferedReader(
                             new InputStreamReader(cameraSocket.getInputStream()));
                    switch (readState) {
                        case STATE_GET_NEXT_BUFFER:
                            Timber.d("Entered STATE_GET_NEXT_BUFFER");
                            Timber.d("Before Read, lastPos = %d, nextPos = %d",
                                    lastPos, nextPos);
                            bytesRead = bufferedReader.read(buffer, nextPos, Constants.READ_SIZE);
                            lastPos = nextPos;
                            nextPos += bytesRead;
                            readState = prevReadState;
                            break;
                        case STATE_FIND_START:
                            Timber.d("Entered STATE_FIND_START");
                            pos = findByte(buffer, lastPos, nextPos - lastPos, 2);
                            if (pos != -1) {
                                Timber.d("Found start at %d", nextPos + pos);

                                readState = STATE.STATE_FIND_END;
                                prevReadState = readState;
                            } else {
                                Timber.d("Did not find start");
                                prevReadState = STATE.STATE_FIND_START;
                                readState = STATE.STATE_GET_NEXT_BUFFER;
                            }
                            break;
                        case STATE_FIND_END:
                            Timber.d("Entered STATE_FIND_END");
                            if ((endPos = findByte(buffer, lastPos, nextPos - lastPos, 3)) != -1) {
                                Timber.d("Found end at %d", endPos);
                                cmd = new String(buffer, 0, endPos+1);
                                Timber.d("cmd = '%s'", cmd);

                                imageChannel.onNext(parseResponse(cmd));
                                if(cmd.length() == nextPos) {
                                    nextPos = 0;
                                    lastPos = 0;
                                    Timber.d("response is empty");
                                    readState = STATE.STATE_GET_NEXT_BUFFER;
                                    prevReadState = STATE.STATE_FIND_START;
                                } else {
                                    int bytesLeft = nextPos-endPos;
                                    Timber.d("There are %d bytes left in the buffer", bytesLeft);
                                    for(int iPos = endPos+1, startPos = 0; iPos < nextPos; iPos++, startPos++) {
                                        buffer[startPos] = buffer[iPos];
                                    }
                                    lastPos = bytesLeft;
                                    nextPos = bytesLeft;
                                    readState = STATE.STATE_FIND_START;
                                    prevReadState = readState;
                                }
                            } else {
                                Timber.d("Did not find end");
                                prevReadState = STATE.STATE_FIND_END;
                                readState = STATE.STATE_GET_NEXT_BUFFER;
                            }
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    imageChannel.onError(e);
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                    imageChannel.onError(e);
                }
            }
        }
    }

    private int findByte(char[] bytes, int offset, int length, int value) {
        Timber.d("Searching for %d starting at %d for %d bytes", value, offset, length);
        int index;
        for(index = offset; index < offset + length; index++) {
            if(bytes[index] == value) {
                Timber.d("Found %d at %d", value, index);
                return index;
            }
        }
        Timber.d("\\\\findByte\\\\buffer pos = %d", index);
        if(index > 0) {
            Timber.d("\\\\findByte\\\\bytes[%d] = %c", index - 1, bytes[index - 1]);
        }
        Timber.d("\\\\findByte\\\\bytes[%d] = %c", index, bytes[index]);
        return -1;
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
            response = new byte[Constants.READ_BUFFER_SIZE];

//            try {
//                if (bufferedReader == null) {
//                    bufferedReader = new bufferedReader(cameraSocket.getInputStream());
//                }
//                cameraSocket.getOutputStream().write(command.getBytes(StandardCharsets.UTF_8));
//                //Timber.d("Sent cmd: '%s' to camera", command);
//                while (isStreaming) {
//                    bytesRead = bufferedReader.read(buffer, 0, buffer.length);
//                    //Timber.d("Read %d bytes from camera", bytesRead);
//                    for (threePos = 0; threePos < bytesRead; threePos++) {
//                        if (buffer[threePos] == '\3') {
//                            //Timber.d("Found end of image at %d", threePos);
//                            break;
//                        }
//                    }
//                    //Timber.d("bytesRead = %d, threePos = %d", bytesRead, threePos);
//                    if (bytesRead - threePos > 0) {
//                        response += new String(buffer, 0, threePos + 1);
//                        JSONObject jsonString = parseResponse(response);
//                        //Timber.d("Sending onNext()");
//                        imageChannel.onNext(jsonString);
//                        int bytesLeft = bytesRead - threePos;
//                        if (bytesLeft > 0) {
//                            //Timber.d("There were %d bytes left in the buffer", bytesLeft);
//                            Timber.d("buffer is %s null, threePos = %d, bytesLeft = %d",
//                                    (buffer==null?"":"not"), threePos, bytesLeft);
//                            response = new String(buffer, threePos + 1, bytesLeft - 1);
//                            //Timber.d("New Response is '%s'", response);
//                        } else {
//                            response = "";
//                        }
//                    } else {
//                        response += new String(buffer, 0, bytesRead);
//                    }
//                }
//                command = Constants.CMD_SET_STREAM_OFF;
//                cameraSocket.getOutputStream().write(command.getBytes(StandardCharsets.UTF_8));
//                //flush out any unprocessed images
//                do {
//                    bytesRead = bufferedReader.read(buffer, 0, buffer.length);
//                    Timber.d("Flushing %d bytes, last = %d", bytesRead, buffer[bytesRead - 1]);
//                    if (buffer[bytesRead - 1] == 3) {
//                        bytesRead = -1;
//                    }
//                } while (bytesRead > 0);
//                //Timber.d("Buffer flushed");
//            } catch (IOException e) {
//                e.printStackTrace();
//                imageChannel.onError(e);
//            }
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