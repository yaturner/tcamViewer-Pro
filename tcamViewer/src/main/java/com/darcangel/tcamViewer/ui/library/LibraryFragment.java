package com.darcangel.tcamViewer.ui.library;

import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavDirections;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.SelectionTracker.SelectionObserver;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;
import com.darcangel.tcamViewer.adapters.LibrarySection;
import com.darcangel.tcamViewer.adapters.LibrarySelectionAdapter;
import com.darcangel.tcamViewer.databinding.FragmentLibraryBinding;
import com.darcangel.tcamViewer.model.ImageDto;
import com.google.android.material.navigation.NavigationBarView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;

public class LibraryFragment extends Fragment implements MenuProvider  {

    private FragmentLibraryBinding binding;
    private MainActivity mainActivity;
    private LibraryViewModel libraryViewModel;
    private LibrarySelectionAdapter sectionAdapter;
    private ArrayList<LibrarySection> librarySections;

    private AssetManager assetManager;
    private GridLayoutManager gridLayoutManager;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<File> imageFolder;
    private ArrayList<ImageDto> selectedImages;

    private View root;
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
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        binding = FragmentLibraryBinding.inflate(inflater, container, false);

        initRecyclerView();

        root = binding.getRoot();
        return root;
    }

    private void initRecyclerView() {
        librarySections = new ArrayList<>();
        selectedImages = new ArrayList<>();
        imageFolder = new ArrayList<File>();

        gridLayoutManager = new GridLayoutManager(mainActivity, 1);

        File dir = mainActivity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File list[] = dir.listFiles();
        imageFolder.addAll(Arrays.asList(list));

        // Create an instance of SectionedRecyclerViewAdapter
        sectionAdapter = new LibrarySelectionAdapter();

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

        selectionTracker = new SelectionTracker.Builder<Long>("librarySelection",
                binding.rvLibrary,
                new LibrarySelectionAdapter.KeyProvider(binding.rvLibrary.getAdapter()),
                new LibrarySelectionAdapter.DetailsLookup(binding.rvLibrary),
                StorageStrategy.createLongStorage())
                .withSelectionPredicate(new LibrarySelectionAdapter.Predicate())
                .build();

        selectionTracker.addObserver(new SelectionObserver<Long>() {
            @Override
            public void onItemStateChanged(@NonNull Long key, boolean selected) {
                super.onItemStateChanged(key, selected);
                LibrarySection section = (LibrarySection) sectionAdapter.getSectionForPosition(key.intValue());
                if(selected) {
                    selectionTracker.select(key);
                } else {
                    selectionTracker.deselect(key);
                }
            }

            @Override
            public void onSelectionChanged() {
                super.onSelectionChanged();
            }
        });

        // Add your Sections only if the directory is not empty
        //  or in the free version only movie files
        librarySections.clear();
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void setMenuItems(Menu menu) {
        MenuItem itemDelete = menu.findItem(R.id.action_item_delete);
        MenuItem itemSlideShow = menu.findItem(R.id.action_slideshow);
        itemDelete.setEnabled(true);
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
        if (!selection.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
            builder.setTitle("Confirm Deletion")
                    .setMessage("Are you sure you want to permanently remove these image(s)")
                    .setNegativeButton("Cancel", (dlg, which) -> {
                        dlg.dismiss();
                    })
                    .setPositiveButton("OK", (dlg, which) -> {
                        deleteImages(selection);
                    })
                    .show();
        } else {
            Toast.makeText(mainActivity, R.string.nothing_to_delete, Toast.LENGTH_LONG).show();
        }
    }

    private void deleteImages(final Selection selection) {
        String filename;
        ArrayList<String> imageFile;
        ArrayList<Integer> keys = new ArrayList<>(selection.size());
        Iterator<Long> it = selection.iterator();
        while (it.hasNext()) {
            keys.add(it.next().intValue());
        }
        keys.sort(Comparator.reverseOrder());
        for (Integer key : keys) {
            int position = sectionAdapter.getPositionInSection(key);
            LibrarySection section = (LibrarySection) sectionAdapter.getSectionForPosition(key);
            imageFile = section.getImageFile();
            filename = imageFile.get(position);
            File file = new File(filename);
            if (file.exists()) {
                file.delete();
            }
            section.deleteItem(position);
            sectionAdapter.notifyItemRemoved(key);
        }
        deselectAll();
        initRecyclerView();
        root.requestLayout();
    }

    private void deselectAll() {
        selectionTracker.clearSelection();
        libraryViewModel.clearAllSelectedImages();
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
        deselectAll();
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
            //Toast.makeText(mainActivity, selectionTracker.getSelection().toString(), Toast.LENGTH_LONG).show();
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