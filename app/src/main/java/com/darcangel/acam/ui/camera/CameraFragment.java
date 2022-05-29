package com.darcangel.acam.ui.camera;

import android.app.appsearch.GetByDocumentIdRequest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.darcangel.acam.AcamApplication;
import com.darcangel.acam.Factory.PaletteFactory;
import com.darcangel.acam.MainActivity;
import com.darcangel.acam.R;
import com.darcangel.acam.constants.Constants;
import com.darcangel.acam.databinding.FragmentCameraBinding;
import com.darcangel.acam.pallete.Rainbow;
import com.darcangel.acam.service.CameraService;
import com.darcangel.acam.utils.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

import timber.log.Timber;

public class CameraFragment extends Fragment {

    private FragmentCameraBinding binding;
    private Socket cameraSocket;
    private CameraService cameraService;
    private CameraViewModel cameraViewModel;
    private View root = null;
    private boolean isCameraConnected = false;
    private boolean isConnectingToCamera = false;
    private boolean isVisibleToUser = false;
    private Bitmap image = null;
    private String[] paletteNames = null;
    private String selectedPalette;

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

        if(savedInstanceState != null) {
            isCameraConnected = savedInstanceState.getBoolean(Constants.KEY_IS_CAMERA_CONNECTED);
            image = savedInstanceState.getParcelable(Constants.KEY_CAMERA_IMAGE);
            selectedPalette = savedInstanceState.getString(Constants.KEY_SELECTED_PALETTE);
        }

        mainActivity = MainActivity.getInstance();
        paletteNames = mainActivity.getResources().getStringArray(R.array.palette_names);
        selectedPalette = "Rainbow";
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        cameraViewModel =
                new ViewModelProvider(this).get(CameraViewModel.class);

        binding = FragmentCameraBinding.inflate(inflater, container, false);
        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    binding.ivColorBar.setVisibility(View.VISIBLE);
                    binding.ivColorBar.setImageBitmap(mainActivity.getUtil().createColorBar(
                            mainActivity.getPaletteFactory().getPaletteByName(selectedPalette)));
                    if (image != null) {
                        binding.ivCamera.setImageBitmap(image);
                        binding.llButtonBar.setVisibility(View.VISIBLE);
                    } else {
                        //no image no buttons
                        binding.llButtonBar.setVisibility(View.INVISIBLE);
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(Constants.KEY_IS_CAMERA_CONNECTED, isCameraConnected);
        if(image != null) {
            outState.putParcelable(Constants.KEY_CAMERA_IMAGE, image);
        }
        outState.putString(Constants.KEY_SELECTED_PALETTE, selectedPalette);
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
                        !title.equalsIgnoreCase(selectedPalette)) {
                    selectedPalette = title;
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int[][] palette = mainActivity.getPaletteFactory()
                                    .getPaletteByName(selectedPalette);
                            if(palette != null) {
                                binding.ivColorBar.setImageBitmap(mainActivity.getUtil().createColorBar(palette));
                                if (image != null) {
                                    binding.ivCamera.setImageBitmap(mainActivity.getUtil().remapCurrentImage(palette));
                                }
                            }
                        }
                    });
                }
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
                exportImage(image);
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    /**
     * connectToCamera
     */
    private void connectToCamera() {
        MainActivity.CameraCallback callback = new MainActivity.CameraCallback() {
            @Override
            public void callback(JSONObject response) {
                try {
                    if (response.has("result")) {
                        if (response.getString("result").equals("OK")) {
                            isCameraConnected = true;
                            mainActivity.invalidateOptionsMenu();
                            if(isConnectingToCamera) {
                                setTime();
                            }
                        } else {
                            //TODO handle error
                            isCameraConnected = false;
                        }
                    } else {
                        //TODO handle error
                        isCameraConnected = false;
                    }
                } catch (JSONException e1) {
                    //TODO handle error
                    isCameraConnected = false;
                }
            }
        };
        try {
            ((MainActivity)mainActivity).getCameraService().connect(callback);
        } catch (Exception e) {

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
                            isCameraConnected = false;
                            mainActivity.invalidateOptionsMenu();
                        } else {
                            //TODO handle error
                            isCameraConnected = true;
                        }
                    } else {
                        //TODO handle error
                        isCameraConnected = true;
                    }
                } catch (JSONException e1) {
                    //TODO handle error
                    isCameraConnected = true;
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
                        mainActivity.dismissProgressDialog();
                        if (response.has("result")) {
                            if (response.getString("result").equals("OK")) {
                                JSONObject responseObj = response.getJSONObject("response");
                                image = mainActivity.getUtil().processImageResponse(responseObj,
                                        mainActivity.getPaletteFactory().getPaletteByName(selectedPalette));
                                //show the image on the UI thread
                                mainActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (image != null) {
                                            ImageView imageView = (ImageView) root.findViewById(R.id.ivCamera);
                                            imageView.setImageBitmap(image);
                                            binding.llButtonBar.setVisibility(View.VISIBLE);
                                        }
                                        binding.ivColorBar.setImageBitmap(
                                                mainActivity.getUtil().createColorBar(mainActivity.getPaletteFactory()
                                                        .getPaletteByName(selectedPalette)));
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

        if(selectedPalette != null && !selectedPalette.isEmpty()) {
            itemPalette.setTitle(selectedPalette);
        }
        //since this fragment can be recreated, prevent multiple items
        paletteSubMenu.clear();
        for(int i=0; i<paletteNames.length; i++) {
            paletteSubMenu.add(Menu.NONE, R.id.action_palette, Menu.NONE,paletteNames[i]);
        }
        if(!isCameraConnected) {
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
                        image.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
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