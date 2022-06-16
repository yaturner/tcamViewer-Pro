package com.darcangel.acam.utils;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.util.Pair;
import android.widget.ImageView;

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.constants.Constants;
import com.darcangel.acam.pallete.Rainbow;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.regex.Pattern;


public class CameraUtils {
    private boolean AGC;
    private boolean shutdown;
    private int emissivity;
    private int TLinearEnabled;
    private int TLinearResolution; // 0 = 0.1, 1 = 0.01
    private int spotmeterMean;
    private Rect spotLoc;
    private boolean sutterLockout;
    private int FFCState;
    private int FFCDesired;
    private int gainMode;
    private int autoGainMode;

    private int[] pixels;
    private int[] imageData;
    private int diff;
    int maxTemperature = Integer.MIN_VALUE;
    int minTemperature = Integer.MAX_VALUE;

    private int offsetA = 0;
    private int offsetB = 80;
    private int offsetC = 160;

    private static final Pattern IP_PATTERN = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");



    public Bitmap processImageResponse(JSONObject response, int[][] palette) throws JSONException {
        if(pixels != null) {
            pixels = null;
        }
        if(imageData != null) {
            imageData = null;
        }
        String radiometricString = response.getString("radiometric");
        String telemetryString = response.getString("telemetry");
        byte[] imageBytes = Base64.getDecoder().decode(radiometricString.getBytes());

        int imageLen = imageBytes.length;
        imageData = new int[imageLen / 2];
        pixels = new int[Constants.IMAGE_WIDTH * Constants.IMAGE_HEIGHT];
        int[] telemetryData;

        telemetryData = parseTelemetryData(telemetryString);
        int status = ((telemetryData[4] & 0xffff) << 16) | (telemetryData[3] & 0xffff);
        AGC = ((status & Constants.TELEMETRY_MASK_AGC) == Constants.TELEMETRY_MASK_AGC);
        shutdown = ((status & Constants.TELEMETRY_MASK_SHUTDOWN) == Constants.TELEMETRY_MASK_SHUTDOWN);
        emissivity = telemetryData[offsetB + 19];
        gainMode = telemetryData[offsetC + 5];
        autoGainMode = telemetryData[offsetC + 6];
        TLinearEnabled = telemetryData[offsetC + 48];
        TLinearResolution = telemetryData[offsetC + 49];
        spotmeterMean = telemetryData[offsetC + 50];
        Integer x1 = telemetryData[offsetC+22]&0xffff;
        Integer y1 = telemetryData[offsetC+23]&0xffff;
        Integer x2 = telemetryData[offsetC+24]&0xffff;
        Integer y2 = telemetryData[offsetC+25]&0xffff;
        spotLoc = new Rect(x1, y1, x2, y2);

        for (int i = 0, j = 0; i < imageLen; i = i + 2, j++) {
            imageData[j] = ((imageBytes[i + 1] & 0xff) << 8) | (imageBytes[i] & 0xff);
            minTemperature = Math.min(imageData[j], minTemperature);
            maxTemperature = Math.max(imageData[j], maxTemperature);
        }
        if(AGC) {
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = rgbToPixel(palette[imageData[i]]);
            }
        } else {
            diff = maxTemperature - minTemperature;
            for (int i = 0; i < imageData.length; i++) {
                pixels[i] = rgbToPixel(palette[Math.min(((imageData[i] - minTemperature) * 255) / diff, 255)]);
            }
        }
        return Bitmap.createBitmap(pixels, Constants.IMAGE_WIDTH, Constants.IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
    }


    private int rgbToPixel(int[] rgb) {
        int r = rgb[0] & 0xFF;
        int g = rgb[1] & 0xFF;
        int b = rgb[2] & 0xFF;
        return (0xff << 24) | (r << 16) | ((g << 8) | b);
    }

    private int[] parseTelemetryData(String telemetryString) {
        byte[] telemetryBytes = Base64.getDecoder().decode(telemetryString.getBytes());
        int[] telemetryData;

        int len = telemetryBytes.length;
        telemetryData = new int[len / 2];

        for (int i = 0, j = 0; i < len; i = i + 2, j++) {
            telemetryData[j] = ((telemetryBytes[i + 1] & 0xff) << 8) | (telemetryBytes[i] & 0xff);
        }
        return telemetryData;
    }

    public Bitmap createColorBar(int[][] palette, int width) {
        int[] pixels = new int[width*256];
        for(int row = 0; row < 256; row++) {
            for(int col = 0; col < width; col++) {
                pixels[row*width+col] = rgbToPixel(palette[255-row]);
            }
        }
        return Bitmap.createBitmap(pixels, width, 256, Bitmap.Config.ARGB_8888);
    }

    public Bitmap remapCurrentImage(int[][] palette) {
        if(AGC) {
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = rgbToPixel(palette[imageData[i]]);
            }
        } else {
            diff = maxTemperature - minTemperature;
            for (int i = 0; i < imageData.length; i++) {
                pixels[i] = rgbToPixel(palette[Math.min(((imageData[i] - minTemperature) * 255) / diff, 255)]);
            }
        }
        return Bitmap.createBitmap(pixels, Constants.IMAGE_WIDTH, Constants.IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
    }

    public static Boolean isValidIPAddress(String address) {
        return IP_PATTERN.matcher(address).matches();
    }

    public void DrawHotspot(ImageView imageView, int x, int y) {
        Paint paint = new Paint();
        paint.setColor(0xffffffff);

        //Create a new image bitmap and attach a brand new canvas to it
        Bitmap cameraBitmap = Bitmap.createBitmap(pixels, Constants.IMAGE_WIDTH, Constants.IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
        Bitmap tempBitmap = Bitmap.createBitmap(cameraBitmap.getWidth(), cameraBitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas tempCanvas = new Canvas(tempBitmap);

        //Draw the image bitmap into the canvas
        tempCanvas.drawBitmap(cameraBitmap, 0, 0, null);

        //Draw everything else you want into the canvas, in this example a rectangle with rounded edges
        tempCanvas.drawRect(new Rect(x-2, y-2, x+2, y+2), paint);

        //Attach the canvas to the ImageView
        imageView.setImageBitmap(tempBitmap);
    }

    public float getMaxTemperature(boolean celsius) {
        return scaleTemperature(celsius, maxTemperature);
    }

    public float getMinTemperature(boolean celsius) {
        return scaleTemperature(celsius, minTemperature);
    }

    private float scaleTemperature(boolean celsius, float value) {
        float scale = TLinearResolution == 0 ? 0.1f : 0.01f;
        if(celsius) {
            return scale * value - 273.15f;
        } else {
            return (((scale * value - 273.15f)*9.0f)/5.0f) + 32.0f;
        }
    }
}