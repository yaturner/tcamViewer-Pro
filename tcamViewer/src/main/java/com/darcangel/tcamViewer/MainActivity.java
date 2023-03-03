package com.darcangel.tcamViewer;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;

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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import dagger.hilt.android.AndroidEntryPoint;
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

    private NavController navController;
    private ThreadPoolExecutor executor;
    private MutableLiveData<Boolean> mBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _instance = this;

        // Bind to LocalService
        if(!isBound()) {
            Intent intent = new Intent(this, CameraService.class);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
            //////startService(intent);
            setBound(false);
        }

        RxDogTag.install();

        if (savedInstanceState != null) {
            cameraUtils = savedInstanceState.getParcelable(Constants.KEY_CAMERAUTILS);
            utils = savedInstanceState.getParcelable(Constants.KEY_UTILS);
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
        try {
            Class.forName("dalvik.system.CloseGuard")
                    .getMethod("setEnabled", boolean.class)
                    .invoke(null, true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

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

    @Override
    protected void onUserLeaveHint()
    {
//        Timber.d("onUserLeaveHint\\\\Home button pressed");
        super.onUserLeaveHint();
        //JMT cameraViewModel.startStreaming(false);
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
//                Timber.d("New Destination is %s", navDestination.toString());
                BottomNavigationView navBar = findViewById(R.id.nav_view);
                if(navDestination.getId() == R.id.navigation_librarySlideShowFragment ||
                        navDestination.getId() == R.id.navigation_settings ||
                        navDestination.getId() == R.id.wifiSettingsFragment) {
                    navBar.setVisibility(View.GONE);
                } else {
                    navBar.setVisibility(View.VISIBLE);
                }
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
    }

    public void dismissProgressDialog() {
    }

    public void showSocketError() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Socket Error");
            builder.setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());
            builder.setMessage(R.string.socket_error);
            builder.create().show();
        } catch(Exception e) {
            //TODO handle error
            e.printStackTrace();
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