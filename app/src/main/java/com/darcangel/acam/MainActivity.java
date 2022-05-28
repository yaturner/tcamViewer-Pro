package com.darcangel.acam;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.widget.Spinner;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.darcangel.acam.Factory.PaletteFactory;
import com.darcangel.acam.constants.Constants;
import com.darcangel.acam.databinding.ActivityMainBinding;
import com.darcangel.acam.dialogs.CameraPreferences;
import com.darcangel.acam.service.CameraService;
import com.darcangel.acam.ui.camera.CameraViewModel;
import com.darcangel.acam.ui.library.LibraryViewModel;
import com.darcangel.acam.ui.settings.SettingsViewModel;
import com.darcangel.acam.utils.Util;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Callable;

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
    private PaletteFactory paletteFactory;

    private CameraService cameraService;
    private boolean isCameraServiceBound = false;
    private Util util;

    private ProgressDialog progressDialog;

    public interface CameraCallback {
        void callback(JSONObject response);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _instance = this;

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init();
        getPermissions();
    }

    private void init() {
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        libraryViewModel = new ViewModelProvider(this).get(LibraryViewModel.class);
        cameraViewModel = new ViewModelProvider(this).get(CameraViewModel.class);

        sharedPreferences = this.getSharedPreferences("tcam", MODE_PRIVATE);

        observe();
        startCameraService();

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_camera, R.id.navigation_settings, R.id.navigation_library)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        CameraPreferences dialog = new CameraPreferences(this);
        dialog.show();
    }

    private void getPermissions() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
            //performAction(...);
        } else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                    Constants.REQUEST_WRITE_PERMISSION);
        }
    }

    private void observe() {

        //Observer for Camera IP Address
        settingsViewModel.getCameraAddress().observe(this, s -> {
            Timber.d("Camera Address is now %s", s);
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

    public void showProgessDialog(final String title, final String msg) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
        }
        if (!progressDialog.isShowing()) {
            progressDialog.setMessage(msg);
            progressDialog.setTitle(title);
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
    }

    public void dismissProgressDialog()
    {
        if(progressDialog != null || progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
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
    }


    private void destroyCameraService() {
        if (isCameraServiceBound) {
            // Detach our existing connection.
            unbindService(connection);
            isCameraServiceBound = false;
        }
    }

    public void quit() {
        finishAndRemoveTask();
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

    public CameraService getCameraService() {
        return cameraService;
    }

    public Util getUtil() {
        if(util == null) {
            util = new Util();
        }
        return util;
    }

    public PaletteFactory getPaletteFactory() {
        if(paletteFactory == null) {
            paletteFactory = new PaletteFactory();
        }
        return paletteFactory;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyCameraService();
    }
}