package com.darcangel.tcamViewer.ui.camera;

import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;
import com.darcangel.tcamViewer.constants.Constants;
import com.darcangel.tcamViewer.container.Settings;
import com.darcangel.tcamViewer.databinding.FragmentCameraBinding;
import com.darcangel.tcamViewer.utils.CameraUtils;

import org.json.JSONObject;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import timber.log.Timber;

public class CameraFragment extends Fragment implements View.OnTouchListener, MenuProvider {

    private FragmentCameraBinding binding;
    private CameraService cameraService;
    private CameraViewModel cameraViewModel;
    private View root = null;
    private boolean isConnectingToCamera = false;
    private final boolean isVisibleToUser = false;
    private String[] paletteNames = null;
    private CameraUtils cameraUtils;
    private Settings settings;
    private Disposable disposable;
    private MainActivity mainActivity = null;

    @Override
    public void onPrepareMenu(@NonNull Menu menu) {
        MenuProvider.super.onPrepareMenu(menu);
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.camera_menu, menu);
        setMenuItems(menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        // command switch
        switch (menuItem.getItemId()) {
            case R.id.action_connect: {
                isConnectingToCamera = true;
                cameraViewModel.connectToCamera();
                mainActivity.invalidateOptionsMenu();
                break;
            }
            case R.id.action_disconnect:
                if(cameraViewModel.getStreaming()) {
                    cameraViewModel.setStreaming(false);
                    cameraService.stopStreaming();
                }
                cameraViewModel.disconnectFromCamera();
                isConnectingToCamera = false;
                mainActivity.invalidateOptionsMenu();
                break;
            case R.id.action_get: {
                if(settings.getShutterSound().getValue()) {
                    MediaPlayer mediaPlayer = MediaPlayer.create(mainActivity, R.raw.camera_shutter);
                    mediaPlayer.start();
                }
                mainActivity.showProgressDialog(getString(R.string.acquiring), "");
                cameraViewModel.getImageFromCamera();
                mainActivity.invalidateOptionsMenu(); //enable save
                break;
            }
            case R.id.action_palette: {
                String title = ((MenuItemImpl) menuItem).getTitle().toString();
                if (!title.equalsIgnoreCase("Palette") &&
                        !title.equalsIgnoreCase(settings.getPalette().getValue())) {
                    settings.setPalette(title);
                    settings.persist();
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int[][] palette = mainActivity.getPaletteFactory()
                                    .getPaletteByName(settings.getPalette().getValue());
                            if (palette != null) {
                                binding.ivColorBar.setImageBitmap(mainActivity.getCameraUtils().createColorBar(palette, Constants.COLORBAR_WIDTH));
                                if (cameraViewModel.getImage() != null) {
                                        cameraViewModel.setImage(cameraUtils.remapCurrentImage(palette));
                                    drawScreen();
                                }
                            }
                        }
                    });
                }
                binding.ivCamera.getRootView().setOnTouchListener(this);
                mainActivity.invalidateOptionsMenu();
                break;
            }
            case R.id.action_stream: {
                if (cameraViewModel.getStreaming()) {
                    cameraViewModel.startStreaming(false);
                } else {
                    cameraViewModel.startStreaming(true);
                }
                mainActivity.invalidateOptionsMenu();
                break;
            }
            // file menu items
            case R.id.action_save:
                if(settings.getShutterSound().getValue()) {
                    MediaPlayer mediaPlayer = MediaPlayer.create(mainActivity, R.raw.camera_shutter);
                    mediaPlayer.start();
                }
                try {
                    cameraUtils.saveTjsn();
                } catch (IOException e) {
                    e.printStackTrace();
                    //TODO handle error
                }
                break;
        }

        return true;
    }

    @Override
    public void onMenuClosed(@NonNull Menu menu) {
        MenuProvider.super.onMenuClosed(menu);
    }

    public interface FileSelectionEntryPoint {
        Fragment fileSelectionOwner = null;

        void onFileCreated(FileDescriptor fileDescriptor);

        void onFileSelected(FileDescriptor fileDescriptor);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainActivity = MainActivity.getInstance();
        paletteNames = mainActivity.getResources().getStringArray(R.array.palette_names);
        cameraViewModel = mainActivity.getCameraViewModel();
        cameraUtils = mainActivity.getCameraUtils();
        cameraService = mainActivity.getCameraService();
        settings = mainActivity.getSettings();

//        info_value:
//        0	Command NACK - the command failed. See the information string for more information.
//        1	Command ACK - the command succeeded.
//        2	Command unimplemented - the camera firmware does not implement the command.
//        3	Command bad - the command was incorrectly formatted or was not a json string.
//        4	Internal Error - the camera detected an internal error. See the information string for more information.
//        5	Debug Message - The information string contains an internal debug message from the camera (not normally generated).

        disposable = cameraService.getImageChannel()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(obj -> {
                            Iterator<String> it = obj.keys();
                            if (it.hasNext()) {
                                String response = it.next();
                                Timber.d("Response String is %s", response);
                                //error handling
                                if(response.equalsIgnoreCase("error")) {
                                    String msg = new JSONObject(obj.getString("error")).getString("message");
                                    mainActivity.dismissProgressDialog(); //just in case
                                    if (msg.startsWith("java.net.SocketTimeoutException") ||
                                            msg.startsWith("java.net.ConnectException")) {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity)
                                                .setCancelable(true)
                                                .setPositiveButton(R.string.ok, ((dialog, which) -> {
                                                    dialog.dismiss();
                                                }))
                                                .setTitle(R.string.title_error)
                                                .setMessage(R.string.error_can_not_connect);
                                        builder.create().show();
                                    }
                                    //connect/disconnect
                                } else if (response.equalsIgnoreCase("connected")) {
                                    String value = obj.getString("connected");
                                    if (value.equalsIgnoreCase("true")) {
                                        if (isConnectingToCamera) {
                                            cameraViewModel.setTime();
                                        }
                                        mainActivity.invalidateOptionsMenu();
                                    } else {
                                        //TODO JMT ERROR
                                        mainActivity.invalidateOptionsMenu();
                                    }
                                //camera settings commands
                                } else if (response.equalsIgnoreCase("cam_info")) {
                                    //multiple response have "cam_info"
                                    JSONObject info = obj.getJSONObject("cam_info");
                                    if (info.has("info_string")) {
                                        String infoType = info.getString("info_string");
                                        if (infoType.equalsIgnoreCase("set_time success")) {
                                            if (isConnectingToCamera) {
                                                cameraViewModel.getConfig();
                                            }
                                        } else if (infoType.equalsIgnoreCase("set_config success")) {
                                            //do nothing
                                        }
                                    }
                                    //get_config
                                } else if (response.equalsIgnoreCase("config")) {
                                    JSONObject config = obj.getJSONObject("config");
                                    if (config.has("agc_enabled")) {
                                        settings.setAGC(config.getInt("agc_enabled") == 1);
                                    }
                                    if (config.has("emissivity")) {
                                        settings.setEmissivity(config.getInt("emissivity"));
                                    }
                                    if (config.has("gain_mode")) {
                                        switch (config.getInt("gain_mode")) {
                                            case 0:
                                                settings.setGainHigh(true);
                                                break;
                                            case 1:
                                                settings.setGainLow(true);
                                                break;
                                            case 2:
                                                settings.setGainAuto(true);
                                                break;
                                        }
                                    }
                                    settings.persist();
                                    //get wifi
                                } else if (response.equalsIgnoreCase("wifi")) {
                                    int flags = 0;
                                    JSONObject wifi = obj.getJSONObject("wifi");
                                    if (wifi.has("ap_ssid")) {
                                        settings.setApSSID(wifi.getString("ap_ssid"));
                                    }
                                    if (wifi.has("sta_ssid")) {
                                        settings.setStaticSSID(wifi.getString("sta_ssid"));
                                    }
                                    if (wifi.has("ap_ip_addr")) {
                                        settings.setApIPAddress(wifi.getString("ap_ip_addr"));
                                    }
                                    if (wifi.has("sta_ip_addr")) {
                                        settings.setStaticIPAddress(wifi.getString("sta_ip_addr"));
                                    }
                                    if (wifi.has("sta_netmask")) {
                                        settings.setStaticNetmask(wifi.getString("sta_netmask"));
                                    }
                                    if (wifi.has("flags")) {
                                        flags = wifi.getInt("flags") & 0xff;
                                        settings.setFlags(flags);
                                    }
                                    //parse flags and set values
                                    settings.setCameraIsAccessPoint((flags & Constants.WIFI_MASK_CLIENT_MODE)
                                            == 0);
                                    settings.setUseStaticIPWhenClient((flags & Constants.WIFI_MASK_STATIC_IP) ==
                                            1);
                                    if (settings.getCameraIsAccessPoint().getValue()) {
                                        settings.setSSID(settings.getApSSID());
                                    } else {
                                        settings.setSSID(settings.getStaticSSID());
                                    }
                                    //get image
                                } else if (response.equalsIgnoreCase("metadata")) {
                                    //Timber.d("Received onNext");
                                    Bitmap bitmap = null;
                                    bitmap = cameraUtils.processImageResponse(obj,
                                            mainActivity.getPaletteFactory().getPaletteByName(settings.getPalette().getValue()),
                                            false);
                                    cameraViewModel.setImage(bitmap);
                                    drawScreen();
                                    mainActivity.dismissProgressDialog();
                                }
                            }
                        });
    }


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        binding = FragmentCameraBinding.inflate(inflater, container, false);
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        root = binding.getRoot();
        binding.ivCamera.setOnTouchListener(this);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        settings.getCameraAddress().observe(mainActivity, address -> {
            Timber.d("Camera ip address is now %s", address);
            if (!address.equals(cameraService.getIpAddress())) {
                cameraService.setIpAddress(address);
                mainActivity.invalidateOptionsMenu();
            }
        });
        //watch for palette changes
        settings.getPalette().observe(mainActivity, palette ->
        {
            if(cameraViewModel.getImage() != null && !palette.equalsIgnoreCase(settings.getPalette().getValue())) {
                cameraViewModel.setRemapNeeded(true);
            }
        });
        //watch for palette rotation requests
        binding.ivColorBar.setOnClickListener(v -> {
            rotateColormap();
        });

        drawScreen();
    }

    private void rotateColormap() {
        String pal = settings.getPalette().getValue();
        for (int index = 0; index < paletteNames.length; index++) {
            if (pal.equalsIgnoreCase(paletteNames[index])) {
                if (index == paletteNames.length - 1) {
                    index = -1;
                }
                settings.setPalette(paletteNames[index + 1]);
                settings.persist();
                if (cameraViewModel.getImage() != null) {
                    cameraViewModel.setImage(cameraUtils.remapCurrentImage(
                            mainActivity.getPaletteFactory().
                                    getPaletteByName(settings.getPalette().getValue())));

                    drawScreen();
                }
                break;
            }
        }
    }

    private void drawScreen() {
        mainActivity.runOnUiThread(() -> {
            Bitmap image = null;
            if (binding != null && binding.ivColorBar.getVisibility() == View.VISIBLE) {
                try {
                    int[][] palette = mainActivity.getPaletteFactory().getPaletteByName(
                            settings.getPalette().getValue());
                    binding.ivColorBar.setVisibility(View.VISIBLE);
                    binding.ivColorBar.setImageBitmap(cameraUtils.createColorBar(
                            palette, Constants.COLORBAR_WIDTH));
                    if (cameraViewModel.getImage() != null) {
                        image = cameraUtils.drawHotspot(settings.getDisplaySpotmeter().getValue());
                        //Do we need to recreate the image
                        if (cameraViewModel.isRemapNeeded()) {
                            cameraViewModel.setRemapNeeded(false);
                            cameraUtils.remapCurrentImage(mainActivity.getPaletteFactory().
                                            getPaletteByName(settings.getPalette().getValue()));
                        }
                        cameraViewModel.setImage(image);
                        binding.ivCamera.setImageBitmap(image);
                        //Always get AGC for the current image, when settings are changed it refers to the next get
                        if (cameraUtils.isAGC()) {
                            binding.tvMaxTemperature.setText("AGC");
                            binding.tvMinTemperature.setText("AGC");
                        } else {
                            Pair<Float, Float> temps = cameraUtils.getTemperatures();
                            binding.tvMinTemperature.setText(createTemperatureString(temps.first));
                            binding.tvMaxTemperature.setText(createTemperatureString(temps.second));
                        }
                        binding.ivHistogram.setImageBitmap(cameraUtils.createHistogram(palette,
                                (int) getResources().getDimension(R.dimen.histogram_width)));
                        binding.tvSpotmeter.setText(createTemperatureString(cameraUtils.
                                getMeanTemperatureAtSpotmeter()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float displayImageHeight = mainActivity.getResources().getDimension(R.dimen.display_image_height);
            float displayImageWidth = mainActivity.getResources().getDimension(R.dimen.display_image_width);
            float scaleX = 160.0f / displayImageWidth;
            float scaleY = 120.0f / displayImageHeight;
            int imageViewX = (int) (event.getX() * scaleX);
            int imageViewY = (int) (event.getY() * scaleY);
            String args = String.format(Locale.US, Constants.ARGS_SET_SPOTMETER,
                    imageViewX,
                    imageViewX + 1,
                    imageViewY,
                    imageViewY + 1);
            String cmd = String.format(Constants.CMD_SET_SPOTMETER, args);
            try {
                if (cameraViewModel.getStreaming()) {
                    cameraService.stopStreaming();
                }
                cameraService.sendCmd(cmd);
                cameraUtils.setSpotmeterLocation(new Rect(
                        imageViewX,
                        imageViewY,
                        imageViewX + 1,
                        imageViewY + 1));
                cameraViewModel.setImage(cameraUtils.drawHotspot(settings.getDisplaySpotmeter().getValue()));
                if (cameraViewModel.getStreaming()) {
                    cameraService.startStreaming();
                } else {
                    drawScreen();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private void setMenuItems(Menu menu) {
        MenuItem itemSave = menu.findItem(R.id.action_save);
        MenuItem itemConnect = menu.findItem(R.id.action_connect);
        MenuItem itemDisconnect = menu.findItem(R.id.action_disconnect);
        MenuItem itemGet = menu.findItem(R.id.action_get);
        MenuItem itemPalette = menu.findItem(R.id.action_palette);
        MenuItem itemStream = menu.findItem(R.id.action_stream);
        SubMenu paletteSubMenu = itemPalette.getSubMenu();

        if (settings.getPalette() != null && !settings.getPalette().getValue().isEmpty()) {
            itemPalette.setTitle(settings.getPalette().getValue());
        }
        //since this fragment can be recreated, prevent multiple items
        paletteSubMenu.clear();
        for (int i = 0; i < paletteNames.length; i++) {
            paletteSubMenu.add(Menu.NONE, R.id.action_palette, Menu.NONE, paletteNames[i]);
        }
        itemPalette.setEnabled(true);
        itemSave.setVisible(true);
        if(cameraViewModel.getImage() == null) {
            itemSave.setEnabled(false); //only true if there is an image
        } else {
            itemSave.setEnabled(true);
        }
        if (!cameraService.isConnected()) {
            itemConnect.setVisible(true);
            itemDisconnect.setVisible(false);
            itemGet.setEnabled(false);
            itemStream.setEnabled(false);
        } else {
            itemConnect.setVisible(false);
            itemDisconnect.setVisible(true);
            itemGet.setEnabled(true);
            itemStream.setEnabled(true);
        }
        if (!cameraService.isConnected() || cameraViewModel.getStreaming()) {
            itemGet.setEnabled(false);
        } else {
            itemGet.setEnabled(true);
        }
    }

    private String createTemperatureString(float temperature) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format(Locale.US, "%.01f", temperature));
        stringBuilder.append("\u00B0");
        if (mainActivity.getSettings().getUnitsC().getValue()) {
            stringBuilder.append("C");
        } else {
            stringBuilder.append("F");
        }
        return stringBuilder.toString();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.REQUEST_WRITE_PERMISSION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraViewModel.getStreaming()) {
            cameraService.stopStreaming();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (cameraViewModel.getStreaming()) {
            cameraService.startStreaming();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraViewModel.getStreaming()) {
            cameraService.stopStreaming();
        }
        if(disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
        binding = null;
    }
}