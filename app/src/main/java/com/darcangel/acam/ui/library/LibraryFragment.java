package com.darcangel.acam.ui.library;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.R;
import com.darcangel.acam.adapters.LibrarySection;
import com.darcangel.acam.constants.Constants;
import com.darcangel.acam.databinding.FragmentLibraryBinding;
import com.darcangel.acam.factory.PaletteFactory;
import com.darcangel.acam.utils.CameraUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;

public class LibraryFragment extends Fragment {

    private FragmentLibraryBinding binding;
    private MainActivity mainActivity;
    private LibraryViewModel libraryViewModel;
    private CameraUtils cameraUtils;

    private AssetManager assetManager;
    private GridLayoutManager gridLayoutManager;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<File> imageFolder;
    private int nFolders = 0;
    private ActivityResultLauncher<Intent> shareActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //ACTION_SEND always returns RESULT_CANCELLED, ignore it
                    // There are no request codes
                    File imagePath = mainActivity.getCacheDir();
                    File newFile = new File(imagePath, Constants.SHARED_IMAGE_FILENAME);
                    if(newFile.exists()) {
                        newFile.delete();
                    }
                }
            });


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if (mainActivity == null) {
            mainActivity = MainActivity.getInstance();
        }
        if (assetManager == null) {
            assetManager = mainActivity.getAssets();
        }
        libraryViewModel = mainActivity.getLibraryViewModel();
        cameraUtils = mainActivity.getCameraUtils();

    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        imageFolder = new ArrayList<File>();
        File dir = mainActivity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File list[] = dir.listFiles();
        imageFolder.addAll(Arrays.asList(list));

        gridLayoutManager = new GridLayoutManager(MainActivity.getInstance(), 1);
        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        binding.rvLibrary.setLayoutManager(gridLayoutManager);


        // Create an instance of SectionedRecyclerViewAdapter
        SectionedRecyclerViewAdapter sectionAdapter = new SectionedRecyclerViewAdapter();


        // Add your Sections only if the directory is not empty
        //  or in the free version only movie files
        try {
            for (int i = 0; i < imageFolder.size(); i++) {
                if (hasImages(imageFolder.get(i).toString())) {
                    sectionAdapter.addSection(new LibrarySection(imageFolder.get(i).toString()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            //TODO handle error
        }

        // Set up your RecyclerView with the SectionedRecyclerViewAdapter
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(final int position) {
                if (sectionAdapter.getSectionItemViewType(position) == SectionedRecyclerViewAdapter.VIEW_TYPE_HEADER) {
                    return 2;
                }
                return 1;
            }
        });
        binding.rvLibrary.setLayoutManager(gridLayoutManager);
        binding.rvLibrary.setAdapter(sectionAdapter);

        View root = binding.getRoot();
        return root;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.library_menu, menu);
        setMenuItems(menu);
    }

    //TODO use LiveData
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // command switch
        switch (item.getItemId()) {
            case R.id.action_item_share:
                shareImage(libraryViewModel.getSelectedImage());
        }
        return true;
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        setMenuItems(menu);
    }

    private void setMenuItems(Menu menu) {
        MenuItem itemDelete = menu.findItem(R.id.action_item_delete);
        MenuItem itemShare = menu.findItem(R.id.action_item_share);
        itemDelete.setVisible(true);
        itemShare.setVisible(true);
    }

    private Boolean hasImages(String imageFolder) {
        File folder = new File(imageFolder);
        String files[] = folder.list();
        //For free version, filter out movies
        String file;
        int count = 0;
        for (int i = 0; i < files.length; i++) {
            file = files[i];
            if (file.substring(file.lastIndexOf(".")).equals(".tjsn")) {
                count++;
            }
        }
        return count > 0;
    }


    private void shareImage(final String filename) {
        try {
            Intent shareIntent = new Intent();
            String tjsnString = cameraUtils.readTjsnFile(filename);
            JSONObject jsonObject = new JSONObject(tjsnString);
            int[][] palette = mainActivity.getPaletteFactory().getPaletteByName("Rainbow");
            Bitmap bitmap = cameraUtils.processImageResponse(jsonObject, palette);
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
                shareIntent.setType(mainActivity.getContentResolver().getType(imageUri));
                shareIntent.setData(imageUri);
                shareIntent.setClipData(ClipData.newRawUri("", imageUri));
                shareIntent.putExtra(Intent.EXTRA_SUBJECT,Constants.SHARED_IMAGE_FILENAME);
                shareIntent.addFlags(
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareActivityResultLauncher.launch(shareIntent);
            }
            //startActivity(Intent.createChooser(shareIntent, null));
        } catch (IOException e) {
            e.printStackTrace();
            //TODO handle error
        }
        catch (JSONException e1) {
            e1.printStackTrace();
        }
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}