package com.darcangel.acam.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.darcangel.acam.constants.Constants;
import com.darcangel.acam.pallete.DoubleRainBow;
import com.darcangel.acam.pallete.Rainbow;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;


public class Util {

    public static Bitmap processImageResponse(JSONObject response) throws JSONException {
        String radiometricString = response.getString("radiometric");
        String telemetryString = response.getString("telemetry");
        byte[] imageBytes = Base64.getDecoder().decode(radiometricString.getBytes());
        byte[] telemetryBytes = Base64.getDecoder().decode(telemetryString.getBytes());
        Bitmap image = null;

        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        int imageLen = imageBytes.length;
        int[] imageData = new int[imageLen/2];
        int[] imageNorm = new int[imageLen/2];
        int[] pixels = new int[Constants.IMAGE_WIDTH*Constants.IMAGE_HEIGHT];
        for(int i=0, j=0; i < imageLen; i=i+2, j++) {
            imageData[j] = ((imageBytes[i+1]&0xff)<<8) | (imageBytes[i]&0xff);
            min = Math.min(imageData[j], min );
            max = Math.max(imageData[j], max);
        }
        int diff = max - min;
        for(int i=0; i<imageNorm.length; i++) {
            imageNorm[i] = Math.min(((imageData[i]- min)*255)/diff, 255);
        }

//        for(int row=0; row<image.getHeight(); row++) {
//            for(int col=0; col<image.getWidth(); col++) {
//                image.setPixel(col, row, rgbToPixel(Rainbow.palette[imageNorm[row*col+col]]));
//            }
//        }
        for(int i=0; i<pixels.length; i++) {
            pixels[i] = rgbToPixel(Rainbow.palette[imageNorm[i]]);
        }
        image = Bitmap.createBitmap(pixels, Constants.IMAGE_WIDTH, Constants.IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
        return image;
    }

    
    private static int rgbToPixel(int[] rgb) {
        int r = rgb[0]&0xFF;
        int g = rgb[1]&0xFF;
        int b = rgb[2]&0xFF;
//        return Color.rgb(r, g, b);
        return (0xff<<24)|(r<<16)|((g<<8)|b);
    }
}