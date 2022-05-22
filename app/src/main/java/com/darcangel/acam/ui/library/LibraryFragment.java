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

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.databinding.FragmentLibraryBinding;
import com.darcangel.acam.network.CameraSocketIO;

public class LibraryFragment extends Fragment {

    private FragmentLibraryBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        LibraryViewModel libraryViewModel =
                new ViewModelProvider(this).get(LibraryViewModel.class);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textLibrary;
        libraryViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}