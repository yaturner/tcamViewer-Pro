package com.darcangel.tcamViewer.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.databinding.BaseObservable;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.constants.Constants;
import com.darcangel.tcamViewer.container.Settings;

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

import timber.log.Timber;


public class CameraUtils extends BaseObservable implements Parcelable {
    private boolean AGC;
    private boolean shutdown;
    private int emissivity;
    private int TLinearEnabled;
    private int TLinearResolution; // 0 = 0.1, 1 = 0.01
    private int spotmeterMean;
    private Rect spotmeterLocation;
    private boolean shutterLockout;
    private boolean isManualRange;
    private int FFCState;
    private int FFCDesired;
    private int gainMode;
    private int autoGainMode;

    private int[] pixels;
    private int[] imageData;
    private byte[] imageBytes;
    private int imageLen;
    private int diff;
    private int maxTemperature;
    private int minTemperature;
    private int manualMaxTemperature;
    private int manualMinTemperature;
    private JSONObject response;
    private boolean unitsCelsius;

    private Settings settings;

    private final static int offsetA = 0;
    private final static int offsetB = 80;
    private final static int offsetC = 160;

    private static final Pattern IP_PATTERN = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    private static final SimpleDateFormat simpleDateFormatFolder = new SimpleDateFormat("yy_MM_dd");
    private static final SimpleDateFormat simpleDateFormatFile = new SimpleDateFormat("HH_mm_ss");

    //default constructor
    public CameraUtils() {
        //observe any changes from settings for manual range and/or units
        settings = MainActivity.getInstance().getSettings();
        settings.getManualRange().observeForever(v -> {
            isManualRange = v;
        });
        settings.getManualRangeMin().observeForever(v -> {
            manualMinTemperature = convertToRadiometric(v);
        });
        settings.getManualRangeMax().observeForever(v -> {
            manualMaxTemperature = convertToRadiometric(v);
        });
        settings.getUnitsC().observeForever(v -> {
            unitsCelsius = v;
        });
        Timber.d("CameraUtils default constructor");
    }

    protected CameraUtils(Parcel in) {
        AGC = in.readByte() != 0;
        shutdown = in.readByte() != 0;
        emissivity = in.readInt();
        TLinearEnabled = in.readInt();
        TLinearResolution = in.readInt();
        spotmeterMean = in.readInt();
        spotmeterLocation = in.readParcelable(Rect.class.getClassLoader());
        shutterLockout = in.readByte() != 0;
        isManualRange = in.readByte() != 0;
        FFCState = in.readInt();
        FFCDesired = in.readInt();
        gainMode = in.readInt();
        autoGainMode = in.readInt();
        pixels = in.createIntArray();
        imageData = in.createIntArray();
        diff = in.readInt();
        maxTemperature = in.readInt();
        minTemperature = in.readInt();
        manualMaxTemperature = in.readInt();
        manualMinTemperature = in.readInt();
    }

    public static final Creator<CameraUtils> CREATOR = new Creator<CameraUtils>() {
        @Override
        public CameraUtils createFromParcel(Parcel in) {
            return new CameraUtils(in);
        }

        @Override
        public CameraUtils[] newArray(int size) {
            return new CameraUtils[size];
        }
    };

    public Bitmap processImageResponse(JSONObject response, int[][] palette,
                                       boolean celsius)
            throws JSONException {
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
        imageBytes = Base64.getDecoder().decode(radiometricString.getBytes());

        imageLen = imageBytes.length;
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
        }

        setTemperatureRange();

        if(AGC) {
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = rgbToPixel(palette[imageData[i]]);
            }
        } else {
            int min, max;
            Pair<Integer, Integer> temps = getRadiometricTemperatures();
            min = temps.first;
            max = temps.second;
            diff = max - min;
            for (int i = 0; i < imageData.length; i++) {
                int v = imageData[i];
                int value;
                if (isManualRange) {
                    if(v < min) {
                        v = min;
                    } else if(v > max) {
                        v = max;
                    }
                    value = ((v - min) * 255) / diff;
                } else {
                    value = ((v - minTemperature) * 255) / diff;
                }

                pixels[i] = rgbToPixel(palette[Math.min(Math.max(value, 0), 255)]);
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

    @NonNull
    private int[] parseTelemetryData(@NonNull String telemetryString) {
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
        //double the size. half black for the arrow
        int width2 = 2*width;
        int[] pixels = new int[width2 * 256];
        for (int row = 0; row < 256; row++) {
            for (int col = 0; col < width2; col++) {
                if (col < width) {
                    //padding for arrow
                    pixels[row * width2 + col] = 0x00;
                } else {
                    pixels[row * width2 + col] = rgbToPixel(palette[255 - row]);
                }
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(pixels, width2, Constants.COLORBAR_HEIGHT, Bitmap.Config.ARGB_8888);
        return drawHotspotArrow(bitmap);
    }

    public Bitmap drawHotspotArrow(Bitmap colorBar) {
        //if there is no camera image, no arrow
        if(minTemperature == 0 && maxTemperature == 0) {
            return colorBar;
        }

        float offset = (float)Constants.COLORBAR_HEIGHT -
                ((((float)(spotmeterMean-minTemperature))/(float)diff) * (float)Constants.COLORBAR_HEIGHT);

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
        path.offset(20f, offset);
        canvas.drawPath(path, paint);

        return tempBitmap;
    }

    /**
     *
     * @param palette
     * @param width
     * @return bitmap of histogram
     *
     * the indices for the colors are all 255-value to match the color bar
     */
    public Bitmap createHistogram(int[][] palette, int width) {
        int[] bin = new int[256];
        int maxBinCount = -1;
        Paint black = new Paint();
        Paint paint = new Paint();
        Rect fill = new Rect(0, 0, width, Constants.COLORBAR_HEIGHT);

        black.setColor(0xff202020);
        black.setStyle(Paint.Style.FILL);

        paint.setColor(0xffffffff);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.0f);

        try {
            for (int index = 0; index < imageData.length; index++) {
                //if Manual Range was specified, only include values min < v < max
                int v = imageData[index];
                int b = -1;
                Pair<Integer, Integer> temps = getRadiometricTemperatures();
                int min = temps.first;
                int max = temps.second;
                if (isManualRange) {
                    if (min < v && v < max) {
                        b = Math.min(Math.max(((v - min) * 255 / diff), 0), 255);
                    } else {
                        b = -1;
                    }
                } else {
                    b = Math.min(Math.max(((v - min) * 255 / diff), 0), 255);
                }
                if (b >= 0) {
                    bin[255 - b] = bin[255 - b] + 1;
                    maxBinCount = Math.max(bin[255 - b], maxBinCount);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        //add a 5% margin
        float scale = (float)width/(float)(maxBinCount + maxBinCount/20);

        Bitmap image = Bitmap.createBitmap(width, Constants.COLORBAR_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawRect(fill, black);

        for(int index = 0; index < 256; index++) {
            paint.setColor(rgbToPixel(palette[255 - index]));
            canvas.drawLine(0, (float)index, (float)bin[index]*scale, (float)index, paint);
        }

        return image;
    }

    /**
     * setTemperatureRange
     *
     * sets the min & max temperature in radiometric
     *
     * there is an edge condition where if Manual Range is set in Prefs but there is no image yet,
     * the radiometric data for the min/max is useless, so we always recalculate it from settings
     */
    private void setTemperatureRange() {
        if(isManualRange()) {
            minTemperature = manualMinTemperature = convertToRadiometric(settings.getManualRangeMin().getValue());
            maxTemperature = manualMaxTemperature = convertToRadiometric(settings.getManualRangeMax().getValue());
        } else {
            for (int i = 0, j = 0; i < imageLen; i = i + 2, j++) {
                minTemperature = Math.min(imageData[j], minTemperature);
                maxTemperature = Math.max(imageData[j], maxTemperature);
            }
        }
    }

    public Bitmap remapCurrentImage(int[][] palette) {
        if(AGC) {
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = rgbToPixel(palette[imageData[i]]);
            }
        } else {
            setTemperatureRange();
            for (int i = 0; i < imageData.length; i++) {
                pixels[i] = rgbToPixel(palette[Math.min(((imageData[i] - minTemperature) * 255) / diff, 255)]);
            }
        }

        return Bitmap.createBitmap(pixels, Constants.IMAGE_WIDTH, Constants.IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
    }

    public static Boolean isValidIPAddress(String address) {
        return IP_PATTERN.matcher(address).matches();
    }

    public Bitmap drawHotspot(boolean draw) {
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
        if(draw) {
            tempCanvas.drawRect(new Rect(imageX - 2, imageY - 2, imageX + 2, imageY + 2), paintWhite);
            tempCanvas.drawRect(new Rect(imageX - 3, imageY - 3, imageX + 3, imageY + 3), paintBlack);
        }

        return tempBitmap;
    }

    public boolean isUnitsCelsius() {
        return unitsCelsius;
    }

    public boolean isManualRange() {
        return isManualRange;
    }

//    public void setManualRange(boolean manualRange) {
//        isManualRange = manualRange;
//    }

    /**
     * getRadiometricTemperatures
     *
     * @return min, max temperatures in radiometric values
     */
    public Pair<Integer, Integer> getRadiometricTemperatures() {
        if(isManualRange) {
            return new Pair<>(manualMinTemperature, manualMaxTemperature);
        } else {
            return new Pair<>(minTemperature, maxTemperature);
        }
    }

    public Pair<Float, Float> getTemperatures() {
        if(isManualRange) {
            return new Pair<>(convertToDisplayUnits(manualMinTemperature), convertToDisplayUnits(manualMaxTemperature));
        } else {
            return new Pair<>(convertToDisplayUnits(minTemperature), convertToDisplayUnits(maxTemperature));
        }
    }

    public float getMeanTemperatureAtSpotmeter() {
        Rect spotmeter = getSpotmeterLocation();
        int topLeft = imageData[spotmeter.top * Constants.IMAGE_WIDTH + spotmeter.left];
        int topRight = imageData[spotmeter.top * Constants.IMAGE_WIDTH + spotmeter.left + 1];
        int bottomLeft = imageData[spotmeter.bottom * Constants.IMAGE_WIDTH + spotmeter.right];
        int bottomRight = imageData[spotmeter.bottom * Constants.IMAGE_WIDTH + spotmeter.right +1];
        return convertToDisplayUnits((topLeft + topRight + bottomLeft + bottomRight)/4);
    }

    //Convert radiometric data to Celsius/Fahrenheit
    private float convertToDisplayUnits(Integer value) {
        float scale = TLinearResolution == 0 ? 0.1f : 0.01f;
        if(isUnitsCelsius()) {
            return scale * (float)value - 273.15f;
        } else {
            return ((scale * (float)value) - 273.15f)*(9.0f/5.0f) + 32.0f;
        }
    }

    //Convert Celsius/Fahrenheit to radiometric data
    public int convertToRadiometric( float value) {
        float scale = TLinearResolution == 0 ? 10f : 100f;
        if(isUnitsCelsius()) {
            return Math.round((value + 273.15f) * scale);
        } else {
            float c = (value - 32f) * .5556f;
            return Math.round((c + 273.15f) * scale);
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

    public boolean isAGC() {
        return AGC;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if(response != null) {
            dest.writeString(response.toString());
        }
    }
}