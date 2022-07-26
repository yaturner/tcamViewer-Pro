package com.darcangel.acam.ui.camera;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.R;
import com.darcangel.acam.constants.Constants;
import com.darcangel.acam.container.Settings;
import com.darcangel.acam.utils.CameraUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraViewModel extends ViewModel {

    private MutableLiveData<Boolean> isCameraConnected;
    private MutableLiveData<String> selectedPalette;
    private MutableLiveData<Bitmap> image;
    private CameraService cameraService;
    private CameraUtils cameraUtils;
    private MainActivity mainActivity;
    private Settings settings;

    // GetContent creates an ActivityResultLauncher<String> to allow you to pass
    // in the mime type you'd like to allow the user to select
    private ActivityResultLauncher<String> exportActivityResultLauncher =
            MainActivity.getInstance().registerForActivityResult(new ActivityResultContracts.CreateDocument(),
                    new ActivityResultCallback<Uri>() {
                        @Override
                        public void onActivityResult(Uri uri) {
                            //Timber.d("Result = %s", uri.toString());
                            OutputStream outputStream = null;
                            Bitmap bitmap = getImage();
                            try {
                                ContentResolver contentResolver = mainActivity.getContentResolver();
                                MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
                                String type = mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
                                outputStream = contentResolver.openOutputStream(uri);
                                if (outputStream != null && bitmap != null) {
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                                    outputStream.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });

    private ActivityResultLauncher<Intent> openActivityResultLauncher = MainActivity.getInstance().registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Uri uri = data.getData();
                        ContentResolver contentResolver = mainActivity.getContentResolver();
                        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
                        String type = contentResolver.getType(uri);
                        //do what we can can to assure that this is a tjsn file
                        if (!type.equalsIgnoreCase("application/octet-stream")) {
                            return;
                        }
                        String path = uri.getPath().toString();
                        String ext = path.substring(path.lastIndexOf(".") + 1);
                        if (!ext.isEmpty() && !ext.equalsIgnoreCase("tjsn")) {
                            return;
                        }
                        InputStream inputStream;
                        try {
                            inputStream = contentResolver.openInputStream(uri);

                            String tjsn = new String();
                            String line;
                            // if file the available for reading
                            if (inputStream != null) {

                                // prepare the file for reading
                                InputStreamReader chapterReader = new InputStreamReader(inputStream);
                                BufferedReader bufferedReader = new BufferedReader(chapterReader);

                                while ((line = bufferedReader.readLine()) != null) {
                                    tjsn = tjsn + line;
                                }
                                JSONObject jsonObject = new JSONObject(tjsn);
                                Bitmap bitmap = cameraUtils.processImageResponse(jsonObject,
                                        mainActivity.getPaletteFactory().getPaletteByName(getSelectedPalette()));
                                if (bitmap != null) {
                                    setImage(bitmap);
                                }

                            } else {
                                //TODO handle error here
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                            //TODO handle error
                        } catch (JSONException e1) {
                            e1.printStackTrace();
                            //TODO handle error
                        }
                    }
                }
            });


    public CameraViewModel() {
        setSelectedPalette("Fusion"); //TODO get from SharedPrefs
        mainActivity = MainActivity.getInstance();
        cameraService = mainActivity.getCameraService();
        cameraUtils = mainActivity.getCameraUtils();
        settings = mainActivity.getSettings();
        //Listen for changes in ipAddress
        MutableLiveData<String> camera = mainActivity.getSettings().getLiveDataCameraAddress();
        camera.observe(mainActivity, address -> {
            mainActivity.invalidateOptionsMenu();
        });

    }

    public Bitmap getImage() {
        if(image == null) {
            image = new MutableLiveData<Bitmap>(null);
        }
        return image.getValue();
    }

    public MutableLiveData<Bitmap> getImageLiveData() {
        if(image == null) {
            image = new MutableLiveData<Bitmap>(null);
        }
        return image;
    }

    public void setImage(Bitmap newImage) {
        if(image == null) {
            image = new MutableLiveData<Bitmap>(null);
        }
        if(image.getValue() == null || !image.getValue().sameAs(newImage)) {
            image.setValue(newImage);
        }
    }

    public String getSelectedPalette() {
        if(selectedPalette == null) {
            selectedPalette = new MutableLiveData<>();
        }
        return selectedPalette.getValue();
    }

    public void setSelectedPalette(String value) {
        if(selectedPalette == null) {
            selectedPalette = new MutableLiveData<>();
        }
        selectedPalette.setValue(value);
    }

    //Camera operations
    /**
     * connectToCamera
     * this is called when the camera is connected
     */
    public void connectToCamera() {
        try {
            mainActivity.getCameraService().connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * disconnectFromCamera
     */
    public void disconnectFromCamera() {
        try {
            mainActivity.getCameraService().disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * setTime
     */
    public void setTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Constants.ARGS_SET_TIME);
        String args = simpleDateFormat.format(new Date());
        String cmd = String.format(Constants.CMD_SET_TIME, args);
        try {
            mainActivity.getCameraService().sendCmd(cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * setConfig
     */
    public void setConfig() {
        if (cameraService.isConnected()) {
            String args = String.format(Constants.ARGS_SET_CONFIG,
                    settings.getAGC() ? 1 : 0,
                    settings.getEmissivity(),
                    settings.getGainHigh() ? 0 : settings.getGainLow() ? 1 : 2);
            String cmd = String.format(Constants.CMD_SET_CONFIG, args);
            //isConnectingToCamera = false;
            try {
                mainActivity.getCameraService().sendCmd(cmd);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * getImage
     */
    public void getImageFromCamera() {
        try {
//            mainActivity.showProgressDialog(mainActivity.getResources().getString(R.string.get_image),
//                    mainActivity.getResources().getString(R.string.acquiring));
            mainActivity.getCameraService().sendCmd(Constants.CMD_GET_IMAGE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startStreaming(Boolean flag) {
        try {
            if(flag) {
                mainActivity.getCameraService().startStreaming();
            } else {
                mainActivity.getCameraService().stopStreaming();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * exportImage
     *
     * @param image
     */
    public void exportImage(@NonNull final Bitmap image) {
        exportActivityResultLauncher.launch(CameraUtils.generateNewFilename() + ".png");
    }

    /**
     * openImage
     */
    public void openImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent = Intent.createChooser(intent, "Select an image file");
        intent.addFlags(
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
        openActivityResultLauncher.launch(intent);
    }
}