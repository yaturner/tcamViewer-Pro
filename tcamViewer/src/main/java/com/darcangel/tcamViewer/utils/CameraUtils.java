package com.darcangel.tcamViewer.utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.databinding.BaseObservable;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;
import com.darcangel.tcamViewer.constants.Constants;
import com.darcangel.tcamViewer.model.ImageDto;
import com.darcangel.tcamViewer.model.Settings;
import com.darcangel.tcamViewer.ui.camera.CameraViewModel;

import org.json.JSONException;

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


public class CameraUtils extends BaseObservable {

    private int[] pixels;
    private int[] imageData;
    private byte[] imageBytes;
    private int imageLen;

    private Settings settings;
    private CameraViewModel cameraViewModel;

    private final static int offsetA = 0;
    private final static int offsetB = 80;
    private final static int offsetC = 160;

    private static final Pattern IP_PATTERN = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    private static final SimpleDateFormat simpleDateFormatFolder = new SimpleDateFormat("yy_MM_dd");
    private static final SimpleDateFormat simpleDateFormatFile = new SimpleDateFormat("HH_mm_ss");

    //default constructor
    public CameraUtils() {
    }

    public void processImageResponse(ImageDto imageDto) throws JSONException {
        int[][] palette = imageDto.getPalette();
        int diff = 0;

        if(pixels != null) {
            pixels = null;
        }
        if(imageData != null) {
            imageData = null;
        }
        imageDto.setMaxTemperature(Integer.MIN_VALUE);
        imageDto.setMinTemperature(Integer.MAX_VALUE);

        String radiometricString = imageDto.getJsonObject().getString("radiometric");
        String telemetryString = imageDto.getJsonObject().getString("telemetry");
        imageBytes = Base64.getDecoder().decode(radiometricString.getBytes());

        imageLen = imageBytes.length;
        imageData = new int[imageLen / 2];
        pixels = new int[Constants.IMAGE_WIDTH * Constants.IMAGE_HEIGHT];
        int[] telemetryData;

        telemetryData = parseTelemetryData(telemetryString);
        int status = ((telemetryData[4] & 0xffff) << 16) | (telemetryData[3] & 0xffff);
        imageDto.setAGC((status & Constants.TELEMETRY_MASK_AGC) == Constants.TELEMETRY_MASK_AGC);
        imageDto.setShutdown((status & Constants.TELEMETRY_MASK_SHUTDOWN) == Constants.TELEMETRY_MASK_SHUTDOWN);
        imageDto.setEmissivity(telemetryData[offsetB + 19]);
        imageDto.setGainMode(telemetryData[offsetC + 5]);
        imageDto.setAutoGainMode(telemetryData[offsetC + 6]);
        imageDto.setTLinearEnabled(telemetryData[offsetC + 48]);
        imageDto.setTLinearResolution(telemetryData[offsetC + 49]);
        imageDto.setSpotmeterMean(telemetryData[offsetC + 50]);
        Integer x1 = telemetryData[offsetC+55]&0xffff;
        Integer y1 = telemetryData[offsetC+54]&0xffff;
        Integer x2 = telemetryData[offsetC+57]&0xffff;
        Integer y2 = telemetryData[offsetC+56]&0xffff;
        imageDto.setSpotmeterLocation(new Rect(x1, y1, x2, y2));

        for (int i = 0, j = 0; i < imageLen; i = i + 2, j++) {
            imageData[j] = ((imageBytes[i + 1] & 0xff) << 8) | (imageBytes[i] & 0xff);
        }

        setTemperatureRange(imageDto);

        if(imageDto.isAGC()) {
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = rgbToPixel(palette[imageData[i]]);
            }
        } else {
            int min, max;
            Pair<Integer, Integer> temps = getRadiometricTemperatures(imageDto);
            min = temps.first;
            max = temps.second;
            diff = max - min;
            for (int i = 0; i < imageData.length; i++) {
                int v = imageData[i];
                int value;
                if (isManualRange()) {
                    if(v < min) {
                        v = min;
                    } else if(v > max) {
                        v = max;
                    }
                    value = ((v - min) * 255) / diff;
                } else {
                    value = ((v - imageDto.getMinTemperature()) * 255) / diff;
                }

                pixels[i] = rgbToPixel(palette[Math.min(Math.max(value, 0), 255)]);
            }
        }
        imageDto.setBitmap(Bitmap.createBitmap(pixels, Constants.IMAGE_WIDTH, Constants.IMAGE_HEIGHT, Bitmap.Config.ARGB_8888));
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

    public Bitmap createColorBar(ImageDto imageDto, int width) {
        int[][] palette = imageDto.getPalette();
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
        return drawHotspotArrow(imageDto, bitmap);
    }

    public Bitmap drawHotspotArrow(ImageDto imageDto, Bitmap colorBar) {
        //if there is no camera image, no arrow
        if(imageDto.getMinTemperature() == 0 && imageDto.getMaxTemperature() == 0) {
            return colorBar;
        }
        int min, max, diff;
        Pair<Integer, Integer> temps = getRadiometricTemperatures(imageDto);
        min = temps.first;
        max = temps.second;
        diff = max - min;

        float offset = (float)Constants.COLORBAR_HEIGHT -
                ((((float)(imageDto.getSpotmeterMean()-imageDto.getMinTemperature()))/(float)diff) * (float)Constants.COLORBAR_HEIGHT);

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
     * @param imageDto
     * @return bitmap of histogram
     *
     * the indices for the colors are all 255-value to match the color bar
     */
    public Bitmap createHistogram(ImageDto imageDto) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        int width = Math.round(MainActivity.getInstance().getResources().getDimension(R.dimen.histogram_width));
        int[][] palette = imageDto.getPalette();
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

        int b = -1, d = -1, v = -1;
        Timber.d("\\\\ManualRange\\\\createHistogram\\\\ isManualRange() = %s", (isManualRange()?"true":"false"));

        try {
            Pair<Integer, Integer> temps = getRadiometricTemperatures(imageDto);
            int min = temps.first;
            int max = temps.second;
            int diff = max - min;
            for (int index = 0; index < imageData.length; index++) {
                if(!imageDto.isAGC()) {
                    //if Manual Range was specified, only include values min < v < max
                    v = imageData[index];
                    if (isManualRange()) {
                        if (min < v && v < max) {
                            d = Math.round(((float)(v-min)/(float)diff) * 255f);
                            b = Math.min(Math.max(d, 0), 255);
                        } else {
                            b = -1;
                        }
                    } else {
                        d = Math.round(((float)(v-min)/(float)diff) * 255f);
                        b = Math.min(Math.max(d, 0), 255);
                    }
                } else {
                    b = imageData[index];
                }
                if(!isManualRange()) {
                    Timber.d("\\\\CreateHistogram\\\\ b = %d, v = %d, d = %d, min = %d", b, v, d, min);
                }
                if (b >= 0) {
                    bin[255 - b] = bin[255 - b] + 1;
                    maxBinCount = Math.max(bin[255 - b], maxBinCount);
                } else {
                    if(!isManualRange()) {
                        Timber.d("\\\\CreateHistogram\\\\ b = %d, v = %d, d = %d", b, v, d);
                    }
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
            if (bin[index] > 0) {
                paint.setColor(rgbToPixel(palette[255 - index]));
                canvas.drawLine(0, (float) index, (float) bin[index] * scale, (float) index, paint);
            }
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
    private void setTemperatureRange(ImageDto imageDto) {
        Timber.d("\\\\ManualRange\\\\setTemperatureRange\\\\ isManualRange() = %s", (isManualRange()?"true":"false"));
//        if(isManualRange()) {
//            imageDto.setMinTemperature(convertToRadiometric(imageDto, getManualRangeMin()));
//            imageDto.setMaxTemperature(convertToRadiometric(imageDto, getManualRangeMax()));
//        } else {
            int minTemperature = Integer.MAX_VALUE;
            int maxTemperature = Integer.MIN_VALUE;
            for (int i = 0; i < imageData.length; i++) {
                minTemperature = Math.min(imageData[i], minTemperature);
                maxTemperature = Math.max(imageData[i], maxTemperature);
            }
            imageDto.setMinTemperature(minTemperature);
            imageDto.setMaxTemperature(maxTemperature);
//        }
    }

    public void remapImage(ImageDto imageDto) {
        int[][] palette = imageDto.getPalette();
        int diff;
        if(imageDto.isAGC()) {
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = rgbToPixel(palette[imageData[i]]);
            }
        } else {
            int v, b, d;
            Pair<Integer, Integer> temps = getRadiometricTemperatures(imageDto);
            int min = temps.first;
            int max = temps.second;
            diff = max - min;
            for (int index = 0; index < imageData.length; index++) {
                v = imageData[index];
                if (isManualRange()) {
                    if (min < v && v < max) {
                        d = Math.round(((float)(v-min)/(float)diff) * 255f);
                        b = Math.min(Math.max(d, 0), 255);
                    } else {
                        b = -1;
                    }
                } else {
                    d = Math.round(((float)(v-min)/(float)diff) * 255f);
                    b = Math.min(Math.max(d, 0), 255);
                }
                if(b > 0) {
                    pixels[index] = rgbToPixel(palette[b]);
                } else {
                    pixels[index] = 0;
                }
            }
        }

        imageDto.setBitmap(Bitmap.createBitmap(pixels, Constants.IMAGE_WIDTH, Constants.IMAGE_HEIGHT, Bitmap.Config.ARGB_8888));
    }

    public static Boolean isValidIPAddress(String address) {
        return IP_PATTERN.matcher(address).matches();
    }

    public Bitmap drawHotspot(ImageDto imageDto) {
        Paint paintWhite = new Paint();
        Paint paintBlack = new Paint();
        paintWhite.setColor(0xffffffff);
        paintWhite.setStyle(Paint.Style.STROKE);
        paintWhite.setStrokeWidth(1f);
        paintBlack.setColor(0xff000000);
        paintBlack.setStyle(Paint.Style.STROKE);
        paintBlack.setStrokeWidth(1f);

        int imageX = imageDto.getSpotmeterLocation().left;
        int imageY = imageDto.getSpotmeterLocation().top;
        //Create a new image bitmap and attach a brand new canvas to it
        Bitmap cameraBitmap = imageDto.getBitmap();
        Bitmap tempBitmap = Bitmap.createBitmap(cameraBitmap.getWidth(), cameraBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas tempCanvas = new Canvas(tempBitmap);

        //Draw the image bitmap into the canvas
        tempCanvas.drawBitmap(cameraBitmap, 0, 0, null);
        tempCanvas.drawRect(new Rect(imageX - 2, imageY - 2, imageX + 2, imageY + 2), paintWhite);
        tempCanvas.drawRect(new Rect(imageX - 3, imageY - 3, imageX + 3, imageY + 3), paintBlack);

        return tempBitmap;
    }

    public void rotateColormap(ImageDto imageDto) {
        String pal = imageDto.getPaletteName();
        MainActivity mainActivity = MainActivity.getInstance();
        String[] paletteNames = mainActivity.getPaletteFactory().getPaletteNames();
        for (int index = 0; index < paletteNames.length; index++) {
            if (pal.equalsIgnoreCase(paletteNames[index])) {
                if (index == paletteNames.length - 1) {
                    index = -1;
                }
                String paletteName = paletteNames[index + 1];
                if(settings == null) {
                    settings = MainActivity.getInstance().getSettings();
                }
                settings.setPalette(paletteName);
                settings.persist();
                imageDto.setPaletteName(paletteName);
                imageDto.setPalette(mainActivity.getPaletteFactory().getPaletteByName(paletteName));
                if (imageDto.getBitmap() != null) {
                    imageDto.remapImage();
                }
                break;
            }
        }
    }


    public boolean isUnitsCelsius() {
        return cameraViewModel.isUnitsCelsius();
    }

    private boolean isManualRange() {
        if(cameraViewModel == null) {
            cameraViewModel = MainActivity.getInstance().getCameraViewModel();
        }
        return cameraViewModel.isManualRange();
    }

    private float getManualRangeMin() {
        if(cameraViewModel == null) {
            cameraViewModel = MainActivity.getInstance().getCameraViewModel();
        }
        return cameraViewModel.getManualMinTemperature();
    }

    private float getManualRangeMax() {
        if(cameraViewModel == null) {
            cameraViewModel = MainActivity.getInstance().getCameraViewModel();
        }
        return cameraViewModel.getManualMaxTemperature();
    }

    /**
     * getRadiometricTemperatures
     *
     * @return min, max temperatures in radiometric values
     */
    public Pair<Integer, Integer> getRadiometricTemperatures(ImageDto imageDto) {
        Timber.d("\\\\ManualRange\\\\getRadiometricTemperatures\\\\ isManualRange() = %s", (isManualRange()?"true":"false"));
        if(isManualRange()) {
            return new Pair<>(convertToRadiometric(imageDto, cameraViewModel.getManualMinTemperature()),
                    convertToRadiometric(imageDto, cameraViewModel.getManualMaxTemperature()));
        } else {
            return new Pair<>(imageDto.getMinTemperature(), imageDto.getMaxTemperature());
        }
    }

    public Pair<Float, Float> getTemperatures(ImageDto imageDto) {
        Pair<Integer, Integer> temps = getRadiometricTemperatures(imageDto);
        return new Pair<>(convertToDisplayUnits(imageDto, temps.first), convertToDisplayUnits(imageDto, temps.second));
    }

    public float getMeanTemperatureAtSpotmeter(ImageDto imageDto) {
        Rect spotmeter = imageDto.getSpotmeterLocation();
        int topLeft = imageData[spotmeter.top * Constants.IMAGE_WIDTH + spotmeter.left];
        int topRight = imageData[spotmeter.top * Constants.IMAGE_WIDTH + spotmeter.left + 1];
        int bottomLeft = imageData[spotmeter.bottom * Constants.IMAGE_WIDTH + spotmeter.right];
        int bottomRight = imageData[spotmeter.bottom * Constants.IMAGE_WIDTH + spotmeter.right +1];
        return convertToDisplayUnits(imageDto, (topLeft + topRight + bottomLeft + bottomRight)/4);
    }

    //Convert radiometric data to Celsius/Fahrenheit
    private float convertToDisplayUnits(ImageDto imageDto, Integer value) {
        float scale = imageDto.getTLinearResolution() == 0 ? 0.1f : 0.01f;
        if(isUnitsCelsius()) {
            return scale * (float)value - 273.15f;
        } else {
            return ((scale * (float)value) - 273.15f)*(9.0f/5.0f) + 32.0f;
        }
    }

    //Convert Celsius/Fahrenheit to radiometric data
    public int convertToRadiometric( ImageDto imageDto, float value) {
        float scale = imageDto.getTLinearResolution() == 0 ? 10f : 100f;
        if(isUnitsCelsius()) {
            return Math.round((value + 273.15f) * scale);
        } else {
            float c = (value - 32f) * .5556f;
            return Math.round((c + 273.15f) * scale);
        }
    }

    public Boolean saveTjsn(ImageDto imageDto) throws IOException {

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
        fileOutputStream.write(imageDto.getJsonObject().toString().getBytes(StandardCharsets.UTF_8));
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

    public void saveBitmapToFile(ImageDto imageDto, File file) throws IOException {
        Bitmap bitmap = imageDto.getBitmap();
        FileOutputStream outputStream = new FileOutputStream(file);
        if (outputStream != null && bitmap != null) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.close();
        }
    }

    public String readTjsnFile(String path) {
        String json = new String();
        String line;
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(
                    new FileReader(new File(path)));
            do {
                line = bufferedReader.readLine();
                if (line != null) {
                    json = json + line;
                }
            } while (line != null);
        } catch (IOException e) {
            e.printStackTrace();
            json = "";
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return json;
    }

    public Rect getSpotmeterLocation(ImageDto imageDto) {
        return imageDto.getSpotmeterLocation();
    }

    public void setSpotmeterLocation(ImageDto imageDto, Rect rect) {
        imageDto.setSpotmeterLocation(rect);
    }

//    public boolean isAGC(ImageDto imageDto) {
//        return AGC;
//    }

}