package com.darcangel.acam.ui.library;

import android.app.appsearch.StorageInfo;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.adapters.LibraryAdapter;
import com.darcangel.acam.adapters.LibraryHeaderAdapter;
import com.darcangel.acam.adapters.LibrarySection;
import com.darcangel.acam.databinding.FragmentLibraryBinding;
import com.darcangel.acam.network.CameraSocketIO;

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
    private LibraryAdapter libraryAdapter;
    private LibraryHeaderAdapter libraryHeaderAdapter;

    private ArrayList<String> imageFolder;
    private ArrayList<String> imageFile;
    private int nFolders = 0;

    Pattern PATTERN = Pattern.compile("^img_([0-9_]*)\\.tjsn$");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        if (mainActivity == null) {
            mainActivity = MainActivity.getInstance();
        }
        if(assetManager == null) {
            assetManager = mainActivity.getAssets();
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        LibraryViewModel libraryViewModel = mainActivity.getLibraryViewModel();

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        binding = FragmentLibraryBinding.inflate(inflater, container, false);

        try {
            imageFile = new ArrayList<String>();
            imageFolder = new ArrayList<String>();
            int itemCount = 0;
            imageFolder.addAll(Arrays.asList(assetManager.list("test_images")));
            for(String folder : imageFolder) {
                String[] files = assetManager.list("test_images/" + folder);
                for (int iFile = 0; iFile < files.length; iFile++) {
                    imageFile.add("test_images/" + folder + "/" + files[iFile]);
                    itemCount++;
                }
            }
        } catch(IOException e) {
            //TODO handle error
            e.printStackTrace();
        }

        gridLayoutManager = new GridLayoutManager(MainActivity.getInstance(),1);
        binding.rvLibrary.setLayoutManager(gridLayoutManager);
        libraryAdapter = new LibraryAdapter(imageFile);
        libraryHeaderAdapter = new LibraryHeaderAdapter(null);
        // Create an instance of SectionedRecyclerViewAdapter
        SectionedRecyclerViewAdapter sectionAdapter = new SectionedRecyclerViewAdapter();


        // Add your Sections
        for(int i = 0; i < imageFolder.size(); i++) {
            sectionAdapter.addSection(new LibrarySection(imageFolder.get(i)));
        }

        // Set up your RecyclerView with the SectionedRecyclerViewAdapter
        binding.rvLibrary.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.rvLibrary.setAdapter(sectionAdapter);

        View root = binding.getRoot();
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}