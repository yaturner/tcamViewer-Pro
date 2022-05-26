package com.darcangel.acam.utils;

import android.graphics.Bitmap;
import android.util.Pair;

import com.darcangel.acam.constants.Constants;
import com.darcangel.acam.pallete.Rainbow;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Base64;


public class Util {
    private boolean AGC;
    private boolean shutdown;
    private int emissivity;
    private int TLinearEnabled;
    private int TLinear;
    private int spotmeterMean;
    private Pair<Integer, Integer> spotLoc;
    private boolean sutterLockout;
    private int FFCState;
    private int FFCDesired;

    private int offsetA = 0;
    private int offsetB = 80;
    private int offsetC = 160;


    public Bitmap processImageResponse(JSONObject response) throws JSONException {
        String radiometricString = response.getString("radiometric");
        String telemetryString = response.getString("telemetry");
        byte[] imageBytes = Base64.getDecoder().decode(radiometricString.getBytes());
        Bitmap image = null;

        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        int imageLen = imageBytes.length;
        int[] imageData = new int[imageLen / 2];
        int[] imageNorm = new int[imageLen / 2];
        int[] pixels = new int[Constants.IMAGE_WIDTH * Constants.IMAGE_HEIGHT];
        int[] telemetryData;

        telemetryData = parseTelemetryData(telemetryString);
        int status = ((telemetryData[4] & 0xffff) << 16) | (telemetryData[3] & 0xffff);
        AGC = ((status & Constants.TELEMETRY_MASK_AGC) == Constants.TELEMETRY_MASK_AGC);
        shutdown = ((status & Constants.TELEMETRY_MASK_SHUTDOWN) == Constants.TELEMETRY_MASK_SHUTDOWN);
        emissivity = telemetryData[offsetB + 19];
        TLinearEnabled = telemetryData[offsetC + 48];
        TLinear = telemetryData[offsetC + 49];
        spotmeterMean = telemetryData[offsetC + 50];
        Integer x = (telemetryData[offsetC+25]&0xffff)|(telemetryData[23]&0xffff);
        Integer y = (telemetryData[offsetC+24]&0xffff)|(telemetryData[22]&0xffff);
        spotLoc = new Pair<>(x, y);

        for (int i = 0, j = 0; i < imageLen; i = i + 2, j++) {
            imageData[j] = ((imageBytes[i + 1] & 0xff) << 8) | (imageBytes[i] & 0xff);
            min = Math.min(imageData[j], min);
            max = Math.max(imageData[j], max);
        }
        if(AGC) {
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = rgbToPixel(Rainbow.palette[imageData[i]]);
            }
        } else {
            int diff = max - min;
            for (int i = 0; i < imageNorm.length; i++) {
                imageNorm[i] = Math.min(((imageData[i] - min) * 255) / diff, 255);
            }
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = rgbToPixel(Rainbow.palette[imageNorm[i]]);
            }
        }
        image = Bitmap.createBitmap(pixels, Constants.IMAGE_WIDTH, Constants.IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
        return image;
    }


    private int rgbToPixel(int[] rgb) {
        int r = rgb[0] & 0xFF;
        int g = rgb[1] & 0xFF;
        int b = rgb[2] & 0xFF;
        return (0xff << 24) | (r << 16) | ((g << 8) | b);
    }

    private static int[] parseTelemetryData(String telemetryString) {
        byte[] telemetryBytes = Base64.getDecoder().decode(telemetryString.getBytes());
        int[] telemetryData;

        int len = telemetryBytes.length;
        telemetryData = new int[len / 2];

        for (int i = 0, j = 0; i < len; i = i + 2, j++) {
            telemetryData[j] = ((telemetryBytes[i + 1] & 0xff) << 8) | (telemetryBytes[i] & 0xff);
        }
        return telemetryData;
    }

}