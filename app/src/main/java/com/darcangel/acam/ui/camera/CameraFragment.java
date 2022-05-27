package com.darcangel.acam.ui.camera;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.darcangel.acam.AcamApplication;
import com.darcangel.acam.MainActivity;
import com.darcangel.acam.R;
import com.darcangel.acam.constants.Constants;
import com.darcangel.acam.databinding.FragmentCameraBinding;
import com.darcangel.acam.service.CameraService;
import com.darcangel.acam.utils.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if(savedInstanceState != null) {
            isCameraConnected = savedInstanceState.getBoolean(Constants.KEY_IS_CAMERA_CONNECTED);
            image = savedInstanceState.getParcelable(Constants.KEY_CAMERA_IMAGE);
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        cameraViewModel =
                new ViewModelProvider(this).get(CameraViewModel.class);

        binding = FragmentCameraBinding.inflate(inflater, container, false);
        root = binding.getRoot();
        if(image != null) {
            MainActivity.getInstance().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (image != null) {
                        ImageView imageView = (ImageView) root.findViewById(R.id.ivCamera);
                        imageView.setImageBitmap(image);
                    }
                }
            });
        }
        return root;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(Constants.KEY_IS_CAMERA_CONNECTED, isCameraConnected);
        if(image != null) {
            outState.putParcelable(Constants.KEY_CAMERA_IMAGE, image);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        this.isVisibleToUser = isVisibleToUser;
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
//                View menuItemView = MainActivity.getInstance().getWindow().findViewById(R.id.action_palette);
//                item.hasSubMenu();
//                PopupMenu popupMenu = new PopupMenu(MainActivity.getInstance(), menuItemView);
//                popupMenu.inflate(R.menu.popup_palette);
//                popupMenu.show();
                break;
            }
            case R.id.action_stream: {
                break;
            }
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
                            MainActivity.getInstance().invalidateOptionsMenu();
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
            MainActivity.getInstance().getCameraService().connect(callback);
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
                            MainActivity.getInstance().invalidateOptionsMenu();
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
            MainActivity.getInstance().getCameraService().disconnect(callback);
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
            MainActivity.getInstance().getCameraService().sendCmd(cmd, callback);
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
        MainActivity.getInstance().getCameraService().sendCmd(cmd, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * getImage
     */
    private void getImage() {
        String args = String.format(Constants.ARGS_SET_CONFIG, 0, 20, 0);
        String cmd = String.format(Constants.CMD_SET_CONFIG, args);
        try {
            MainActivity.CameraCallback callback = new MainActivity.CameraCallback() {
                @Override
                public void callback(JSONObject response) {
                    try {
                        if (response.has("result")) {
                            if (response.getString("result").equals("OK")) {
                                JSONObject responseObj = response.getJSONObject("response");
                                image = MainActivity.getInstance().getUtil().processImageResponse(responseObj);
                                //show the image on the UI thread
                                MainActivity.getInstance().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (image != null) {
                                            ImageView imageView = (ImageView) root.findViewById(R.id.ivCamera);
                                            imageView.setImageBitmap(image);
                                        }
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
                MainActivity.getInstance().getCameraService().sendCmd(Constants.CMD_GET_IMAGE, callback);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e2) {
            //TODO handle error
        }
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        setMenuItems(menu);
    }

    private void setMenuItems(Menu menu) {
        MenuItem itemConnect = menu.findItem(R.id.action_connect);
        MenuItem itemDisconnect = menu.findItem(R.id.action_disconnect);
        MenuItem itemGet = menu.findItem(R.id.action_get);
        MenuItem itemPalette = menu.findItem(R.id.action_palette);
        MenuItem itemStream = menu.findItem(R.id.action_stream);
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}