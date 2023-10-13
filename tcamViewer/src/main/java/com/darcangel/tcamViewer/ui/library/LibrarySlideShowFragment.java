package com.darcangel.tcamViewer.ui.library;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;
import com.darcangel.tcamViewer.adapters.LibrarySlideshowAdapter;
import com.darcangel.tcamViewer.constants.Constants;
import com.darcangel.tcamViewer.databinding.FragmentLibrarySlideshowBinding;
import com.darcangel.tcamViewer.model.ImageDto;
import com.darcangel.tcamViewer.model.LibraryViewModel;
import com.darcangel.tcamViewer.model.RecordingDto;
import com.darcangel.tcamViewer.model.Settings;
import com.darcangel.tcamViewer.utils.CameraUtils;
import com.darcangel.tcamViewer.utils.FileUtils;
import com.darcangel.tcamViewer.utils.Utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;

import io.sentry.Sentry;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;


public class LibrarySlideShowFragment extends Fragment implements MenuProvider {
    private ViewGroup container;
    private ViewPager2 viewPager;
    private ArrayList<ImageDto> imageDtos;
    private LibrarySlideshowAdapter slideshowAdapter;
    private FragmentLibrarySlideshowBinding binding;
    private LibraryViewModel libraryViewModel;
    private MainActivity mainActivity;
    private Settings settings;
    private CameraUtils cameraUtils;
    private Utils utils;
    private View root;
    private int sharedImagePosition = -1;
    private int tab;

    private final ActivityResultLauncher<Intent> saveActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Timber.d("\\\\result = %s", result.toString());
                    if (result.getResultCode() == RESULT_OK) {
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
                        String[] words = filename.split("\\.");
                        if(words.length != 2  || !words[1].startsWith("tjsn"))
                        {
                            Toast.makeText(getContext(), R.string.filetype_not_mtjsn, Toast.LENGTH_LONG)
                                    .show();

                        } else {
                            try {
                                int position = binding.vpSlideshow.getCurrentItem();
                                ImageDto imageDto = imageDtos.get(position);
                                String tjsnString = imageDto.getTjsnString();
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

    private final ActivityResultLauncher<Intent> shareActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //ACTION_SEND always returns RESULT_CANCELLED, ignore it
                    // There are no request codes
                    File imagePath = mainActivity.getCacheDir();
                    if (sharedImagePosition != -1) {
                        File newFile = new File(imagePath, imageDtos.get(sharedImagePosition).getFilename());
                        if (newFile.exists()) {
                            newFile.delete();
                        }
                        sharedImagePosition = -1;
                    }
                }
            });


    public LibrarySlideShowFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = MainActivity.getInstance();
        settings = mainActivity.getSettings();
        cameraUtils = mainActivity.getCameraUtils();
        utils = mainActivity.getUtils();
        libraryViewModel = mainActivity.getLibraryViewModel();
        imageDtos = libraryViewModel.getSelectedImages().getValue();
        slideshowAdapter = new LibrarySlideshowAdapter(getContext(), imageDtos);

        slideshowAdapter.setOnTouchListener(new LibrarySlideshowAdapter.TouchListener() {
            @Override
            public void onTouch(ImageDto imageDto, View v, MotionEvent event) {
                if(imageDto == null) {
                    return;
                }
                int h = v.getHeight();
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getY() > (h / 2)) {
                        imageDto.rotateColormap(Constants.ROTATE_FORWARD);
                    } else {
                        imageDto.rotateColormap(Constants.ROTATE_BACKWARD);
                    }
                    v.getRootView().invalidate();
                    slideshowAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.container = container;

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        binding = FragmentLibrarySlideshowBinding.inflate(inflater, container, false);
        binding.vpSlideshow.setAdapter(slideshowAdapter);
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tab = binding.vpSlideshow.getCurrentItem();
        setNavigationArrows(tab);
        // Images left navigation
        binding.leftNav.setOnClickListener(v ->{
                tab = binding.vpSlideshow.getCurrentItem();
                if (tab > 0) {
                    tab--;
                    binding.vpSlideshow.setCurrentItem(tab);
                } else if (tab == 0) {
                    binding.vpSlideshow.setCurrentItem(tab);
                }
                setNavigationArrows(tab);
                mainActivity.invalidateOptionsMenu();
            });

        // Images right navigation
        binding.rightNav.setOnClickListener(v -> {
            tab = binding.vpSlideshow.getCurrentItem();
            tab++;
            binding.vpSlideshow.setCurrentItem(tab);
            setNavigationArrows(tab);
            mainActivity.invalidateOptionsMenu();
        });
    }

    private void setNavigationArrows(int tab) {
        //set the arrows visibility
        int nImages = binding.vpSlideshow.getAdapter().getItemCount();
        if(nImages == 1) {
            binding.leftNav.setVisibility(View.GONE);
            binding.rightNav.setVisibility(View.GONE);
        } else if(tab == 0) {
            binding.leftNav.setVisibility(View.GONE);
            binding.rightNav.setVisibility(View.VISIBLE);
        } else if(tab == nImages - 1) {
            binding.rightNav.setVisibility(View.GONE);
            binding.leftNav.setVisibility(View.VISIBLE);
        } else {
            binding.rightNav.setVisibility(View.VISIBLE);
            binding.leftNav.setVisibility(View.VISIBLE);
        }
    }

    private void shareImage(final int position) {
        //save this for the result callback
        sharedImagePosition = position;
        ImageDto imageDto = imageDtos.get(position);
        Bitmap sharedBitmap = utils.createExportImage(imageDto);
        if (sharedBitmap != null) {
            getSharedFilename(imageDto, sharedBitmap);
        }
    }

    private void getSharedFilename(final ImageDto imageDto, final Bitmap sharedBitmap) {
        final ConstraintLayout layout = (ConstraintLayout) mainActivity.getLayoutInflater().
                inflate(R.layout.dialog_enter_share_filename, null);
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                    .setCancelable(true)
                    .setMessage(R.string.enter_name_for_shared_image)
                    .setNegativeButton(R.string.cancel, (dlg, which) -> {
                        dlg.dismiss();
                    })
                    .setPositiveButton(R.string.ok, (dlg, which) -> {
                        EditText et = layout.findViewById(R.id.etFilename);
                        String filename = et.getText().toString();
                        if(filename.isEmpty()) {
                            Toast.makeText(getContext(), R.string.you_must_specify_filename,
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        File imagePath = mainActivity.getCacheDir();
                        File newFile = new File(imagePath, filename + ".png");
                        if (newFile.exists()) {
                            newFile.delete();
                        }
                        try {
                            FileUtils.saveBitmapToFile(sharedBitmap, newFile);
                        } catch (IOException e) {
                            Sentry.captureException(e);
                        }

                        Uri imageUri = FileProvider.getUriForFile(mainActivity,
                                "com.darcangel.fileprovider", newFile);
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("*/*");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                        shareIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{""});
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, newFile.getName());
                        shareIntent.addFlags(
                                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        shareActivityResultLauncher.launch(shareIntent);
                        dlg.dismiss();
                    })
                    .setTitle("Share Image")
                    .setView(layout);
            builder.create().show();
    }

    private void deleteImage(final int position) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
            ImageDto imageDto = imageDtos.get(position);
            builder.setTitle("Confirm Deletion")
                    .setMessage("Are you sure you want to permanently remove this image")
                    .setNegativeButton("Cancel", (dlg, which) -> {
                        dlg.dismiss();
                    })
                    .setPositiveButton("OK", (dlg, which) -> {
                        slideshowAdapter.removeItem(position);
                        String filename = imageDto.getFilename();
                        File file = new File(filename);
                        if (file.exists()) {
                            file.delete();
                        }
                        String infoFilename = filename.substring(0, filename.lastIndexOf(".")) + ".info";
                        file = new File(infoFilename);
                        if (file.exists()) {
                            file.delete();
                        }
                        file = null;
                        if (slideshowAdapter.getItemCount() == 0) {
                            NavDirections navDirections = LibrarySlideShowFragmentDirections.actionLibrarySlideShowFragmentToNavigationLibrary();
                            mainActivity.getNavController().navigate(navDirections);
                        } else {
                            binding.vpSlideshow.setAdapter(slideshowAdapter);
                        }
                        setNavigationArrows(binding.vpSlideshow.getCurrentItem());
                    })
                    .show();
    }



    private void setMenuItems(Menu menu) {
        MenuItem itemDelete = menu.findItem(R.id.action_item_delete);
        MenuItem itemSlideShow = menu.findItem(R.id.action_item_share);
        MenuItem itemRecording = menu.findItem(R.id.action_recording);
        MenuItem itemPlay = menu.findItem(R.id.action_movie_play);
//        MenuItem itemAnalyze = menu.findItem(R.id.action_movie_analyze);
        MenuItem itemSave = menu.findItem(R.id.action_movie_save);
        if(binding.vpSlideshow != null) {
            int position = binding.vpSlideshow.getCurrentItem();
            itemRecording.setEnabled(imageDtos.get(position).isMovie());
        }
    }

    @Override
    public void onPrepareMenu(@NonNull Menu menu) {
        MenuProvider.super.onPrepareMenu(menu);
        MenuItem save = menu.findItem(R.id.action_movie_save);
        if(mainActivity.isRunningOnEmulator()) {
            save.setEnabled(false);
            save.setVisible(false);
        }
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.library_slideshow_item_menu, menu);
        setMenuItems(menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        // command switch
        int position = binding.vpSlideshow.getCurrentItem();
        int id = menuItem.getItemId();
        NavDirections navDirections;
        //delete image
        if (id == R.id.action_item_delete) {
            deleteImage(position);
            return true;
        } else if (id == R.id.action_item_share) {
            //share image
            shareImage(position);
            return true;
        } else if (id == R.id.action_item_export) {
            //export image
            try {
                ImageDto imageDto = imageDtos.get(position);
                utils.exportImage(imageDto);
            } catch (IOException e) {
                e.printStackTrace();
                Sentry.captureException(e);
            }
            return true;
        } else if (id == R.id.action_item_export_tjsn) {
            String imageName, imageDirectory;
            Date now = new Date();
            imageDirectory = Constants.simpleDateFormatFolder.format(now);
            imageName = Constants.simpleDateFormatFile.format(now);
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
            return true;
        } else if (id == android.R.id.home) {
            navDirections = LibrarySlideShowFragmentDirections.actionLibrarySlideShowFragmentToNavigationLibrary();
            mainActivity.getNavController().navigate(navDirections);
            return true;
        } else if(id == R.id.action_movie_play) {
            xferToPlayback(Constants.PLAYBACK_ACTION_PLAY, position);
            return true;
        } else if(id == R.id.action_movie_save) {
            xferToPlayback(Constants.PLAYBACK_ACTION_SAVE, position);
            return true;
        } else {
            return false;
        }
    }

    private void xferToPlayback(Integer action, Integer position) {
        ImageDto imageDto = imageDtos.get(position);
        if(imageDto == null) {
            //TODO handle error
        }
        libraryViewModel.setPlaybackImageDto(imageDto);
        libraryViewModel.setRecordingDto(new RecordingDto());
        libraryViewModel.resetFrameOffset();
        libraryViewModel.resetFrameSize();
        libraryViewModel.resetFrameDelay();
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.PLAYBACK_ACTION, action);
        Navigation.findNavController(getView()).navigate(R.id.action_navigation_librarySlideShowFragment_to_playbackFragment, bundle);
    }

    @Override
    public void onMenuClosed(@NonNull Menu menu) {
        MenuProvider.super.onMenuClosed(menu);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}

