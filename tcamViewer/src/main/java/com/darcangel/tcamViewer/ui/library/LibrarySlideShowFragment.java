package com.darcangel.tcamViewer.ui.library;

import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavDirections;
import androidx.viewpager2.widget.ViewPager2;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;
import com.darcangel.tcamViewer.adapters.LibrarySlideshowAdapter;
import com.darcangel.tcamViewer.constants.Constants;
import com.darcangel.tcamViewer.databinding.FragmentLibrarySlideshowBinding;
import com.darcangel.tcamViewer.model.ImageDto;
import com.darcangel.tcamViewer.model.Settings;
import com.darcangel.tcamViewer.utils.CameraUtils;
import com.darcangel.tcamViewer.utils.Utils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import timber.log.Timber;


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
    private BottomNavigationView navBar;
    private View root;

    private ActivityResultLauncher<Intent> shareActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //ACTION_SEND always returns RESULT_CANCELLED, ignore it
                    // There are no request codes
                    File imagePath = mainActivity.getCacheDir();
                    File newFile = new File(imagePath, Constants.SHARED_IMAGE_FILENAME);
                    if (newFile.exists()) {
                        newFile.delete();
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
        this.imageDtos = libraryViewModel.getSelectedImages().getValue();
        slideshowAdapter = new LibrarySlideshowAdapter(getContext(), imageDtos);
        slideshowAdapter.setOnItemClickListener(new LibrarySlideshowAdapter.ClickListener() {
            @Override
            public void onItemClick(ImageDto imageDto, int position, View v) {
                Timber.d("clicked on colorbar");
                imageDto.rotateColormap();
                v.getRootView().invalidate();
                //slideshowAdapter.notifyDataSetChanged();
                slideshowAdapter.notifyItemChanged(position);
            }

            @Override
            public void onItemLongClick(ImageDto imageDto, int position, View v) {

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
        navBar = getActivity().findViewById(R.id.nav_view);
        if (navBar != null) {
            navBar.setVisibility(View.GONE);
        }

    }

    private void shareImage(final int position) {
        ImageDto imageDto = imageDtos.get(position);
        try {
            Intent shareIntent = new Intent();
            Bitmap bitmap = utils.createExportImage(imageDto);
            File imagePath = mainActivity.getCacheDir();
            File newFile = new File(imagePath, Constants.SHARED_IMAGE_FILENAME);
            if (bitmap != null) {
                if (newFile.exists()) {
                    newFile.delete();
                }
                cameraUtils.saveBitmapToFile(bitmap, newFile);
                Uri imageUri = FileProvider.getUriForFile(mainActivity, "com.darcangel.fileprovider", newFile);
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                shareIntent.setDataAndType(imageUri, mainActivity.getContentResolver().getType(imageUri));
                shareIntent.setClipData(ClipData.newRawUri("", imageUri));
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, Constants.SHARED_IMAGE_FILENAME);
                shareIntent.addFlags(
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareActivityResultLauncher.launch(shareIntent);
            }
        } catch (IOException e) {
            e.printStackTrace();
            //TODO handle error
        }
    }


    private ContentValues contentValues() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        return values;
    }

    private void deleteImage(final int position) {
        slideshowAdapter.removeItem(position);
        if (slideshowAdapter.getItemCount() == 0) {
            NavDirections navDirections = LibrarySlideShowFragmentDirections.actionLibrarySlideShowFragmentToNavigationLibrary();
            mainActivity.getNavController().navigate(navDirections);
        } else {
            binding.vpSlideshow.setAdapter(slideshowAdapter);
        }
    }

    private void setMenuItems(Menu menu) {
        MenuItem itemDelete = menu.findItem(R.id.action_item_delete);
        MenuItem itemSlideShow = menu.findItem(R.id.action_item_share);
    }


    @Override
    public void onPrepareMenu(@NonNull Menu menu) {
        MenuProvider.super.onPrepareMenu(menu);
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
        if (id == R.id.action_item_delete) {
            deleteImage(position);
            return true;
        } else if (id == R.id.action_item_share) {
            shareImage(position);
            return true;
        } else if (id == R.id.action_item_export) {
            try {
                ImageDto imageDto = imageDtos.get(position);
                utils.exportImage(imageDto);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return true;
        } else if (id == android.R.id.home) {
            NavDirections navDirections = LibrarySlideShowFragmentDirections.actionLibrarySlideShowFragmentToNavigationLibrary();
            mainActivity.getNavController().navigate(navDirections);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onMenuClosed(@NonNull Menu menu) {
        MenuProvider.super.onMenuClosed(menu);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        libraryViewModel.clearAllSelectedImages();
        if (navBar != null) {
            navBar.setVisibility(View.VISIBLE);
        }
    }
}

