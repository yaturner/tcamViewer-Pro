package com.darcangel.acam;

import android.os.Bundle;

import com.darcangel.acam.ui.camera.CameraViewModel;
import com.darcangel.acam.ui.library.LibraryViewModel;
import com.darcangel.acam.ui.settings.SettingsViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.darcangel.acam.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements ViewModelStoreOwner {

    private ActivityMainBinding binding;
    private ViewModel settingsViewModel;
    private ViewModel libraryViewModel;
    private ViewModel cameraViewModel;

    public static MainActivity getInstance() {return _instance;}

    private static MainActivity _instance = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _instance = this;

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        libraryViewModel = new ViewModelProvider(this).get(LibraryViewModel.class);
        cameraViewModel = new ViewModelProvider(this).get(CameraViewModel.class);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_camera, R.id.navigation_settings, R.id.navigation_library)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    public ViewModel getSettingsViewModel() {
        return settingsViewModel;
    }

    public ViewModel getLibraryViewModel() {
        return libraryViewModel;
    }

    public ViewModel getCameraViewModel() {
        return cameraViewModel;
    }

}