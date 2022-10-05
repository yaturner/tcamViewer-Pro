package com.darcangel.tcamViewer;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.darcangel.tcamViewer.constants.Constants;
import com.darcangel.tcamViewer.model.Settings;
import com.darcangel.tcamViewer.databinding.ActivityMainBinding;
import com.darcangel.tcamViewer.factory.PaletteFactory;
import com.darcangel.tcamViewer.ui.camera.CameraService;
import com.darcangel.tcamViewer.ui.camera.CameraViewModel;
import com.darcangel.tcamViewer.ui.library.LibraryViewModel;
import com.darcangel.tcamViewer.ui.settings.SettingsViewModel;
import com.darcangel.tcamViewer.utils.CameraUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements ViewModelStoreOwner {

    private ActivityMainBinding binding;
    private SettingsViewModel settingsViewModel;
    private LibraryViewModel libraryViewModel;
    private CameraViewModel cameraViewModel;

    private Settings settings;

    public static MainActivity getInstance() {
        return _instance;
    }

    private static MainActivity _instance = null;
    private SharedPreferences sharedPreferences;
    private PaletteFactory paletteFactory;

    private CameraService cameraService;
    private CameraUtils cameraUtils;

    private ProgressDialog progressDialog;

    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _instance = this;

        System.loadLibrary("camera");

        if(savedInstanceState != null) {
            cameraUtils = savedInstanceState.getParcelable(Constants.KEY_CAMERAUTILS);
            cameraService = savedInstanceState.getParcelable(Constants.KEY_CAMERASERVICE);
            settings = savedInstanceState.getParcelable(Constants.KEY_SETTINGS);
        }

        //order is important, do this before setting the view
        ViewModelProvider viewModelProvider = new ViewModelProvider(this);
        settingsViewModel = viewModelProvider.get(SettingsViewModel.class);
        libraryViewModel = viewModelProvider.get(LibraryViewModel.class);
        cameraViewModel = viewModelProvider.get(CameraViewModel.class);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init();
        getPermissions();
        getSettings();

        navController.addOnDestinationChangedListener((navController1, navDestination, bundle) -> {
            if(navDestination.getId() == R.id.navigation_settings ||
                    navDestination.getId() == R.id.wifiSettingsFragment) {
                getNavView().setVisibility(View.GONE);
            } else {
                getNavView().setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(Constants.KEY_CAMERAUTILS, cameraUtils);
        outState.putParcelable(Constants.KEY_SETTINGS, settings);
        outState.putParcelable(Constants.KEY_CAMERASERVICE, cameraService);
        super.onSaveInstanceState(outState);
    }

    private void init() {
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_camera, R.id.navigation_settings, R.id.navigation_library)
                .build();

        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController navController, @NonNull NavDestination navDestination,
                                             @Nullable Bundle bundle) {
                Timber.d("New Destination is %s", navDestination.toString());
            }
        });
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

    private void putSharedPreferences(final String key, final Object value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean)value);
        } else if (value instanceof String) {
            editor.putString(key, (String)value);
        } else if(value instanceof Integer) {
            editor.putInt(key, (Integer)value);
        } else if(value instanceof Float) {
            editor.putFloat(key, (Float)value);
        } else if(value instanceof Long) {
            editor.putLong(key, (Long)value);
        }
        editor.commit();
    }

    public void showProgressDialog(final String title, final String msg) {
//        if (progressDialog == null) {
//            progressDialog = new ProgressDialog(this);
//        }
//        if (!progressDialog.isShowing()) {
//            progressDialog.setMessage(msg);
//            progressDialog.setTitle(title);
//            progressDialog.setIndeterminate(true);
//            progressDialog.setCancelable(false);
//            progressDialog.show();
//        }
    }

    public void dismissProgressDialog()
    {
        if(progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
//    private ServiceConnection connection = new ServiceConnection() {
//
//        @Override
//        public void onServiceConnected(ComponentName className,
//                                       IBinder service) {
//            // We've bound to LocalService, cast the IBinder and get LocalService instance
////            CameraService.LocalBinder binder = (CameraService.LocalBinder) service;
////            cameraService = binder.getService();
//            if(cameraService == null) {
//                cameraService = new CameraService();
//            }
//            cameraService.setIpAddress(settings.getCameraAddress());
////            isCameraServiceBound = true;
//
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName arg0) {
//            isCameraServiceBound = false;
//        }
//    };
//
//    private void startCameraService() {
//        Intent intent = new Intent(this, CameraService.class);
//        bindService(intent, connection, Context.BIND_AUTO_CREATE);
//        isCameraServiceBound = true;
//    }
//
//
//    private void destroyCameraService() {
//        if (isCameraServiceBound) {
//            // Detach our existing connection.
//            unbindService(connection);
//            isCameraServiceBound = false;
//        }
//    }

    public NavController getNavController() {
        return navController;
    }

    public void quit() {
        finishAndRemoveTask();
    }

    public SharedPreferences getSharedPreferences() {
        if(sharedPreferences == null) {
            sharedPreferences = getSharedPreferences("tcam", MODE_PRIVATE);
        }
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
        if(cameraService == null) {
            cameraService = new CameraService();
        }
        return cameraService;
    }

    public CameraUtils getCameraUtils() {
        if(cameraUtils == null) {
            cameraUtils = new CameraUtils();
        }
        return cameraUtils;
    }

    public PaletteFactory getPaletteFactory() {
        if(paletteFactory == null) {
            paletteFactory = new PaletteFactory();
        }
        return paletteFactory;
    }

    public Settings getSettings() {
        if(settings == null) {
            settings = new Settings();
        }
        return settings;
    }

    public View getNavView() {
        try {
            return binding.navView;
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraViewModel != null && cameraViewModel.getStreaming()) {
            cameraService.stopStreaming();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}