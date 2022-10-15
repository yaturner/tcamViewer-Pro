package com.darcangel.tcamViewer.ui.library;

import android.content.ClipData;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;
import com.darcangel.tcamViewer.adapters.LibrarySection;
import com.darcangel.tcamViewer.adapters.LibrarySelectionAdapter;
import com.darcangel.tcamViewer.constants.Constants;
import com.darcangel.tcamViewer.databinding.FragmentLibraryBinding;
import com.darcangel.tcamViewer.model.ImageDto;
import com.darcangel.tcamViewer.ui.settings.SettingsFragmentDirections;
import com.darcangel.tcamViewer.utils.CameraUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;

public class LibraryFragment extends Fragment implements MenuProvider {

    private FragmentLibraryBinding binding;
    private MainActivity mainActivity;
    private LibraryViewModel libraryViewModel;
    private CameraUtils cameraUtils;
    private LibrarySelectionAdapter sectionAdapter;
    private ArrayList<LibrarySection> librarySections;

    private AssetManager assetManager;
    private GridLayoutManager gridLayoutManager;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<File> imageFolder;
    private ArrayList<ImageDto> selectedImages;

    private int nFolders = 0;

    private SelectionTracker<Long> selectionTracker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        if (mainActivity == null) {
            mainActivity = MainActivity.getInstance();
        }
        if (assetManager == null) {
            assetManager = mainActivity.getAssets();
        }
        libraryViewModel = mainActivity.getLibraryViewModel();
        cameraUtils = mainActivity.getCameraUtils();
        librarySections = new ArrayList<>();
        selectedImages  = new ArrayList<>();
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

        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        // Create an instance of SectionedRecyclerViewAdapter
        sectionAdapter = new LibrarySelectionAdapter(); ///SectionedRecyclerViewAdapter

        // Set up your RecyclerView with the SectionedRecyclerViewAdapter
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(final int position) {
                if (sectionAdapter.getSectionItemViewType(position) == SectionedRecyclerViewAdapter.VIEW_TYPE_HEADER) {
                    return 2;
                } else {
                    return 1;
                }
            }
        });
        binding.rvLibrary.setLayoutManager(gridLayoutManager);
        binding.rvLibrary.setAdapter(sectionAdapter);

        View root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        selectionTracker = new SelectionTracker.Builder<Long>("librarySelection",
                binding.rvLibrary,
                new LibrarySelectionAdapter.KeyProvider(binding.rvLibrary.getAdapter()),
                new LibrarySelectionAdapter.DetailsLookup(binding.rvLibrary),
                StorageStrategy.createLongStorage())
                .withSelectionPredicate(new LibrarySelectionAdapter.Predicate())
                .build();

        // Add your Sections only if the directory is not empty
        //  or in the free version only movie files
        try {
            for (int iFolder = 0; iFolder < imageFolder.size(); iFolder++) {
                if (hasImages(imageFolder.get(iFolder).toString())) {
                    LibrarySection section = new LibrarySection(imageFolder.get(iFolder).toString(), selectionTracker);
                    librarySections.add(section);
                    sectionAdapter.addSection(section);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            //TODO handle error
        }

        sectionAdapter.setSelectionTracker(selectionTracker);
    }

    private void setMenuItems(Menu menu) {
        MenuItem itemDelete = menu.findItem(R.id.action_item_delete);
        MenuItem itemSlideShow = menu.findItem(R.id.action_slideshow);
        if (selectionTracker != null && selectionTracker.hasSelection()) {
            itemDelete.setEnabled(true);
        } else {
            itemDelete.setEnabled(false);
        }
        itemSlideShow.setEnabled(true);
    }

    private Boolean hasImages(String imageFolder) {
        File folder = new File(imageFolder);
        String files[] = folder.list();
        //For free version, filter out movies
        String file;
        int count = 0;
        for (int iFile = 0; iFile < files.length; iFile++) {
            file = files[iFile];
            if (file.substring(file.lastIndexOf(".")).equals(".tjsn")) {
                count++;
            }
        }
        return count > 0;
    }


    private void deleteImage(final Selection selection) {
        int key;
        String filename;
        ArrayList<String> imageFile;
        if (!selection.isEmpty()) {
            Iterator<Long> it = selection.iterator();
            while (it.hasNext()) {
                key = it.next().intValue();
                int position = sectionAdapter.getPositionInSection(key);
                LibrarySection section = (LibrarySection) sectionAdapter.getSectionForPosition(key);
                imageFile = section.getImageFile();
                filename = imageFile.get(position);
                File file = new File(filename);
                if (file.exists()) {
                    //file.delete();
                }
                section.deleteItem(position);
                sectionAdapter.notifyItemRemoved(position);
            }
        }
    }

    private void exportImage() {

    }

    @Override
    public void onDestroyView() {
        libraryViewModel.clearAllSelectedImages();
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        selectedImages = new ArrayList<>();
        for (Long key : selectionTracker.getSelection()) {
            selectionTracker.deselect(key);
        }
    }

    @Override
    public void onPrepareMenu(@NonNull Menu menu) {
        MenuProvider.super.onPrepareMenu(menu);
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.library_menu, menu);
        setMenuItems(menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        // command switch
        int id = menuItem.getItemId();
        Selection<Long> selection = selectionTracker.getSelection();
        if (id == R.id.action_item_delete) {
            deleteImage(selection);
        } else if (id == R.id.action_slideshow) {
            Toast.makeText(mainActivity, selectionTracker.getSelection().toString(), Toast.LENGTH_LONG).show();
            if (!selection.isEmpty()) {
                int key, position;
                ArrayList<String> imageFile;
                Iterator<Long> it = selection.iterator();
                while (it.hasNext()) {
                    key = it.next().intValue();
                    position = sectionAdapter.getPositionInSection(key);
                    LibrarySection section = (LibrarySection) sectionAdapter.getSectionForPosition(key);
                    ImageDto imageDto = section.getImages().get(position);
                    selectedImages.add(imageDto);
                }
                libraryViewModel.setSelectedImages(selectedImages);
                NavDirections navDirections = LibraryFragmentDirections.actionNavigationLibraryToNavigationLibrarySlideShowFragment();
                mainActivity.getNavController().navigate(navDirections);
            }
        }
        return true;
    }

    @Override
    public void onMenuClosed(@NonNull Menu menu) {
        MenuProvider.super.onMenuClosed(menu);
    }

}