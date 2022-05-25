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

import timber.log.Timber;

public class CameraFragment extends Fragment {

    private FragmentCameraBinding binding;
    private Socket cameraSocket;
    private CameraService cameraService;
    private CameraViewModel cameraViewModel;
    private boolean socketConnected = false;
    private View root = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        cameraViewModel =
                new ViewModelProvider(this).get(CameraViewModel.class);

        binding = FragmentCameraBinding.inflate(inflater, container, false);
        root = binding.getRoot();
        return root;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.camera_menu, menu);
        setMenuItems(menu);
    }

    // Since this is the first fragment we can get a race condition where cameraService isn't
    //  bound when the menu options are active so we always call the getter
    //TODO use LiveData
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        MainActivity activity = MainActivity.getInstance();
        MainActivity.CameraCallback callback = new MainActivity.CameraCallback() {
            @Override
            public void callback(JSONObject response) {
                try {
                    Timber.d("response = %s, item = %d", response, item.getItemId());
                    String resultCode = response.getString("result");
                    switch (item.getItemId()) {
                        case R.id.action_connect:
                            if (resultCode.equals("OK")) {
                                socketConnected = true;
                            } else {
                                socketConnected = false;
                            }
                            activity.invalidateOptionsMenu();
                            break;
                        case R.id.action_disconnect:
                            if (resultCode.equals("OK")) {
                                socketConnected = false;
                            } else {
                                socketConnected = true;
                            }
                            activity.invalidateOptionsMenu();
                            break;
                        case R.id.action_get:
                            if(resultCode.equals("OK")) {
                                JSONObject responseObj = response.getJSONObject("response");
                                Bitmap image = Util.processImageResponse(responseObj);
                                MainActivity.getInstance().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(image != null) {
                                            ImageView imageView = (ImageView)root.findViewById(R.id.ivCamera);
                                            imageView.setImageBitmap(image);
                                        }
                                    }
                                });
                            }
                            break;
                    }
                    ;
                } catch (JSONException e) {
                    //TODO handle this
                }
            }
        };
        // command switch
        switch (item.getItemId()) {
            case R.id.action_connect:  {
                try {
                    MainActivity.getInstance().getCameraService().connect(callback);
                    Thread.sleep(500);
                    String cmd = String.format(Constants.CMD_SET_TIME, "{" +
                            "    \"sec\": 14,\n" +
                            "    \"min\": 10,\n" +
                            "    \"hour\": 21,\n" +
                            "    \"dow\": 2,\n" +
                            "    \"day\": 18,\n" +
                            "    \"mon\": 5,\n" +
                            "    \"year\": 50\n" +
                            "  }");
                    MainActivity.getInstance().getCameraService().sendCmd(cmd, callback);
                    Thread.sleep(500);
                    cmd = String.format(Constants.CMD_SET_CONFIG, "{" +
                            "     \"agc_enabled\": 0," +
                            "     \"emissivity\": 10," +
                            "     \"gain_mode\": 2" +
                            "    }");
                    MainActivity.getInstance().getCameraService().sendCmd(cmd, callback);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                break;
            }
            case R.id.action_disconnect:
                MainActivity.getInstance().getCameraService().disconnect(callback);
                break;
            case R.id.action_get: {
                try {
                    MainActivity.getInstance().getCameraService().sendCmd(Constants.CMD_GET_IMAGE, callback);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
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
        if(!socketConnected) {
            itemConnect.setVisible(true);
            itemDisconnect.setVisible(false);
            itemGet.setEnabled(false);
        } else {
            itemConnect.setVisible(false);
            itemDisconnect.setVisible(true);
            itemGet.setEnabled(true);
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}