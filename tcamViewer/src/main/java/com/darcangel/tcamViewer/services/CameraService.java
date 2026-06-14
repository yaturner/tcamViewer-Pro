package com.darcangel.tcamViewer.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.constants.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.SocketAddress;

import java.nio.charset.StandardCharsets;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.BackpressureOverflowStrategy;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.sentry.Sentry;
import timber.log.Timber;

public class CameraService extends Service {
    private IBinder binder;
    private Socket cameraSocket;
    private String command;
    private Boolean isStreaming = false;
    private String ipAddress;
    private PublishSubject<JSONObject> imageChannel;
    private final MainActivity mainActivity = MainActivity.getInstance();
    private JSONObject jsonObject;
    private Thread listenerThread;
    private boolean running = false;
    private int totalBytesRead = 0;
    private int bytes_read = 0;
    private int responsePos = 0;
    private InputStream inFromSocket;
    private OutputStream outToSocket;
    private byte[] readBuffer;
    private char[] response;
    private boolean startFound, endFound;
    private String cameraCommand;
    private StringBuilder sb = new StringBuilder();

    private long prevTime = 0L;

    public class CameraServiceBinder extends Binder {
        public CameraService getService() {
            return CameraService.this;
        }
    }

    /******************************************
     *             Runnables                  *
     ******************************************/
    /**
     * connectRunnable
     */
    Runnable connectRunnable = new Runnable() {
        public void run() {
            try {
                cameraSocket = new Socket();
                SocketAddress address = new InetSocketAddress(ipAddress, 5001);
                int timeoutMs = 30 * 1000;
                cameraSocket.connect(address, timeoutMs);
                if (cameraSocket != null) {
                    inFromSocket = cameraSocket.getInputStream();
                    outToSocket = cameraSocket.getOutputStream();
                }
            } catch (SocketTimeoutException e) {
                // Handle a failure caused specifically by the timeout expiring
                e.printStackTrace();

            } catch (IOException e) {
                // Handle other connection errors (e.g., connection refused, wrong IP)
                e.printStackTrace();

            }
        }
    };


    /**
     * listeningRunnable
     */
    Runnable listeningRunnable = new Runnable() {
        @Override
        public void run() {
            startListening();
//            Timber.d("Return from startListening");
        }
    };

    /**
     * sendCmdRunnable
     */
    Runnable sendCmdRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                outToSocket.write(cameraCommand.getBytes(StandardCharsets.UTF_8));
                outToSocket.flush();
            } catch (Exception e) {
                cameraSocket = null;
                String errorMsg = String.format(Constants.ERROR_RESPONSE, e.toString());
                imageChannel.onNext(parseResponse(errorMsg));
                Sentry.captureException(e);
            }
        }
    };

    @Override
    public void onCreate() {
        binder = new CameraServiceBinder();
        imageChannel = PublishSubject.create();
        imageChannel.observeOn(AndroidSchedulers.mainThread())
                .toFlowable(BackpressureStrategy.BUFFER).onBackpressureBuffer(256, () -> {},
                        BackpressureOverflowStrategy.DROP_LATEST);

        readBuffer = new byte[Constants.BUFFER_LENGTH];
        response = new char[Constants.BUFFER_LENGTH];
        cameraSocket = new Socket();
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
        Thread connectThread = new Thread(connectRunnable);
        try {
            running = true;
            connectThread.start();
            connectThread.join(150 * 1000);
        } catch (InterruptedException e) {
            Sentry.captureException(e);
            return false;
        }


        if (isConnected()) {
            Thread listeningThread = new Thread(listeningRunnable);
            listeningThread.run();
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
        if (isConnected()) {
            stopStreaming();
            stopListening();
            try {
                cameraSocket.close();
            } catch (IOException e) {
                Sentry.captureException(e);
            }
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
        if (isConnected()) {
            cameraCommand = cmd;
            Thread sendCmdThread = new Thread(sendCmdRunnable);
            try {
                sendCmdThread.start();
                sendCmdThread.join(15 * 1000);
            } catch (Exception e) {
                Sentry.captureException(e);
            }
        }
    }

    /**
     * isConnected
     *
     * @return
     */
    public boolean isConnected() {
        if (cameraSocket == null ||
                cameraSocket.isClosed() ||
                !cameraSocket.isConnected()) {
            return false;
        } else {
            return true;
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

        // Use a background thread properly
        listenerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Cache frequently accessed/mutated state locally for speed
                int localResponsePos = 0;
                boolean startFound = false;

                while (isConnected() && running) {
                    long prevTime = SystemClock.elapsedRealtime();
                    try {
                        bytes_read = inFromSocket.read(readBuffer);

                        // End of stream reached (-1)
                        if (bytes_read == -1) {
                            running = false;
                            break;
                        }
                    } catch (IOException e) {
                        // Cleaner, safer check for closed socket
                        if (e instanceof java.net.SocketException || "Socket closed".equals(e.getMessage())) {
                            running = false;
                            break; // Stop looping if socket is dead
                        }
                        String jsonString = String.format(Constants.ERROR_RESPONSE, e.toString());
                        imageChannel.onNext(parseResponse(jsonString));
                        continue;
                    }

                    if (bytes_read == 0) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt(); // Restore interrupted status
                            Sentry.captureException(e);
                        }
                        continue;
                    }

                    // Highly optimized parsing loop
                    for (int index = 0; index < bytes_read; index++) {
                        byte b = readBuffer[index]; // Keep it as byte, don't cast to char yet

                        if (b == 0x02) { // '\02' STX (Start of Text)
                            startFound = true;
                            localResponsePos = 0; // Reset directly instead of flag juggling
                        } else if (startFound && b == 0x03) { // '\03' ETX (End of Text)
                            // Convert the precise byte array slice directly to a String (Saves memory & CPU)
                            String r = new String(response, 0, localResponsePos);

                            Timber.d("\\\\response\\\\ response = '%s'", r.substring(0, Math.min(r.length(), 64)));

                            imageChannel.onNext(parseResponse(r));

                            // Reset local flags
                            startFound = false;
                            localResponsePos = 0;
                        } else if (startFound) {
                            // Prevent ArrayOutOfBoundsException if response buffer fills up
                            if (localResponsePos < response.length) {
                                response[localResponsePos++] = (char) b;
                            }
                        }
                        totalBytesRead++;
                    }
                }
            }
        });

        // CRITICAL FIX: Use .start() to actually run this on a background thread!
        listenerThread.start();
    }

    void resetBuffers() {
        responsePos = 0;
        endFound = false;
        startFound = false;
        totalBytesRead = 0;
        //sb = new StringBuilder();
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
                return new JSONObject(response);
            }
        } catch (JSONException e) {
            handleError(e);
            return new JSONObject();
        }
        return new JSONObject();
    }

    private void handleError(Exception e) {
        Sentry.captureException(e);
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

