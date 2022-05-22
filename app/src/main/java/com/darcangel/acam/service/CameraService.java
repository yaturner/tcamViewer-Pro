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
import java.security.Provider;

public class CameraService extends Service {

    Socket cameraSocket;
    PrintStream printStream;
    final IBinder cameraBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return cameraBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        cameraSocket = new Socket();
    }

    public class LocalBinder extends Binder {
        public CameraService getService() {
            return CameraService.this;
        }
    }

    public void onStart(Intent intent, int startId) {
//        super.onStart(intent, startId);
        Toast.makeText(this, "Service created ...", Toast.LENGTH_LONG).show();
        Runnable connect = new connectSocket();
        new Thread(connect).start();
    }

    class connectSocket implements Runnable {

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

    public void IsBoundable(){
        Toast.makeText(this,"I bind like butter", Toast.LENGTH_LONG).show();
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