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

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class CameraFragment extends Fragment {

    private FragmentCameraBinding binding;
    private Socket cameraSocket;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if(cameraSocket == null) {
            cameraSocket = ((AcamApplication)MainActivity.getInstance().getApplication()).getCameraSocket();
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        CameraViewModel cameraViewModel =
                new ViewModelProvider(this).get(CameraViewModel.class);

        binding = FragmentCameraBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

//        final TextView textView = binding.textCamera;
//        cameraViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.camera_menu, menu);
        setMenuItems(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        MainActivity activity = MainActivity.getInstance();
        switch (item.getItemId()) {
            case R.id.action_connect:  {
                //Start the SocketIO thread
                activity.invalidateOptionsMenu();
                break;
            }
            case R.id.action_disconnect:
                //activity.destroyCameraSocketIOThread();
                activity.invalidateOptionsMenu();
                break;
            case R.id.action_get: {
                try {
                    cameraSocket.getOutputStream().write("\2get_status\3".getBytes(StandardCharsets.UTF_8));
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