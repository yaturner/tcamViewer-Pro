package com.darcangel.acam.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

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
    private byte[] response = new byte[128];
    private String command;
    private final IBinder cameraBinder = new LocalBinder();

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
    public void connect() {
        Runnable connect = new ConnectSocket();
        new Thread(connect).start();
    }

    public String sendCmd(final String cmd) throws IOException {
        command = cmd;
        Runnable sendCmd = new SendCmd();
        new Thread(sendCmd).start();
        return new String(response);
    }


    class ConnectSocket implements Runnable {

        @Override
        public void run() {
            SocketAddress socketAddress = new InetSocketAddress("192.168.0.42", 5001);
            try {
                cameraSocket.connect(socketAddress);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    class SendCmd implements Runnable {
        @Override
        public void run() {
            try {
                cameraSocket.getOutputStream().write(command.getBytes(StandardCharsets.UTF_8));
                cameraSocket.getInputStream().read(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
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