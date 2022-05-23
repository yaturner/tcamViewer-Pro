package com.darcangel.acam;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.darcangel.acam.constants.Constants;
import com.darcangel.acam.databinding.ActivityMainBinding;
import com.darcangel.acam.service.CameraService;
import com.darcangel.acam.ui.camera.CameraViewModel;
import com.darcangel.acam.ui.library.LibraryViewModel;
import com.darcangel.acam.ui.settings.SettingsViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements ViewModelStoreOwner {

    private ActivityMainBinding binding;
    private SettingsViewModel settingsViewModel;
    private LibraryViewModel libraryViewModel;
    private CameraViewModel cameraViewModel;

    public static MainActivity getInstance() {
        return _instance;
    }

    private static MainActivity _instance = null;
    private SharedPreferences sharedPreferences;

    private CameraService cameraService;
    private boolean isCameraServiceBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _instance = this;

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init();
    }

    private void init() {
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        libraryViewModel = new ViewModelProvider(this).get(LibraryViewModel.class);
        cameraViewModel = new ViewModelProvider(this).get(CameraViewModel.class);

        sharedPreferences = this.getSharedPreferences("tcam", MODE_PRIVATE);

        observe();

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_camera, R.id.navigation_settings, R.id.navigation_library)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        startCameraService();
    }

    private void observe() {

        //Observer for Camera IP Address
        settingsViewModel.getCameraAddress().observe(this, s -> {
            Timber.d("Camera Address is now " + s);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(Constants.KEY_CAMERA_IP_ADDRESS, s);
            editor.commit();
        });

        //Create the observer for emissivity
        settingsViewModel.getEmissivity().observe(this, s -> {
            Timber.d("EmissivityList is " + s);
        });

        //Create the observer for the Manual Range Switch
        settingsViewModel.getManualRange().observe(this, s -> Timber.d("Manual Range Switch is " + s));
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            CameraService.LocalBinder binder = (CameraService.LocalBinder) service;
            cameraService = binder.getService();
            isCameraServiceBound = true;
            cameraService.connect();
            try {
                String response = cameraService.sendCmd("\2{\"cmd\":\"get_status\"}\3");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isCameraServiceBound = false;
        }
    };

    private void startCameraService() {
        Intent intent = new Intent(this, CameraService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        isCameraServiceBound = true;
        /////cameraService.IsBoundable();
    }


    private void destroyCameraService() {
        if (isCameraServiceBound) {
            // Detach our existing connection.
            unbindService(connection);
            isCameraServiceBound = false;
        }
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public SettingsViewModel getSettingsViewModel() {
        return settingsViewModel;
    }

    public LibraryViewModel getLibraryViewModel() {
        return libraryViewModel;
    }

    public CameraViewModel getCameraViewModel() {
        return cameraViewModel;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyCameraService();
    }
}