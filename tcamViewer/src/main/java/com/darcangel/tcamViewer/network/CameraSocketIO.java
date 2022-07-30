package com.darcangel.tcamViewer.network;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.constants.Constants;

import java.io.BufferedReader;
import java.net.InetAddress;
import java.net.Socket;

public class CameraSocketIO implements Runnable {

    private String cameraAddress;
    private Socket clientSocket;
    private BufferedReader input;

    /***
    public String sendCmd(final String cmd) {
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(clientSocket.getOutputStream())),
                    true);
            out.println(cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
     */

    @Override
    public void run() {
        if (cameraAddress == null || cameraAddress.isEmpty()) {
            cameraAddress = MainActivity.getInstance().getSharedPreferences().getString(Constants.KEY_CAMERA_IP_ADDRESS, "");
            if (cameraAddress.isEmpty()) {
                //TODO error
            }
        }
        try {
            InetAddress cameraIPAddress = InetAddress.getByName(cameraAddress);
            clientSocket = new Socket(cameraIPAddress, 5001);
        } catch (Exception e) {
            //TODO error
            e.printStackTrace();
        }
    }
}