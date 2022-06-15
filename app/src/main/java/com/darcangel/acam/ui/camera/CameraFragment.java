package com.darcangel.acam.ui.camera;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.fragment.app.Fragment;

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.R;
import com.darcangel.acam.constants.Constants;
import com.darcangel.acam.databinding.FragmentCameraBinding;
import com.darcangel.acam.service.CameraService;
import com.darcangel.acam.utils.CameraUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import timber.log.Timber;

public class CameraFragment extends Fragment implements View.OnTouchListener, View.OnClickListener {

    private FragmentCameraBinding binding;
    private Socket cameraSocket;
    private CameraService cameraService;
    private CameraViewModel cameraViewModel;
    private View root = null;
    private boolean isConnectingToCamera = false;
    private boolean isVisibleToUser = false;
    private String[] paletteNames = null;

    private MainActivity mainActivity = null;

    public interface FileSelectionEntryPoint {
        Fragment fileSelectionOwner = null;
        void onFileCreated(FileDescriptor fileDescriptor);
        void onFileSelected(FileDescriptor fileDescriptor);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mainActivity = MainActivity.getInstance();
        paletteNames = mainActivity.getResources().getStringArray(R.array.palette_names);
        cameraViewModel = mainActivity.getCameraViewModel();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        binding = FragmentCameraBinding.inflate(inflater, container, false);
        root = binding.getRoot();
        binding.ivCamera.setOnTouchListener(this);
        binding.ivCamera.setOnClickListener(this);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    CameraUtils util = mainActivity.getCameraUtils();
                    binding.ivColorBar.setVisibility(View.VISIBLE);
                    binding.ivColorBar.setImageBitmap(util.createColorBar(
                            mainActivity.getPaletteFactory().getPaletteByName(
                                    cameraViewModel.getSelectedPalette()), Constants.COLORBAR_WIDTH));
                    if (cameraViewModel.getImage() != null) {
                        binding.ivCamera.setImageBitmap(cameraViewModel.getImage());
                        binding.tvMaxTemperature.setText(createTemperatureString(util.getMaxTemperature()));
                        binding.tvMinTemperature.setText(createTemperatureString(util.getMinTemperature()));
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            //scale the UI position to the bitmap
            float w = v.getRight() - v.getLeft();
            float h = v.getBottom() - v.getTop();
            float scaleX = Constants.IMAGE_WIDTH/w;
            float scaleY = Constants.IMAGE_HEIGHT/h;
            int touchX = (int) event.getX();
            int touchY = (int) event.getY();
            int imageViewX = (int) (touchX * scaleX);
            int imageViewY = (int) (touchY * scaleY);
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        CameraUtils util = mainActivity.getCameraUtils();
                        util.DrawHotspot(binding.ivCamera, imageViewX, imageViewY);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.camera_menu, menu);
        setMenuItems(menu);
    }

    //TODO use LiveData
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // command switch
        switch (item.getItemId()) {
            case R.id.action_connect:  {
                isConnectingToCamera = true;
                connectToCamera();
                break;
            }
            case R.id.action_disconnect:
                disconnectFromCamera();
                break;
            case R.id.action_get: {
                getImage();
                break;
            }
            case R.id.action_palette: {
                String title = ((MenuItemImpl)item).getTitle().toString();
                if(!title.equalsIgnoreCase("Palette") &&
                        !title.equalsIgnoreCase(cameraViewModel.getSelectedPalette())) {
                    cameraViewModel.setSelectedPalette(title);
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int[][] palette = mainActivity.getPaletteFactory()
                                    .getPaletteByName(cameraViewModel.getSelectedPalette());
                            if(palette != null) {
                                binding.ivColorBar.setImageBitmap(mainActivity.getCameraUtils().createColorBar(palette, Constants.COLORBAR_WIDTH));
                                if (cameraViewModel.getImage() != null) {
                                    binding.ivCamera.setImageBitmap(mainActivity.getCameraUtils().remapCurrentImage(palette));
                                }
                            }
                        }
                    });
                }
                binding.ivCamera.getRootView().setOnTouchListener(this);
                mainActivity.invalidateOptionsMenu();
                break;
            }
            case R.id.action_stream: {
                break;
            }
            // file menu items
            case R.id.action_file_quit:
                mainActivity.quit();
                break;
            case R.id.action_file_export:
                exportImage(cameraViewModel.getImage());
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    /**
     * connectToCamera
     *   this is called when the camera is connected
     */
    private void connectToCamera() {
        MainActivity.CameraCallback callback = new MainActivity.CameraCallback() {
            @Override
            public void callback(JSONObject response) {
                try {
                    if (response.has("result")) {
                        if (response.getString("result").equals("OK")) {
                            cameraViewModel.setIsCameraConnected(true);
                            mainActivity.invalidateOptionsMenu();
                            if(isConnectingToCamera) {
                                setTime();
                            }
                        } else {
                            //TODO handle error
                            cameraViewModel.setIsCameraConnected(false);
                        }
                    } else {
                        //TODO handle error
                        cameraViewModel.setIsCameraConnected(false);
                    }
                } catch (JSONException e1) {
                    //TODO handle error
                    cameraViewModel.setIsCameraConnected(false);
                }
            }
        };
        try {
            ((MainActivity)mainActivity).getCameraService().connect(callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * disconnectFromCamera
     */
    private void disconnectFromCamera() {
        MainActivity.CameraCallback callback = new MainActivity.CameraCallback() {
            @Override
            public void callback(JSONObject response) {
                try {
                    if (response.has("result")) {
                        if (response.getString("result").equals("OK")) {
                            cameraViewModel.setIsCameraConnected(false);
                            mainActivity.invalidateOptionsMenu();
                        } else {
                            //TODO handle error
                            cameraViewModel.setIsCameraConnected(true);
                        }
                    } else {
                        //TODO handle error
                        cameraViewModel.setIsCameraConnected(true);
                    }
                } catch (JSONException e1) {
                    //TODO handle error
                    cameraViewModel.setIsCameraConnected(true);
                }
            }
        };
        try {
            mainActivity.getCameraService().disconnect(callback);
        } catch (Exception e) {

        }
    }

    /**
     * setTime
     */
    private void setTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Constants.ARGS_SET_TIME);
        String args = simpleDateFormat.format(new Date());
        String cmd = String.format(Constants.CMD_SET_TIME, args);
        try {
            MainActivity.CameraCallback callback = new MainActivity.CameraCallback() {
                @Override
                public void callback(JSONObject response) {
                    try {
                        if (response.has("result")) {
                            if (response.getString("result").equals("OK")) {
                                if(isConnectingToCamera) {
                                    setConfig();
                                }
                            } else {
                                //TODO handle error
                            }
                        } else {
                            //TODO handle error
                        }
                    } catch (JSONException e1) {
                        //TODO handle error
                    }
                }
            };
            mainActivity.getCameraService().sendCmd(cmd, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * setConfig
     */
    private void setConfig() {
        String args = String.format(Constants.ARGS_SET_CONFIG, 0, 20, 1); //
        String cmd = String.format(Constants.CMD_SET_CONFIG, args);
        isConnectingToCamera = false;
        try {
            MainActivity.CameraCallback callback = new MainActivity.CameraCallback() {
                @Override
                public void callback(JSONObject response) {
                    try {
                        if (response.has("result")) {
                            if(response.getString("result").equals("OK")) {

                            } else {
                                //TODO handle error
                            }
                        }
                    } catch (JSONException e1) {
                        //TODO handle error
                    }
                }
            };
            mainActivity.getCameraService().sendCmd(cmd, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * getImage
     */
    private void getImage() {
        try {
            MainActivity.CameraCallback callback = new MainActivity.CameraCallback() {
                @Override
                public void callback(JSONObject response) {
                    try {
                        CameraUtils util = mainActivity.getCameraUtils();
                        mainActivity.dismissProgressDialog();
                        if (response.has("result")) {
                            if (response.getString("result").equals("OK")) {
                                JSONObject responseObj = response.getJSONObject("response");
                                cameraViewModel.setImage(mainActivity.getCameraUtils().processImageResponse(responseObj,
                                        mainActivity.getPaletteFactory().getPaletteByName(cameraViewModel.getSelectedPalette())));
                                //show the image on the UI thread
                                mainActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (cameraViewModel.getImage() != null) {
                                            ImageView imageView = (ImageView) root.findViewById(R.id.ivCamera);
                                            imageView.setImageBitmap(cameraViewModel.getImage());
                                        }
                                        binding.ivColorBar.setImageBitmap(
                                                util.createColorBar(mainActivity.getPaletteFactory()
                                                        .getPaletteByName(cameraViewModel.getSelectedPalette()),
                                                                Constants.COLORBAR_WIDTH));
                                        binding.tvMaxTemperature.setText(createTemperatureString(util.getMaxTemperature()));
                                        binding.tvMinTemperature.setText(createTemperatureString(util.getMinTemperature()));
                                    }
                                });
                            } else {
                                //TODO handle error
                            }
                        } else {
                            //TODO handle error
                        }
                    } catch (JSONException e1) {
                        //TODO handle error
                    }
                }
            };
            try {
                mainActivity.showProgressDialog(getString(R.string.get_image), getString(R.string.acquiring));
                mainActivity.getCameraService().sendCmd(Constants.CMD_GET_IMAGE, callback);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e2) {
            //TODO handle error
        }
    }

    /**
     * exportImage
     * @param image
     */
    private void exportImage(@NonNull final Bitmap image) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/png,image/jpg"); //not needed, but maybe usefull
        intent.putExtra(Intent.EXTRA_TITLE, "image"); //not needed, but maybe usefull
        startActivityForResult(intent, Constants.RESULT_CODE_CREATE_DOCUMENT);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        setMenuItems(menu);
    }

    private void setMenuItems(Menu menu) {
        MenuItem itemFile = menu.findItem(R.id.action_file);
        // File submenu items
        MenuItem itemFileQuit = menu.findItem(R.id.action_file_quit);
        MenuItem itemConnect = menu.findItem(R.id.action_connect);
        MenuItem itemDisconnect = menu.findItem(R.id.action_disconnect);
        MenuItem itemGet = menu.findItem(R.id.action_get);
        MenuItem itemPalette = menu.findItem(R.id.action_palette);
        MenuItem itemStream = menu.findItem(R.id.action_stream);
        SubMenu paletteSubMenu = itemPalette.getSubMenu();

        if(cameraViewModel.getSelectedPalette() != null && !cameraViewModel.getSelectedPalette().isEmpty()) {
            itemPalette.setTitle(cameraViewModel.getSelectedPalette());
        }
        //since this fragment can be recreated, prevent multiple items
        paletteSubMenu.clear();
        for(int i=0; i<paletteNames.length; i++) {
            paletteSubMenu.add(Menu.NONE, R.id.action_palette, Menu.NONE,paletteNames[i]);
        }
        if(!cameraViewModel.getIsCameraConnected()) {
            itemConnect.setVisible(true);
            itemDisconnect.setVisible(false);
            itemGet.setEnabled(false);
            itemPalette.setEnabled(false);
            itemStream.setEnabled(false);
        } else {
            itemConnect.setVisible(false);
            itemDisconnect.setVisible(true);
            itemGet.setEnabled(true);
            itemPalette.setEnabled(true);
            itemStream.setEnabled(true);
        }
    }

    private String createTemperatureString(int temperature) {
        float temp = temperature;
        temp = temp/64.0F + 40.0F;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("%.1f",temp));
        stringBuilder.append("\u00B0");
        if(mainActivity.getSettings().getUnitsC()) {
            stringBuilder.append("C");
        } else {
            stringBuilder.append("F");
        }
        return stringBuilder.toString();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constants.REQUEST_WRITE_PERMISSION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                }
        };
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case Constants.RESULT_CODE_CREATE_DOCUMENT:
                if(resultCode == -1) {
                    Uri uri = data.getData();
                    Timber.d("uri = %s", uri.toString());
                    try {
                        OutputStream outputStream = mainActivity.getContentResolver().openOutputStream(uri);
                        //FileOutputStream fileOutputStream = new FileOutputStream(outputStream);
                        cameraViewModel.getImage().compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}