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

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.databinding.FragmentLibraryBinding;
import com.darcangel.acam.network.CameraSocketIO;

public class LibraryFragment extends Fragment {

    private FragmentLibraryBinding binding;
    private MainActivity mainActivity = MainActivity.getInstance();
    private GridLayoutManager gridLayoutManager;

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
        gridLayoutManager = new GridLayoutManager(MainActivity.getInstance(),2);
        binding.rvLibrary.setLayoutManager(gridLayoutManager);



        View root = binding.getRoot();
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}