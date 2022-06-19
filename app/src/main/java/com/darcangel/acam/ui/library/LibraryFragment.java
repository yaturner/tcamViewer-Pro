package com.darcangel.acam.ui.library;

import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.os.Bundle;
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
    private ArrayList<String> imageFolder;
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
            imageFolder = new ArrayList<String>();
            imageFolder.addAll(Arrays.asList(assetManager.list("test_images")));
        } catch(IOException e) {
            //TODO handle error
            e.printStackTrace();
        }

        gridLayoutManager = new GridLayoutManager(MainActivity.getInstance(),1);
        binding.rvLibrary.setLayoutManager(gridLayoutManager);


        // Create an instance of SectionedRecyclerViewAdapter
        SectionedRecyclerViewAdapter sectionAdapter = new SectionedRecyclerViewAdapter();


        // Add your Sections
        for(int i = 0; i < imageFolder.size(); i++) {
            sectionAdapter.addSection(new LibrarySection(imageFolder.get(i)));
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}