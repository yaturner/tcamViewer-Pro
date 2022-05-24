package com.darcangel.acam.utils;

import android.graphics.Bitmap;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;


public class Util {

    public static Bitmap processImageResponse(JSONObject response) throws JSONException {
        String radiometricString = response.getString("radiometric");
        String telemetryString = response.getString("telemetry");
        byte[] imageBytes = Base64.getDecoder().decode(radiometricString.getBytes(StandardCharsets.UTF_8));
        byte[] telemetryBytes = Base64.getDecoder().decode(telemetryString.getBytes(StandardCharsets.UTF_8));

        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        int imageLen = imageBytes.length;
        Integer[] imageData = new Integer[imageLen/2];
        for(int i=0, j=0; i < imageLen; i=i+2, j++) {
            imageData[j] = ((imageBytes[i]&0xff)<<8) | (imageBytes[i+1] & 0xff);
            min = Math.min(imageData[j], min );
            max = Math.max(imageData[j], max);
        }
        int diff = max - min;
        for(int i=0; i<imageData.length; i++) {

        }
        return null;
    }
}