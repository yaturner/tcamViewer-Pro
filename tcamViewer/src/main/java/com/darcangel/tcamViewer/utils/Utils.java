package com.darcangel.tcamViewer.utils;

import android.content.ContentValues;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;
import com.darcangel.tcamViewer.model.ImageDto;
import com.darcangel.tcamViewer.model.Settings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.inject.Singleton;

@Singleton
public class Utils {
    private MainActivity mainActivity;
    private Settings settings;
    private CameraUtils cameraUtils;

    public Utils() {
        mainActivity = MainActivity.getInstance();
        settings = mainActivity.getSettings();
        cameraUtils = mainActivity.getCameraUtils();
    }

    public void exportImage(final ImageDto imageDto) throws FileNotFoundException {
        String imageFilename = null;
        Bitmap bitmap = createExportImage(imageDto);
        String path = imageDto.getFilename();
        String imageName = path.substring(path.lastIndexOf(File.separatorChar) + 1).replace(".tjsn", "");
        int[] widths = mainActivity.getResources().getIntArray(R.array.resolution_widths);
        int[] heights = mainActivity.getResources().getIntArray(R.array.resolution_heights);
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        File myDir = new File(root);
        myDir.mkdirs();
        OutputStream out = null;
        File imageFile = new File(root, imageName);
        saveImage(bitmap, root, imageName);
        Toast.makeText(mainActivity, "Image exported as " + imageName, Toast.LENGTH_LONG).show();
    }

    /**
     * createExportImage
     *
     * @param imageDto
     * @return - the image to be shared/exported, if export metadata is off, then only the image is returned
     */
    public Bitmap createExportImage(ImageDto imageDto) {
        ImageView ivImageView;
        TextView tvMaxTemperature;
        ImageView ivColorBar;
        TextView tvMinTemperature;
        TextView tvLogo;
        TextView tvSpotmeterTemperature;
        TextView tvEmissivity;
        TextView tvDateTime;
        TextView tvGain;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss MM/dd/yyyy");

        Paint paint;
        Resources resources;
        float scale;
        int layoutWidth;
        int layoutHeight;
        int bitmapWidth;
        int bitmapHeight;
        StringBuilder stringBuilder = new StringBuilder();
        int res = settings.getExportResolution().getValue();
        resources = mainActivity.getResources();
        int[] width = resources.getIntArray(R.array.resolution_widths);
        int[] height = resources.getIntArray(R.array.resolution_heights);
        float textSize;
        scale = resources.getDisplayMetrics().density;
        Pair<Float, Float> temps = imageDto.getTemperatures();
        String path = imageDto.getFilename();

        switch (res) {
            case 0:
                textSize = 4f;
                break;
            case 1:
                textSize = 6f;
                break;
            case 3:
                textSize = 12f;
                break;
            case 2:
            default:
                textSize = 8f;
        }
        /////textSize = textSize * scale;

        String imageName = path.substring(path.lastIndexOf(File.separatorChar) + 1).replace(".tjsn", "");
        String hotspotString = cameraUtils.createTemperatureString(imageDto.getMeanTemperatureAtSpotmeter());
        String maxString = cameraUtils.createTemperatureString(temps.second);
        String minString = cameraUtils.createTemperatureString(temps.first);
        View inflatedFrame = mainActivity.getLayoutInflater().inflate(R.layout.export_library_image, null);

        tvMaxTemperature = inflatedFrame.findViewById(R.id.tvMaxTemperature);
        ivColorBar = inflatedFrame.findViewById(R.id.ivColorBar);
        tvMinTemperature = inflatedFrame.findViewById(R.id.tvMinTemperature);
        tvLogo = inflatedFrame.findViewById(R.id.tvLogo);
        tvSpotmeterTemperature = inflatedFrame.findViewById(R.id.tvSpotmeterTemperature);
        tvEmissivity = inflatedFrame.findViewById(R.id.tvEmissivity);
        tvDateTime = inflatedFrame.findViewById(R.id.tvDateTime);
        tvGain = inflatedFrame.findViewById(R.id.tvGain);
        ivImageView = inflatedFrame.findViewById(R.id.ivCamera);

        ViewGroup.LayoutParams lp = new LinearLayout.LayoutParams(width[res], height[res]);
        ivImageView.setLayoutParams(lp);

        tvMaxTemperature.setText(maxString);
        tvMaxTemperature.setTextSize(textSize);
        tvMinTemperature.setText(minString);
        tvMinTemperature.setTextSize(textSize);

        if (!settings.getExportMetaData().getValue()) {
            //scale to resolution in settings
            return Bitmap.createScaledBitmap(imageDto.getBitmap(), width[res], height[res], false);
        }

        LinearLayoutCompat lline1 = inflatedFrame.findViewById(R.id.llAnnotation_line_1);
        tvLogo.setText(R.string.appName);
        tvLogo.setTextSize(textSize);
        tvSpotmeterTemperature.setText(hotspotString);
        tvSpotmeterTemperature.setTextSize(textSize);
        float emissivity = (float) imageDto.getEmissivity() / 8192f;
        tvEmissivity.setText(String.format(Locale.US, "ε%.2f", emissivity));
        tvEmissivity.setTextSize(textSize);
        lline1.requestLayout();

        LinearLayoutCompat lline2 = inflatedFrame.findViewById(R.id.llAnnotation_line_2);
        tvDateTime.setText(sdf.format(imageDto.getCreationDate()));
        tvDateTime.setTextSize(textSize);
        int gain = imageDto.getGainMode();
        tvGain.setText("g" + (gain == 0 ? "LOW" : gain == 1 ? "MEDIUM" : "HIGH"));
        tvGain.setTextSize(textSize);
        lline2.requestLayout();
        inflatedFrame.requestLayout();

        ConstraintLayout constraintLayout = (ConstraintLayout) inflatedFrame.findViewById(R.id.clItemLayout);
        constraintLayout.setDrawingCacheEnabled(true);
        constraintLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        layoutHeight = constraintLayout.getMeasuredHeight();
        layoutWidth = constraintLayout.getMeasuredWidth();
        constraintLayout.layout(0, 0, layoutWidth, layoutHeight);
        constraintLayout.buildDrawingCache(true);

        Bitmap bitmap = imageDto.drawHotspot();
        ivImageView.setImageBitmap(bitmap);
        Bitmap colorbar = imageDto.createColorBar();
        ivColorBar.setImageBitmap(colorbar);
        bitmap = Bitmap.createBitmap(layoutWidth, layoutHeight, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(resources.getColor(android.R.color.black, mainActivity.getTheme()));
        Canvas canvas = new Canvas(bitmap);
        constraintLayout.draw(canvas);
        return bitmap;
    }

    public void saveImage(Bitmap bitmap, String folderName, String imageFilename) throws FileNotFoundException {
        File dir = null, imageFile = null;
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + folderName);
            values.put(MediaStore.Images.Media.IS_PENDING, true);
            // RELATIVE_PATH and IS_PENDING are introduced in API 29.

            Uri uri = mainActivity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                saveImageToStream(bitmap, mainActivity.getContentResolver().openOutputStream(uri));
                values.put(MediaStore.Images.Media.IS_PENDING, false);
                mainActivity.getContentResolver().update(uri, values, null, null);
            }
        } else {
            dir = new File(mainActivity.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "tcamViewer");
            // getExternalStorageDirectory is deprecated in API 29

            if (!dir.exists()) {
                dir.mkdirs();
            }

            java.util.Date date = new java.util.Date();
            imageFile = new File(dir.getAbsolutePath()
                    + File.separator
                    + imageFilename
                    + ".png");
            saveImageToStream(bitmap, new FileOutputStream(imageFile));
            if (imageFile.getAbsolutePath() != null) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, imageFile.getAbsolutePath());
                // .DATA is deprecated in API 29
                mainActivity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            }
        }
    }

    public void saveImageToStream(Bitmap bitmap, OutputStream outputStream) {
        if (outputStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
