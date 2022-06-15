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
    private int TLinear;
    private int spotmeterMean;
    private Pair<Integer, Integer> spotLoc;
    private boolean sutterLockout;
    private int FFCState;
    private int FFCDesired;

    private int[] pixels;
    private int[] imageData;
    private int[] imageNorm;
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
        if(imageNorm != null) {
            imageNorm = null;
        }

        String radiometricString = response.getString("radiometric");
        String telemetryString = response.getString("telemetry");
        byte[] imageBytes = Base64.getDecoder().decode(radiometricString.getBytes());

        int imageLen = imageBytes.length;
        imageData = new int[imageLen / 2];
        imageNorm = new int[imageLen / 2];
        pixels = new int[Constants.IMAGE_WIDTH * Constants.IMAGE_HEIGHT];
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
            minTemperature = Math.min(imageData[j], minTemperature);
            maxTemperature = Math.max(imageData[j], maxTemperature);
        }
        if(AGC) {
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = rgbToPixel(palette[imageData[i]]);
            }
        } else {
            diff = maxTemperature - minTemperature;
            for (int i = 0; i < imageNorm.length; i++) {
                imageNorm[i] = Math.min(((imageData[i] - minTemperature) * 255) / diff, 255);
            }
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = rgbToPixel(palette[imageNorm[i]]);
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
            for (int i = 0; i < imageNorm.length; i++) {
                imageNorm[i] = Math.min(((imageData[i] - minTemperature) * 255) / diff, 255);
            }
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = rgbToPixel(palette[imageNorm[i]]);
            }
        }
        return Bitmap.createBitmap(pixels, Constants.IMAGE_WIDTH, Constants.IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
    }

    public static Boolean isValidIPAddress(String address) {
        return IP_PATTERN.matcher(address).matches();
    }

    public void DrawHotspot(ImageView imageView, int x, int y) {
        Paint paint = new Paint();
        paint.setColor(0xffff0000);

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

    public int getMaxTemperature() {
        return maxTemperature;
    }

    public int getMinTemperature() {
        return minTemperature;
    }
}