package com.darcangel.acam.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Environment;

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.constants.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.regex.Pattern;


public class CameraUtils {
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

    private int[] pixels;
    private int[] imageData;
    private int diff;
    int maxTemperature;
    int minTemperature;
    private JSONObject response;

    private int offsetA = 0;
    private int offsetB = 80;
    private int offsetC = 160;

    private static final Pattern IP_PATTERN = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    private static final SimpleDateFormat simpleDateFormatFolder = new SimpleDateFormat("yy_MM_dd");
    private static final SimpleDateFormat simpleDateFormatFile = new SimpleDateFormat("HH_mm_ss");


    public Bitmap processImageResponse(JSONObject response, int[][] palette) throws JSONException {
        this.response = response;

        if(pixels != null) {
            pixels = null;
        }
        if(imageData != null) {
            imageData = null;
        }
        maxTemperature = Integer.MIN_VALUE;
        minTemperature = Integer.MAX_VALUE;

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
        Integer x1 = telemetryData[offsetC+55]&0xffff;
        Integer y1 = telemetryData[offsetC+54]&0xffff;
        Integer x2 = telemetryData[offsetC+57]&0xffff;
        Integer y2 = telemetryData[offsetC+56]&0xffff;
        spotmeterLocation = new Rect(x1, y1, x2, y2);

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
                pixels[i] = rgbToPixel(palette[Math.min(((imageData[i] - minTemperature) * 255 / diff), 255)]);
            }
        }
        Bitmap result = Bitmap.createBitmap(pixels, Constants.IMAGE_WIDTH, Constants.IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
        return result;
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
        int width2 = width*2;
        int[] pixels = new int[width2 * 256];
        for (int row = 0; row < 256; row++) {
            for (int col = 0; col < width2; col++) {
                if (col < width) {
                    pixels[row * width2 + col] = 0;
                } else {
                    pixels[row * width2 + col] = rgbToPixel(palette[255 - row]);
                }
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(pixels, width2, Constants.COLORBAR_HEIGHT, Bitmap.Config.ARGB_8888);
        return drawHotspotArrow(bitmap, spotmeterMean);
    }

    public Bitmap drawHotspotArrow(Bitmap colorBar,  float temperature) {
        // create and draw triangles
        // use a Path object to store the 3 line segments
        // use .offset to draw in many locations
        // note: this triangle is not centered at 0,0

        //if there is no camera image, no arrow
        if(minTemperature == 0 && maxTemperature == 0) {
            return colorBar;
        }
        float offset = (((float)(spotmeterMean-minTemperature))/(float)diff) * (float)Constants.COLORBAR_HEIGHT;

        Paint paint = new Paint();
        paint.setColor(0xffffffff);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1);
        Path path = new Path();

        //Create a new image bitmap and attach a brand new canvas to it
        Bitmap tempBitmap = Bitmap.createBitmap(colorBar.getWidth(), colorBar.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(tempBitmap);

        //Draw the image bitmap into the canvas
        canvas.drawBitmap(colorBar, 0, 0, null);

        path.moveTo(0, -10);
        path.lineTo(10, 0);
        path.lineTo(0, 10);
        path.close();
//        path.offset(20, 10);
        path.offset(20f, offset);
        canvas.drawPath(path, paint);

        return tempBitmap;
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

    public Bitmap drawHotspot() {
        Paint paintWhite = new Paint();
        Paint paintBlack = new Paint();
        paintWhite.setColor(0xffffffff);
        paintWhite.setStyle(Paint.Style.STROKE);
        paintWhite.setStrokeWidth(1f);
        paintBlack.setColor(0xff000000);
        paintBlack.setStyle(Paint.Style.STROKE);
        paintBlack.setStrokeWidth(1f);

        int imageX = spotmeterLocation.left;
        int imageY = spotmeterLocation.top;
        //Create a new image bitmap and attach a brand new canvas to it
        Bitmap cameraBitmap = Bitmap.createBitmap(pixels, Constants.IMAGE_WIDTH, Constants.IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
        Bitmap tempBitmap = Bitmap.createBitmap(cameraBitmap.getWidth(), cameraBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas tempCanvas = new Canvas(tempBitmap);

        //Draw the image bitmap into the canvas
        tempCanvas.drawBitmap(cameraBitmap, 0, 0, null);

        tempCanvas.drawRect(new Rect(imageX-2, imageY-2, imageX+2, imageY+2), paintWhite);
        tempCanvas.drawRect(new Rect(imageX-3, imageY-3, imageX+3, imageY+3), paintBlack);

        return tempBitmap;
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

    public Boolean saveTjsn() throws IOException{

        File rootDir = MainActivity.getInstance().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String file = generateNewFilename() + ".tjsn";
        File path = new File(rootDir + "/" + generateNewPath());
        if(!path.exists()) {
            path.mkdir();
        }
        File tjsn = new File(path, file);
        FileOutputStream fileOutputStream = new FileOutputStream(tjsn);
        if(!tjsn.exists()) {
            tjsn.createNewFile();
        }
        fileOutputStream.write(response.toString().getBytes(StandardCharsets.UTF_8));
        fileOutputStream.flush();
        fileOutputStream.close();


        return true;
    }

    public static String generateNewFilename() {
        Date now = new Date();
        return new String("img_" + simpleDateFormatFile.format(now));
    }

    public static String generateNewPath() {
        Date now = new Date();
        return simpleDateFormatFolder.format(now);
    }

    public void saveBitmapToFile(Bitmap bitmap, File file) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(file);
        if (outputStream != null && bitmap != null) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.close();
        }
    }

    public String readTjsnFile(String path) {
        String json = new String();
        String line;
        try {
            BufferedReader bufferedReader = new BufferedReader(
                    new FileReader(new File(path)));
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
        return json;
    }

    public Rect getSpotmeterLocation() {
        return spotmeterLocation;
    }

    public void setSpotmeterLocation(Rect rect) {
        spotmeterLocation = rect;
    }
}