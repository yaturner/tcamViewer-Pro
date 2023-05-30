package com.darcangel.tcamViewer.ui.camera;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.internal.view.SupportMenuItem;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.darcangel.tcamViewer.BuildConfig;
import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;
import com.darcangel.tcamViewer.constants.Constants;
import com.darcangel.tcamViewer.databinding.FragmentCameraBinding;
import com.darcangel.tcamViewer.model.ImageDto;
import com.darcangel.tcamViewer.model.RecordingDto;
import com.darcangel.tcamViewer.model.Settings;
import com.darcangel.tcamViewer.services.CameraService;
import com.darcangel.tcamViewer.utils.CameraUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.sentry.Sentry;
import timber.log.Timber;

public class CameraFragment extends Fragment implements View.OnTouchListener, MenuProvider {

    private FragmentCameraBinding binding;
    private CameraService cameraService;
    private CameraViewModel cameraViewModel;
    private View root = null;
    private String[] paletteNames = null;
    private CameraUtils cameraUtils;
    private Settings settings;
    private Disposable disposable;
    private MainActivity mainActivity = null;

    private long startMillis = -1;
    private long startNano;
    private long endNano;
    private long prevImageTime = 0L;
    private BufferedOutputStream tjsnOutputStream;
    private BufferedOutputStream recordingOutputStream;
    private ObjectOutputStream infoOutputStream;
    private String tjsnFilename;
    private String recordingFilename;
    private String infoFilename;
    private File tmjsn;
    private RecordingDto recordingDto;
    private boolean exportTjsn;
    private boolean saveImage;

    private boolean showFrameRate = false;

    private ActivityResultLauncher<Intent> saveActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Timber.d("\\\\result = %s", result.toString());
                    if(result.getResultCode() == RESULT_OK) {
                        Intent intent = result.getData();
                        String filename = "";
                        Uri uri = intent.getData();
                            String uriString = uri.toString();
                            if (result.getData().getData().toString().startsWith("content://")) {
                                Cursor cursor = null;
                                try {
                                    cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
                                    if (cursor != null && cursor.moveToFirst()) {
                                        int index = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME);
                                        filename = (index < 0) ? "" : cursor.getString(index);
                                    }
                                } finally {
                                    cursor.close();
                                }
                            }
                            String words[] = filename.split("\\.");
                            if(words.length != 2  || !words[1].startsWith("tjsn"))
                            {
                                Toast.makeText(getContext(), R.string.filetype_not_mtjsn, Toast.LENGTH_LONG)
                                        .show();
                                return;

                            } else {
                                try {
                                    String tjsnString = cameraViewModel.getImageDto().getValue().getTjsnString();
                                    BufferedOutputStream tjsnOutputStream = new BufferedOutputStream(mainActivity.getContentResolver()
                                            .openOutputStream(intent.getData()));

                                    tjsnOutputStream.write(tjsnString.getBytes(StandardCharsets.UTF_8));
                                    tjsnOutputStream.flush();
                                    tjsnOutputStream.close();

                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                    Sentry.captureException(e);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }

                    }
                }
            });

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
        ImageDto imageDto = cameraViewModel.getImageDto().getValue();
        // command switch
        switch (menuItem.getItemId()) {
            case R.id.action_connect: {
                // Connect
                if (!cameraViewModel.connectToCamera()) {
                    mainActivity.showSocketError();
                }
                mainActivity.invalidateOptionsMenu();
                break;
            }
            case R.id.action_disconnect:
                // Disconnect
                if (cameraViewModel.getStreaming()) {
                    cameraViewModel.setStreaming(false);
                    cameraViewModel.setInStreamingMode(false);
                    cameraService.stopStreaming();
                }
                cameraViewModel.disconnectFromCamera();
                mainActivity.invalidateOptionsMenu();
                break;
            case R.id.action_get: {
                // Get
                if (settings.getShutterSound().getValue()) {
                    MediaPlayer mediaPlayer = MediaPlayer.create(mainActivity, R.raw.camera_shutter);
                    mediaPlayer.start();
                }
                cameraViewModel.getImageFromCamera();
                drawScreen();
                break;
            }
            case R.id.palette_arctic:
                if (!settings.getPalette().getValue().equalsIgnoreCase("Arctic")) {
                    setPaletteFromMenu("Arctic", menuItem);
                }
                break;
            case R.id.palette_banded:
                if (!settings.getPalette().getValue().equalsIgnoreCase("Banded")) {
                    setPaletteFromMenu("Banded", menuItem);
                }
                break;
            case R.id.palette_blackhot:
                if (!settings.getPalette().getValue().equalsIgnoreCase("Blackhot")) {
                    setPaletteFromMenu("Blackhot", menuItem);
                }
                break;
            case R.id.palette_doubleRainbow:
                if (!settings.getPalette().getValue().equalsIgnoreCase("DoubleRainbow")) {
                    setPaletteFromMenu("DoubleRainbow", menuItem);
                }
                break;
            case R.id.palette_fusion:
                if (!settings.getPalette().getValue().equalsIgnoreCase("Fusion")) {
                    setPaletteFromMenu("Fusion", menuItem);
                }
                break;
            case R.id.palette_gray:
                if (!settings.getPalette().getValue().equalsIgnoreCase("Gray")) {
                    setPaletteFromMenu("Gray", menuItem);
                }
                break;
            case R.id.palette_ironblack:
                if (!settings.getPalette().getValue().equalsIgnoreCase("Ironblack")) {
                    setPaletteFromMenu("Ironblack", menuItem);
                }
                break;
            case R.id.palette_isotherm:
                if (!settings.getPalette().getValue().equalsIgnoreCase("Isotherm")) {
                    setPaletteFromMenu("Isotherm", menuItem);
                }
                break;
            case R.id.palette_rainbow:
                if (!settings.getPalette().getValue().equalsIgnoreCase("Rainbow")) {
                    setPaletteFromMenu("Rainbow", menuItem);
                }
                break;
            case R.id.palette_sepia:
                if (!settings.getPalette().getValue().equalsIgnoreCase("Sepia")) {
                    setPaletteFromMenu("Sepia", menuItem);
                }
                break;
            case R.id.action_stop: {
                // stop streaming
                cameraViewModel.startStreaming(false);
                cameraViewModel.setInStreamingMode(false);
                mainActivity.invalidateOptionsMenu();
                if (BuildConfig.FLAVOR.equalsIgnoreCase(Constants.PRO_VERSION) && cameraViewModel.isRecording()) {
                    cameraViewModel.setRecording(false);
                    //write the footer summary and the info file
                    recordingDto.getFrameOffset().add(tmjsn.length());
                    String recordingFooter = cameraViewModel.getRecordingFooter();
                    try {
                        recordingOutputStream.write(recordingFooter.getBytes(StandardCharsets.UTF_8));
                        recordingOutputStream.flush();
                        recordingOutputStream.close();
                        recordingOutputStream = null;
                        //full path name
                        infoOutputStream.writeObject(recordingDto);
                        infoOutputStream.flush();
                        infoOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Sentry.captureException(e);
                    }
                }
                break;
            }
            case R.id.action_stream_start: {
                // start streaming
                cameraViewModel.startStreaming(true);
                cameraViewModel.setInStreamingMode(true);
                mainActivity.invalidateOptionsMenu();
                break;
            }
            case R.id.action_record_start: {
                // start recording the stream
                //for simplicity's sake use the same naming conventions as for tjsn
                try {
                    File rootDir = MainActivity.getInstance().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    String file = cameraUtils.generateNewFilename(true) + ".tmjsn";
                    File path = new File(rootDir + "/" + cameraUtils.generateNewPath());
                    if (!path.exists()) {
                        path.mkdirs();
                    }
                    tmjsn = new File(path, file);
                    if (!tmjsn.exists()) {
                        tmjsn.createNewFile();
                    }
                    recordingFilename = path + "/" + file;
                    recordingOutputStream = new BufferedOutputStream(new FileOutputStream(tmjsn));
                    String filename = tmjsn.getPath();
                    infoFilename = filename.substring(0, filename.lastIndexOf(".")) + ".info";
                    FileOutputStream fos = new FileOutputStream(infoFilename);
                    infoOutputStream = new ObjectOutputStream(fos);

                    cameraViewModel.setRecording(true);
                    cameraViewModel.startStreaming(true);
                    cameraViewModel.setInStreamingMode(true);
                    mainActivity.invalidateOptionsMenu();
                } catch (IOException e) {
                    e.printStackTrace();
                    Sentry.captureException(e);
                }
                break;
            }

            // file menu items
            case R.id.action_save_raw:
                exportTjsn = true;
                saveImage = false;
                saveExportTjsn();
                break;
            case R.id.action_save_both:
                exportTjsn = true;
                saveImage = true;
                saveExportTjsn();
                break;
            case R.id.action_save_image:
                // save
                exportTjsn = false;
                saveImage = true;
                saveExportTjsn();
                break;
        }
        return true;
    }

    private void saveExportTjsn() {
        if (settings.getShutterSound().getValue()) {
            MediaPlayer mediaPlayer = MediaPlayer.create(mainActivity, R.raw.camera_shutter);
            mediaPlayer.start();
        }
        try {
            ImageDto imageDto = cameraViewModel.getImageDto().getValue();
            String imageName, imageDirectory;
            Date now = new Date();
            imageDirectory = CameraUtils.simpleDateFormatFolder.format(now);
            imageName = CameraUtils.simpleDateFormatFile.format(now);
            if (exportTjsn) {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/vnd.tcam.tjsn");
                intent.putExtra(Intent.EXTRA_TITLE, imageName + ".tjsn");

                Uri pickerInitialUri = Uri.parse(imageDirectory);

                // Optionally, specify a URI for the directory that should be opened in
                // the system file picker when your app creates the document.
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS + "/"
                        + imageDirectory);
                saveActivityResultLauncher.launch(intent);
            }
            if(saveImage) {
                imageDto.saveTjsn();
                if (settings.getExportOnSave().getValue()) {
                    mainActivity.getUtils().exportImage(imageDto);
                }
            }
        } catch(Exception e){
            Sentry.captureException(e);
            //TODO handle error
        }
    }


    private void setPaletteFromMenu(final String paletteName, final MenuItem menuItem) {
        SupportMenuItem item = ((SupportMenuItem) menuItem);
        item.setTitle(paletteName);
        //this will trigger the observer and set the palette in the ImageDTO
        settings.setPalette(paletteName);
        settings.persist();
        mainActivity.invalidateMenu();

        ImageDto imageDto = cameraViewModel.getImageDto().getValue();
        int[][] palette;
        if (imageDto != null) {
            palette = mainActivity.getPaletteFactory()
                    .getPaletteByName(imageDto.getPaletteName());
            if (palette == null) {
                palette = mainActivity.getPaletteFactory()
                        .getPaletteByName(settings.getPalette().getValue());
            }
            imageDto.setPalette(palette);
            imageDto.remapImage();
            binding.ivColorBar.setImageBitmap(imageDto.createColorBar());
            binding.ivCamera.getRootView().setOnTouchListener(this);
            mainActivity.invalidateOptionsMenu();
            drawScreen();
        }
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
        settings = mainActivity.getSettings();
        cameraService = mainActivity.getCameraService();
    }


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        binding = FragmentCameraBinding.inflate(inflater, container, false);
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        root = binding.getRoot();
        binding.ivCamera.setOnTouchListener(this);
        binding.ivColorBar.setOnTouchListener(this);
        binding.tvMaxTemperature.setOnTouchListener(this);
        binding.tvFrameRate.setVisibility(View.GONE);
        showFrameRate = false;
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //watch for palette changes
        settings.getPalette().observe(mainActivity, palette ->
        {
            ImageDto imageDto = cameraViewModel.getImageDto().getValue();
            if (imageDto != null && !imageDto.getPaletteName().equals(palette)) {
                imageDto.setPaletteName(palette);
                imageDto.setPalette(MainActivity.getInstance().getPaletteFactory().getPaletteByName(palette));
                cameraViewModel.setRemapNeeded(true);
            }
        });

        mainActivity.getBound().observe(mainActivity, b -> {
            if (b) {
                cameraService = mainActivity.getCameraService();
                cameraViewModel.setCameraService(cameraService);
                //we may be in onResume
                if (cameraViewModel.isInStreamingMode()) {
                    cameraService.startStreaming();
                    cameraViewModel.setStreaming(true);
                }
                disposable = cameraService.getImageChannel()
                        .map(obj -> {
                            if(showFrameRate && BuildConfig.DEBUG) {
                                if (prevImageTime != 0L) {
                                    float elapsedTime = SystemClock.elapsedRealtime() - prevImageTime;
                                    float frameRate = (1000.0f / elapsedTime);
                                    mainActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            String string = String.format(Locale.US, "%.2f", elapsedTime);
                                            if (elapsedTime > 200f) {
                                                binding.tvFrameRate.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_dark, null));
                                            } else if (elapsedTime > 400f) {
                                                binding.tvFrameRate.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark, null));
                                            } else {
                                                binding.tvFrameRate.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark, null));
                                            }
                                            binding.tvFrameRate.setText(string);
                                        }
                                    });
//                                Timber.d("\\\\Streaming\\\\ elapsed time = %.2f millis", elapsedTime);
                                }
                                prevImageTime = SystemClock.elapsedRealtime();
                            }
                            return obj;
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(obj -> handleCameraResponse(obj), Throwable::printStackTrace);
                settings.getCameraAddress().observe(mainActivity, address -> {
//                    Timber.d("Camera ip address is now %s", address);
                    if (!address.equals(cameraService.getIpAddress())) {
                        cameraService.setIpAddress(address);
                        mainActivity.invalidateOptionsMenu();
                    }
                });
            } else {
                cameraService = null;
            }
        });

        drawScreen();
    }

    private void handleCameraResponse(JSONObject obj) throws JSONException {
//        info_value:
//        0	Command NACK - the command failed. See the information string for more information.
//        1	Command ACK - the command succeeded.
//        2	Command unimplemented - the camera firmware does not implement the command.
//        3	Command bad - the command was incorrectly formatted or was not a json string.
//        4	Internal Error - the camera detected an internal error. See the information string for more information.
//        5	Debug Message - The information string contains an internal debug message from the camera (not normally generated).
        startNano = System.nanoTime();

        Iterator<String> it = obj.keys();
        if (it.hasNext()) {
            String response = it.next();
            ///Timber.d("Response String is %s", response);
            //get image
            if (response.equalsIgnoreCase("metadata")) {
                //resuse the ImageDto instance to save time and space
                if (cameraViewModel.getImageDto().getValue() != null) {
                    cameraViewModel.getImageDto().getValue().parse(obj, settings.getPalette().getValue());
                } else {
                    cameraViewModel.setImageDto(new ImageDto(obj, settings.getPalette().getValue()));
                }
//                Timber.d("\\\\recording\\\\ createDate = %s",
//                        cameraViewModel.getImageDto().getValue().getCreationDate().toString());
                if(cameraViewModel.isRecording() && recordingOutputStream != null) {
                    try {
                        //the first image is the start date, the last the end date
                        if(cameraViewModel.getFrameCount() == 0) {
                            recordingDto = cameraViewModel.getRecordingDto();
                            cameraViewModel.setRecordingStartDate();
                        }
                        byte[] bytes = obj.toString().getBytes(StandardCharsets.UTF_8);
                        recordingOutputStream.write(bytes);
                        recordingOutputStream.write((byte)3);
                        recordingOutputStream.flush();
                        recordingDto.getFrameOffset().add(tmjsn.length());
                        recordingDto.getFrameSize().add(bytes.length);
                        recordingDto.addFrameDelay();
                        cameraViewModel.setRecordingStartDate();
                        cameraViewModel.incrFrameCount();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Sentry.captureException(e);
                    }
                }
                drawScreen();
                //if we are not streaming and have an image, enable save
                if (!cameraViewModel.getStreaming()) {
                    mainActivity.invalidateOptionsMenu();
                }
                obj = null;
            } else if (response.equalsIgnoreCase("error")) {
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
                    if (cameraService.isConnected()) {
                        cameraViewModel.setTime();
                    }
                    mainActivity.invalidateOptionsMenu();
                } else {
                    //TODO JMT ERROR
                    mainActivity.invalidateOptionsMenu();
                }
            }
        }
    }

    private void drawScreen() {
        startNano = System.nanoTime();

        Bitmap image = null;
        ImageDto imageDto = cameraViewModel.getImageDto().getValue();
        if (binding != null && binding.ivColorBar != null && imageDto != null) {
            try {
                int[][] palette = imageDto.getPalette();
                binding.ivColorBar.setVisibility(View.VISIBLE);
                binding.ivColorBar.setImageBitmap(imageDto.createColorBar());
                if (imageDto.getBitmap() != null) {
                    //Do we need to recreate the image
                    if (cameraViewModel.isRemapNeeded()) {
                        cameraViewModel.setRemapNeeded(false);
                        imageDto.remapImage();
                    }
                    if (settings.getDisplaySpotmeter().getValue()) {
                        image = imageDto.drawHotspot();
                        binding.tvSpotmeter.setText(cameraUtils.createTemperatureString(imageDto.
                                getMeanTemperatureAtSpotmeter()));
                        imageDto.setBitmap(image);
                    } else {
                        //In case the spot meter was in the bitmap, clear it out
                        imageDto.remapImage();
                    }
                    binding.ivCamera.setImageBitmap(imageDto.getBitmap());
                    //Always get AGC for the current image, when settings are changed it refers to the next get
                    if (imageDto.isAGC()) {
                        binding.tvMaxTemperature.setText("AGC");
                        binding.tvMinTemperature.setText("AGC");
                    } else {
                        Pair<Float, Float> temps = imageDto.getTemperatures();
                        binding.tvMinTemperature.setText(cameraUtils.createTemperatureString(temps.first));
                        binding.tvMaxTemperature.setText(cameraUtils.createTemperatureString(temps.second));
                    }
                    binding.ivHistogram.setImageBitmap(imageDto.createHistogram());
                }
            } catch (Exception e) {
                Sentry.captureException(e);
            }
        }
        endNano = System.nanoTime();
        //Timing Timber.d("\\\\Timing\\\\ drawScreen took %5.2f millis", (float)((endNano-startNano)/1000000.0));
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        ImageDto imageDto = cameraViewModel.getImageDto().getValue();
        if (imageDto != null &&
                event.getAction() == MotionEvent.ACTION_UP) {
            if (v.getId() == R.id.ivCamera &&
                    Boolean.TRUE.equals(settings.getDisplaySpotmeter().getValue())) {
                if (Boolean.TRUE.equals(settings.getAGC().getValue())) {
                    Toast.makeText(getContext(), R.string.changing_spot_meter_not_available, Toast.LENGTH_SHORT).show();
                    return true;
                }
                float displayImageHeight = mainActivity.getResources().getDimension(R.dimen.display_image_height);
                float displayImageWidth = mainActivity.getResources().getDimension(R.dimen.display_image_width);
                float scaleX = Constants.IMAGE_WIDTH / displayImageWidth;
                float scaleY = Constants.IMAGE_HEIGHT / displayImageHeight;
                int imageViewX = (int) (event.getX() * scaleX);
                int imageViewY = (int) (event.getY() * scaleY);
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    String args = String.format(Locale.US, Constants.ARGS_SET_SPOTMETER,
                            imageViewX,
                            imageViewX + 1,
                            imageViewY,
                            imageViewY + 1);
                    String cmd = String.format(Constants.CMD_SET_SPOTMETER, args);
                    cameraService.sendCmd(cmd);
                    imageDto.setSpotmeterLocation(new Rect(
                            imageViewX,
                            imageViewY,
                            imageViewX + 1,
                            imageViewY + 1));
                    if (settings.getDisplaySpotmeter().getValue()) {
                        imageDto.setBitmap(imageDto.drawHotspot());
                    }
                }
                cameraViewModel.setRemapNeeded(true);
                drawScreen();
            } else if (v.getId() == R.id.ivColorBar) {
                if (imageDto != null) {
                    int h = binding.ivColorBar.getHeight();
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (event.getY() > (h / 2)) {
                            imageDto.rotateColormap(Constants.ROTATE_FORWARD);
                        } else {
                            imageDto.rotateColormap(Constants.ROTATE_BACKWARD);
                        }
                        mainActivity.invalidateMenu();
                        drawScreen();
                    }
                }
            } else if (v.getId() == R.id.tvMaxTemperature && BuildConfig.DEBUG) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    showFrameRate = !showFrameRate;
                    binding.tvFrameRate.setVisibility(showFrameRate ? View.VISIBLE : View.GONE);
                }
            }
        }

        return true;
    }

    private void setMenuItems(Menu menu) {
        ImageDto imageDto = cameraViewModel.getImageDto().getValue();
        MenuItem itemSave = menu.findItem(R.id.action_save);
        MenuItem itemConnect = menu.findItem(R.id.action_connect);
        MenuItem itemDisconnect = menu.findItem(R.id.action_disconnect);
        MenuItem itemGet = menu.findItem(R.id.action_get);
        MenuItem itemPalette = menu.findItem(R.id.action_palette);
        MenuItem itemStream = menu.findItem(R.id.action_stream);
        MenuItem itemStreamStart = menu.findItem(R.id.action_stream_start);
        MenuItem itemRecordStart = menu.findItem(R.id.action_record_start);
        MenuItem itemStop = menu.findItem(R.id.action_stop);
        SubMenu paletteSubMenu = itemPalette.getSubMenu();

        //Only available in PRO_VERSION
        if(BuildConfig.FLAVOR.equalsIgnoreCase(Constants.FREE_VERSION)) {
            itemRecordStart.setVisible(false);
            itemStreamStart.setTitle(R.string.start);
        }

        if (imageDto != null && !imageDto.getPaletteName().isEmpty()) {
            paletteSubMenu.setHeaderTitle(imageDto.getPaletteName());
        }

        if (settings.getPalette() != null && !settings.getPalette().getValue().isEmpty()) {
            itemPalette.setTitle(settings.getPalette().getValue());
        }

        TypedArray resIds = getActivity().getResources().obtainTypedArray(R.array.palette_ids);

        //since this fragment can be recreated, prevent multiple items
        paletteSubMenu.clear();
        for (int i = 0; i < paletteNames.length; i++) {
            int resId = resIds.getResourceId(i, 0);
            paletteSubMenu.add(Menu.NONE, resId, Menu.NONE, paletteNames[i]);
        }
        resIds.recycle();

        itemPalette.setEnabled(true);
        itemSave.setVisible(true);
        if (cameraViewModel.getStreaming() ||
                imageDto == null || imageDto.getBitmap() == null) {
            itemSave.setEnabled(false); //only true if there is an image
        } else {
            itemSave.setEnabled(true);
        }
        if (cameraService != null && !cameraService.isConnected()) {
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

        if (cameraService != null && !cameraService.isConnected() || cameraViewModel.getStreaming()) {
            itemGet.setEnabled(false);
            itemStreamStart.setVisible(false);
            if(BuildConfig.FLAVOR.equalsIgnoreCase(Constants.PRO_VERSION)) {
                itemRecordStart.setVisible(false);
            }
            itemStop.setVisible(true);
        } else {
            itemGet.setEnabled(true);
            itemStreamStart.setVisible(true);
            if(BuildConfig.FLAVOR.equalsIgnoreCase(Constants.PRO_VERSION)) {
                itemRecordStart.setVisible(true);
            }
            itemStop.setVisible(false);
        }
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
            cameraViewModel.setStreaming(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (cameraViewModel.isInStreamingMode() && cameraService != null) {
            cameraService.startStreaming();
            cameraViewModel.setStreaming(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraViewModel.getStreaming()) {
            cameraService.stopStreaming();
            cameraViewModel.setStreaming(false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraViewModel.getStreaming()) {
            cameraService.stopStreaming();
            cameraViewModel.setStreaming(false);
        }
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }
}

