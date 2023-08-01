package com.darcangel.tcamViewer.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;
import com.darcangel.tcamViewer.constants.Constants;
import com.darcangel.tcamViewer.model.ImageDto;
import com.darcangel.tcamViewer.model.Settings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.inject.Singleton;

@Singleton
public class Utils {
    private MainActivity mainActivity;
    private Settings settings;
    private CameraUtils cameraUtils;
    private ActivityResultLauncher<Intent> activityResultLauncher;


    public Utils() {
        mainActivity = MainActivity.getInstance();
        settings = mainActivity.getSettings();
        cameraUtils = mainActivity.getCameraUtils();
    }

    public void exportImage(final ImageDto imageDto) throws FileNotFoundException, IOException {
        String imageFilename;
        String imageDirectory;
        String imageName;
        Bitmap bitmap = createExportImage(imageDto);
        String[] word = imageDto.getFilename().split("/");
        int nWords = word.length;
        //if there is only one word, then it is the filename and take the folder from the CreationDate
        if(nWords == 1) {
            imageName = imageDto.getFilename().replace("img_", "").replace(".tjsn", "");
            imageDirectory = Constants.simpleDateFormatFolder.format(imageDto.getCreationDate());
        } else {
            imageDirectory = word[nWords - 2];
            imageName = word[nWords - 1].replace("img_", "").replace(".tjsn", "");
        }
        int[] widths = mainActivity.getResources().getIntArray(R.array.resolution_widths);
        int[] heights = mainActivity.getResources().getIntArray(R.array.resolution_heights);
        saveBitmap(bitmap, imageDirectory, imageName);
        Toast.makeText(mainActivity, "Image exported as " + imageName, Toast.LENGTH_LONG).show();
    }

//    @NonNull
//    public Uri saveTjsn(@NonNull final String tjsnString,
//                        @NonNull final String imageDirectory,
//                        @NonNull final String imageName) throws IOException {
//        final int CREATE_FILE = 1001;
//        activityResultLauncher = mainActivity.registerForActivityResult(
//                new ActivityResultContracts.StartActivityForResult(),
//                new ActivityResultCallback<ActivityResult>() {
//                    @Override
//                    public void onActivityResult(ActivityResult result) {
//                        if (result.getResultCode() == Activity.RESULT_OK) {
//                            // There are no request codes
//                            Intent data = result.getData();
//                            ////doSomeOperations();
//                        }
//                    }
//                });
//
//        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.setType("text/plain");
//        intent.putExtra(Intent.EXTRA_TITLE, "test.tjsn");
//
//        Uri pickerInitialUri = Uri.parse(imageDirectory);
//
//        // Optionally, specify a URI for the directory that should be opened in
//        // the system file picker when your app creates the document.
//        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOCUMENTS + "/" + imageDirectory);
//
//        activityResultLauncher.launch(intent);
//
//        return null;
//    }


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
        String maxString, minString;
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

        String imageName = path.substring(path.lastIndexOf(File.separatorChar) + 1)
                .replace(".tjsn", "");
        String hotspotString = settings.getDisplaySpotmeter().getValue()?
            cameraUtils.createTemperatureString(imageDto.getMeanTemperatureAtSpotmeter()):"";
        if(imageDto.isAGC()) {
            maxString = "AGC";
            minString = "AGC";
        } else {
            maxString = cameraUtils.createTemperatureString(temps.second);
            minString = cameraUtils.createTemperatureString(temps.first);
        }
        View inflatedFrame = mainActivity.getLayoutInflater()
                .inflate(R.layout.export_library_image, null);

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
        tvMaxTemperature.setTextColor(mainActivity.getResources().getColor(R.color.white, mainActivity.getTheme()));
        tvMaxTemperature.setTextSize(textSize);
        tvMinTemperature.setText(minString);
        tvMinTemperature.setTextColor(mainActivity.getResources().getColor(R.color.white, mainActivity.getTheme()));
        tvMinTemperature.setTextSize(textSize);

        if (!settings.getExportMetaData().getValue()) {
            //scale to resolution in settings
            return Bitmap.createScaledBitmap(imageDto.getBitmap(),
                    width[res], height[res], false);
        }

        LinearLayoutCompat lline1 = inflatedFrame.findViewById(R.id.llAnnotation_line_1);
        tvLogo.setText(R.string.appName);
        tvLogo.setTextSize(textSize);
        tvLogo.setTextColor(mainActivity.getResources().getColor(R.color.white, mainActivity.getTheme()));
        tvSpotmeterTemperature.setText(hotspotString);
        tvSpotmeterTemperature.setTextSize(textSize);
        tvSpotmeterTemperature.setTextColor(mainActivity.getResources().getColor(R.color.white, mainActivity.getTheme()));
        float emissivity = (float) imageDto.getEmissivity() / 8192f;
        tvEmissivity.setText(String.format(Locale.US, "ε%.2f", emissivity));
        tvEmissivity.setTextSize(textSize);
        tvEmissivity.setTextColor(mainActivity.getResources().getColor(R.color.white, mainActivity.getTheme()));
        lline1.requestLayout();

        LinearLayoutCompat lline2 = inflatedFrame.findViewById(R.id.llAnnotation_line_2);
        tvDateTime.setText(sdf.format(imageDto.getCreationDate()));
        tvDateTime.setTextSize(textSize);
        tvDateTime.setTextColor(mainActivity.getResources().getColor(R.color.white, mainActivity.getTheme()));
        int gain = imageDto.getGainMode();
        tvGain.setText("g" + (gain == 0 ? "LOW" : gain == 1 ? "MEDIUM" : "HIGH"));
        tvGain.setTextSize(textSize);
        tvGain.setTextColor(mainActivity.getResources().getColor(R.color.white, mainActivity.getTheme()));
        lline2.requestLayout();
        inflatedFrame.requestLayout();

        ConstraintLayout constraintLayout = (ConstraintLayout) inflatedFrame
                .findViewById(R.id.clItemLayout);
        constraintLayout.setDrawingCacheEnabled(true);
        constraintLayout.measure(View.MeasureSpec.makeMeasureSpec(0,
                        View.MeasureSpec.UNSPECIFIED),
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
        bitmap.eraseColor(resources.getColor(android.R.color.background_dark, mainActivity.getTheme()));
        Canvas canvas = new Canvas(bitmap);
        constraintLayout.draw(canvas);
        return bitmap;
    }

    @NonNull
    public Uri saveBitmap(@NonNull final Bitmap bitmap,
                          @NonNull final String imageDirectory,
                          @NonNull final String imageName) {
        final ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, imageName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/" + imageDirectory);

        final ContentResolver resolver = MainActivity.getInstance().getContentResolver();
        Uri uri = null;

        try {
            final Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            uri = resolver.insert(contentUri, values);

            if (uri == null)
                throw new IOException("Failed to create new MediaStore record.");

            try (final OutputStream stream = resolver.openOutputStream(uri)) {
                if (stream == null)
                    throw new IOException("Failed to open output stream.");

                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 95, stream))
                    throw new IOException("Failed to save bitmap.");
            }

            return uri;
        }
        catch (IOException e) {
            if (uri != null) {
                // Don't leave an orphan entry in the MediaStore
                resolver.delete(uri, null, null);
            }
        }
        return null;
    }


    public boolean acceptableFiletype(final File filename) {
        boolean result = false;
        if (filename.getName().endsWith(".tjsn") ||
                filename.getName().endsWith(".tmjsn")) {
            result = true;
        }
        return result;
    }

    public boolean acceptableFiletype(final String pathname) {
        boolean result = false;
        if (pathname.endsWith(".tjsn") ||
                pathname.endsWith(".tmjsn")) {
            result = true;
        }
        return result;
    }

}
