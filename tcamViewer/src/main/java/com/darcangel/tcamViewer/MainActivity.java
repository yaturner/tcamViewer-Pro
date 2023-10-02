package com.darcangel.tcamViewer;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.darcangel.tcamViewer.constants.Constants;
import com.darcangel.tcamViewer.databinding.ActivityMainBinding;
import com.darcangel.tcamViewer.factory.PaletteFactory;
import com.darcangel.tcamViewer.model.Settings;
import com.darcangel.tcamViewer.services.CameraService;
import com.darcangel.tcamViewer.ui.camera.CameraViewModel;
import com.darcangel.tcamViewer.ui.library.LibraryViewModel;
import com.darcangel.tcamViewer.ui.settings.SettingsViewModel;
import com.darcangel.tcamViewer.utils.CameraUtils;
import com.darcangel.tcamViewer.utils.Utils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import dagger.hilt.android.AndroidEntryPoint;
import io.sentry.Sentry;
import rxdogtag2.RxDogTag;
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
    private CameraService.CameraServiceBinder binder;
    private CameraUtils cameraUtils;
    private Utils utils;

    private AlertDialog progressDialog;

    private NavController navController;
    private ThreadPoolExecutor executor;
    private MutableLiveData<Boolean> mBound;
    private static final int EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 101;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can proceed with writing to the directory
                ///////writeToMoviesDirectory();
            } else {
                 //Permission denied, inform the user
                //showPermissionExplanationDialog(this);
            }
        }
    }

    public void getPermissions() {
        // Call this method when you need to request the permission
            // Check if we already have the permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission, so we need to request it
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
            } else {
                // We already have permission, you can proceed with your task
                // For example, you can start writing to external storage here
            }
        }
//        if (ContextCompat.checkSelfPermission(
//                this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
//                PackageManager.PERMISSION_GRANTED) {
//        } else {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                    EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
//        }


//    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
//            new ActivityResultContracts.RequestPermission(),
//            result -> {
//                if (result) {
//                    //Permission granted
//                } else {
//                    //permission denied
//                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//                        shouldShowRequestPermissionRationale("test");
//                    } else {
//                        //display error dialog
//                    }
//                };
//
//            }
//    );

    // Check if the app has external storage permission
    public static boolean hasExternalStoragePermission(Activity activity) {
        int permission = ContextCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    // Request external storage permission
    public static void requestExternalStoragePermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
    }

    // Show a dialog explaining why the permission is needed
    public static void showPermissionExplanationDialog(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Permission Required")
                .setMessage("This app needs access to your external storage to perform certain actions.")
                .setPositiveButton("OK", (dialog, which) -> requestExternalStoragePermission(activity))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _instance = this;

        // Bind to LocalService
        if(!isBound()) {
            Intent intent = new Intent(this, CameraService.class);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
            setBound(false);
        }

        RxDogTag.install();

        if (savedInstanceState != null) {
            settings = savedInstanceState.getParcelable(Constants.KEY_SETTINGS);
            binder = (CameraService.CameraServiceBinder)
                    savedInstanceState.getBinder(Constants.KEY_CAMERA_SERVICE);
            if (binder != null) {
                cameraService = binder.getService();
            }
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

        //For debugging only, catch unclosed resources
//        try {
//            Class.forName("dalvik.system.CloseGuard")
//                    .getMethod("setEnabled", boolean.class)
//                    .invoke(null, true);
//        } catch (ReflectiveOperationException e) {
//            Sentry.captureException(e);
//        }

        if (executor == null || executor.getMaximumPoolSize() == 0) {
            executor = new ThreadPoolExecutor(5, 10, 0,
                    TimeUnit.MICROSECONDS, new LinkedBlockingQueue<Runnable>());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(Constants.KEY_SETTINGS, settings);
        outState.putBinder(Constants.KEY_CAMERA_SERVICE, binder);
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
            public void onDestinationChanged(@NonNull NavController navController,
                                             @NonNull NavDestination navDestination,
                                             @Nullable Bundle bundle) {
                BottomNavigationView navBar = findViewById(R.id.nav_view);
                if(navDestination.getId() == R.id.navigation_librarySlideShowFragment ||
                        navDestination.getId() == R.id.navigation_settings ||
                        navDestination.getId() == R.id.wifiSettingsFragment ||
                        navDestination.getId() == R.id.playbackFragment) {
                    navBar.setVisibility(View.GONE);
                } else {
                    navBar.setVisibility(View.VISIBLE);
                }
            }
        });
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

    public void showProgressDialog(final String title) {
        if(progressDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false); // if you want user to wait for some process to finish,
            builder.setView(R.layout.progress_layout);
            progressDialog = builder.create();
        }
        progressDialog.setMessage(title);
        progressDialog.show();
    }

    public void dismissProgressDialog() {
        if(progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();;
            progressDialog = null;
        }
    }

    public void showSocketError() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Socket Error");
            builder.setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());
            builder.setMessage(R.string.socket_error);
            builder.create().show();
        } catch(Exception e) {
            Sentry.captureException(e);
        }
        cameraViewModel.disconnectFromCamera();
        invalidateMenu();
    }

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
        return cameraService;
    }

    public CameraUtils getCameraUtils() {
        if(cameraUtils == null) {
            cameraUtils = new CameraUtils();
        }
        return cameraUtils;
    }

    public Utils getUtils() {
        if(utils == null) {
            utils = new Utils();
        }
        return utils;
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

    public Boolean isBound() {
        if(mBound == null) {
            mBound = new MutableLiveData<>(false);
        }
        return mBound.getValue();
    }

    public LiveData<Boolean> getBound() {
        if(mBound == null) {
            mBound = new MutableLiveData<>(false);
        }
        return mBound;
    }

    public void setBound(Boolean bound) {
        if(mBound == null) {
            mBound = new MutableLiveData<>(false);
        }
        this.mBound.setValue(bound);
    }

    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            binder = (CameraService.CameraServiceBinder) service;
            cameraService = binder.getService();
            setBound(true);
            Timber.d("\\\\cameraService\\\\ bound = true");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            setBound(false);
            cameraService = null;
            Timber.d("\\\\cameraService\\\\ bound = false");

        }
    };

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing() && !isChangingConfigurations()) {
            stopService(new Intent(this, CameraService.class));
            unbindService(connection);
            setBound(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        if(isFinishing() && cameraViewModel != null && cameraViewModel.getStreaming()) {
//            cameraService.stopStreaming();
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(executor.getMaximumPoolSize() == 0) {
            executor = new ThreadPoolExecutor(5, 10, 0,
                    TimeUnit.MICROSECONDS, new LinkedBlockingQueue<Runnable>());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //executor.shutdown();
    }
}