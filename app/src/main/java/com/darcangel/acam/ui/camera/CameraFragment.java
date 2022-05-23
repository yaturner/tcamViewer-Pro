package com.darcangel.acam.ui.camera;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.darcangel.acam.AcamApplication;
import com.darcangel.acam.MainActivity;
import com.darcangel.acam.R;
import com.darcangel.acam.databinding.FragmentCameraBinding;
import com.darcangel.acam.service.CameraService;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import timber.log.Timber;

public class CameraFragment extends Fragment {

    private FragmentCameraBinding binding;
    private Socket cameraSocket;
    private CameraService cameraService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        CameraViewModel cameraViewModel =
                new ViewModelProvider(this).get(CameraViewModel.class);

        binding = FragmentCameraBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

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
        switch (item.getItemId()) {
            case R.id.action_connect:  {
                //Start the SocketIO thread
                MainActivity.getInstance().getCameraService().connect();
                activity.invalidateOptionsMenu();
                break;
            }
            case R.id.action_disconnect:
                //activity.destroyCameraSocketIOThread();
                activity.invalidateOptionsMenu();
                break;
            case R.id.action_get: {
                try {
                    MainActivity.CameraCallback callback = new MainActivity.CameraCallback() {
                        @Override
                        public void callback(String response) {
                            Timber.d("response = %s", response);
                        }
                    };
                    MainActivity.getInstance().getCameraService().sendCmd("\2{\"cmd\":\"get_status\"}\3", callback);
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
/*
        if(MainActivity.getInstance().getCameraSocketIO() == null) {
            itemConnect.setVisible(true);
            itemDisconnect.setVisible(false);
        } else {
            itemConnect.setVisible(false);
            itemDisconnect.setVisible(true);
        }
*/
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}