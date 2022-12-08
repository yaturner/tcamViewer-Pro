package com.darcangel.tcamViewer.JNI;

import com.darcangel.tcamViewer.MainActivity;

public class CameraServiceJNI {
    public native boolean init(MainActivity.JNIListener JNIListener);
    public native boolean sendCommand(String cmd);
    public native boolean isConnected();
    public native void startListening();
}
