package com.darcangel.acam.ui.library;

import android.content.pm.ActivityInfo;
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

import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;

public class LibraryFragment extends Fragment {

    private FragmentLibraryBinding binding;
    private MainActivity mainActivity = MainActivity.getInstance();
    private GridLayoutManager gridLayoutManager;
    private RecyclerView.LayoutManager layoutManager;
    private LibraryAdapter libraryAdapter;
    private LibraryHeaderAdapter libraryHeaderAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        LibraryViewModel libraryViewModel = mainActivity.getLibraryViewModel();

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        gridLayoutManager = new GridLayoutManager(MainActivity.getInstance(),1);
        binding.rvLibrary.setLayoutManager(gridLayoutManager);
        libraryAdapter = new LibraryAdapter();
        libraryHeaderAdapter = new LibraryHeaderAdapter(null);
        // Create an instance of SectionedRecyclerViewAdapter
        SectionedRecyclerViewAdapter sectionAdapter = new SectionedRecyclerViewAdapter();

        // Add your Sections
        sectionAdapter.addSection(new LibrarySection());

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