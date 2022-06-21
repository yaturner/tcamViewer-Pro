package com.darcangel.acam.ui.library;

import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.adapters.LibrarySection;
import com.darcangel.acam.databinding.FragmentLibraryBinding;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;

public class LibraryFragment extends Fragment {

    private FragmentLibraryBinding binding;
    private MainActivity mainActivity;
    private AssetManager assetManager;
    private GridLayoutManager gridLayoutManager;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<File> imageFolder;
    private int nFolders = 0;

    Pattern PATTERN = Pattern.compile("^img_([0-9_]*)\\.tjsn$");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if (mainActivity == null) {
            mainActivity = MainActivity.getInstance();
        }
        if (assetManager == null) {
            assetManager = mainActivity.getAssets();
        }
        LibraryViewModel libraryViewModel = mainActivity.getLibraryViewModel();

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




    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}