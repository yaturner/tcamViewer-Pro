package com.darcangel.tcamViewer.model;

import android.graphics.Bitmap;
import android.graphics.Rect;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.utils.CameraUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ImageDto {

    private boolean AGC;
    private boolean shutdown;
    private int emissivity;
    private int TLinearEnabled;
    private int TLinearResolution; // 0 = 0.1, 1 = 0.01
    private int spotmeterMean;
    private Rect spotmeterLocation;
    private boolean shutterLockout;
    private int FFCState;
    private int FFCDesired;
    private int gainMode;
    private int autoGainMode;
    private JSONObject jsonObject;
    private String filename;
    private int[][] palette;
    private String paletteName;
    private Bitmap bitmap;

    private CameraUtils cameraUtils;

    //Constructor from camera response
    public ImageDto(JSONObject jsonObject, String paletteName) {
        this.jsonObject = jsonObject;
        this.paletteName = paletteName;
        init();
    }

    //Constructor from file
    public ImageDto(String filename, String paletteName) {
        this.filename = filename;
        this.paletteName = paletteName;
        String line = "";
        String json = "";
        String imageName = filename.substring(filename.lastIndexOf(File.separatorChar)+1);
        try {
            BufferedReader bufferedReader = new BufferedReader(
                    new FileReader(new File(filename)));
            do {
                line = bufferedReader.readLine();
                if (line != null) {
                    json = json + line;
                }
            } while (line != null);
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            json = "";
        }
        if (!json.isEmpty()) {
            try {
                jsonObject = new JSONObject(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        init();
    }

    private void init() {
        cameraUtils = MainActivity.getInstance().getCameraUtils();
        palette = MainActivity.getInstance().getPaletteFactory().getPaletteByName(paletteName);

        try {
            cameraUtils.processImageResponse(this);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public boolean isAGC() {
        return AGC;
    }

    public void setAGC(boolean AGC) {
        this.AGC = AGC;
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public void setShutdown(boolean shutdown) {
        this.shutdown = shutdown;
    }

    public int getEmissivity() {
        return emissivity;
    }

    public void setEmissivity(int emissivity) {
        this.emissivity = emissivity;
    }

    public int getTLinearEnabled() {
        return TLinearEnabled;
    }

    public void setTLinearEnabled(int TLinearEnabled) {
        this.TLinearEnabled = TLinearEnabled;
    }

    public int getTLinearResolution() {
        return TLinearResolution;
    }

    public void setTLinearResolution(int TLinearResolution) {
        this.TLinearResolution = TLinearResolution;
    }

    public int getSpotmeterMean() {
        return spotmeterMean;
    }

    public void setSpotmeterMean(int spotmeterMean) {
        this.spotmeterMean = spotmeterMean;
    }

    public Rect getSpotmeterLocation() {
        return spotmeterLocation;
    }

    public void setSpotmeterLocation(Rect spotmeterLocation) {
        this.spotmeterLocation = spotmeterLocation;
    }

    public boolean isShutterLockout() {
        return shutterLockout;
    }

    public void setShutterLockout(boolean shutterLockout) {
        this.shutterLockout = shutterLockout;
    }

    public int getFFCState() {
        return FFCState;
    }

    public void setFFCState(int FFCState) {
        this.FFCState = FFCState;
    }

    public int getFFCDesired() {
        return FFCDesired;
    }

    public void setFFCDesired(int FFCDesired) {
        this.FFCDesired = FFCDesired;
    }

    public int getGainMode() {
        return gainMode;
    }

    public void setGainMode(int gainMode) {
        this.gainMode = gainMode;
    }

    public int getAutoGainMode() {
        return autoGainMode;
    }

    public void setAutoGainMode(int autoGainMode) {
        this.autoGainMode = autoGainMode;
    }

    public int[][] getPalette() {
        return palette;
    }

    public void setPalette(int[][] palette) {
        this.palette = palette;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
