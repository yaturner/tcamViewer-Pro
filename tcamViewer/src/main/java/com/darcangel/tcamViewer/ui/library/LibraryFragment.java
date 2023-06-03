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
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.darcangel.tcamViewer.BuildConfig;
import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;
import com.darcangel.tcamViewer.adapters.LibrarySelectionAdapter;
import com.darcangel.tcamViewer.constants.Constants;
import com.darcangel.tcamViewer.databinding.FragmentLibraryBinding;
import com.darcangel.tcamViewer.model.ImageDto;
import com.darcangel.tcamViewer.model.Settings;
import com.darcangel.tcamViewer.utils.Utils;
import com.darcangel.tcamViewer.viewholders.LibraryItemViewHolder;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import io.sentry.Sentry;

public class LibraryFragment extends Fragment implements MenuProvider  {

    private FragmentLibraryBinding binding;
    private MainActivity mainActivity;
    private LibraryViewModel libraryViewModel;
    private Utils utils;
    private Settings settings;
    private RecyclerView recyclerView;
    private LibrarySelectionAdapter librarySelectionAdapter;
    private AssetManager assetManager;
    private GridLayoutManager gridLayoutManager;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<File> imageFolder;
    private ArrayList<ImageDto> selectedImages;
    private ArrayList<ImageDto> imageDtos;
    private ArrayList<String> deletedFile;
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
        settings = mainActivity.getSettings();
        utils =mainActivity.getUtils();
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
        selectedImages = new ArrayList<>();
        imageFolder = new ArrayList<File>();
//        imageFile = new ArrayList<String>();

        recyclerView = binding.rvLibrary;
        gridLayoutManager = new GridLayoutManager(mainActivity, 2);

        initDataSet();
        librarySelectionAdapter = new LibrarySelectionAdapter(imageDtos);

        // Set up your RecyclerView with the SectionedRecyclerViewAdapter
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        binding.rvLibrary.setLayoutManager(gridLayoutManager);
        binding.rvLibrary.setAdapter(librarySelectionAdapter);

        selectionTracker = new SelectionTracker.Builder<Long>("librarySelection",
                binding.rvLibrary,
                new LibrarySelectionAdapter.KeyProvider(binding.rvLibrary.getAdapter()),
                new LibrarySelectionAdapter.DetailsLookup(binding.rvLibrary),
                StorageStrategy.createLongStorage())
                .withSelectionPredicate(new LibrarySelectionAdapter.Predicate())
                .build();
        librarySelectionAdapter.setSelectionTracker(selectionTracker);
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

    private void initDataSet() {
        imageDtos = new ArrayList<ImageDto>();
        try {
            File pictureDirectory = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File directoryList[] = pictureDirectory.listFiles();
            File[] imageFileList;
            imageFolder.addAll(Arrays.asList(directoryList));

            for (int iFolder = 0; iFolder < imageFolder.size(); iFolder++) {
                if (hasImages(imageFolder.get(iFolder).toString())) {
                    imageFileList = directoryList[iFolder].listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File pathname) {
                            if(utils.acceptableFiletype(pathname)) {
                                ImageDto imageDto = new ImageDto(pathname.getAbsolutePath(), settings.getPalette().getValue());
                                imageDtos.add(imageDto);
                                return true;
                            } else {
                                return false;
                            }
                        }
                    });
                }
            }
            //sort them
            Collections.sort(imageDtos, (o1, o2) -> {
                //newest first - descending
                return o2.getCreationDate().compareTo(o1.getCreationDate());
            });
        } catch (Exception e) {
            Sentry.captureException(e);
            //TODO handle error
        }
    }

    private Boolean hasImages(String imageFolder) {
        File folder = new File(imageFolder);
        String[] files = folder.list();
        //For free version, filter out movies
        String file;
        int count = 0;
        for (String s : files) {
            file = s;
            if (utils.acceptableFiletype(file)) {
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
                        for(String filename : deletedFile) {
                            File file = new File(filename);
                            if (file.exists()) {
                                file.delete();
                            }
                            String infoFilename = filename.substring(0, filename.lastIndexOf(".")) + ".info";
                            file = new File(infoFilename);
                            if (file.exists()) {
                                file.delete();
                            }
                        }
                        deletedFile.clear();
                    })
                    .show();
        } else {
            Toast.makeText(mainActivity, R.string.nothing_to_selected_to_delete, Toast.LENGTH_LONG).show();
        }
    }

    private void deleteImages(final Selection selection) {
        String filename;
        ArrayList<String> imageFile;
        ArrayList<Integer> keys = new ArrayList<>(selection.size());
        if(deletedFile == null) {
            deletedFile = new ArrayList<String>();
        }
        Iterator<Long> it = selection.iterator();
        while (it.hasNext()) {
            int key = it.next().intValue();
            keys.add(key);
            LibraryItemViewHolder libraryItemViewHolder = (LibraryItemViewHolder) recyclerView.findViewHolderForAdapterPosition(key);
            String path = libraryItemViewHolder.getImagePath();
            deletedFile.add(path);
        }
        keys.sort(Comparator.reverseOrder());
        for (Integer key : keys) {
            librarySelectionAdapter.removeAt(key);
            librarySelectionAdapter.notifyItemRemoved(key);
        }
        deselectAll();
    }

    private void deselectAll() {
        selectionTracker.clearSelection();
        libraryViewModel.clearAllSelectedImages();
    }

    @Override
    public void onDestroyView() {
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
            if (!selection.isEmpty()) {
                int key;
                Iterator<Long> it = selection.iterator();
                while (it.hasNext()) {
                    key = it.next().intValue();
                    LibraryItemViewHolder libraryItemViewHolder = (LibraryItemViewHolder) recyclerView.findViewHolderForAdapterPosition(key);
                    selectedImages.add(libraryItemViewHolder.getImageDto());
                }
                libraryViewModel.setSelectedImages(selectedImages);
                NavDirections navDirections = LibraryFragmentDirections.actionNavigationLibraryToNavigationLibrarySlideShowFragment();
                mainActivity.getNavController().navigate(navDirections);
            } else {
                Toast.makeText(mainActivity, R.string.nothing_selected_to_browse, Toast.LENGTH_LONG).show();
            }
        } else if(id == R.id.action_select_all) {
            int itemCount = binding.rvLibrary.getAdapter().getItemCount();
            ArrayList<Long> keys = new ArrayList<>();
            for(int index = 0; index < itemCount; index++) {
                keys.add((long) index);
            }
            selectionTracker.setItemsSelected(keys, true);
        } else if(id == R.id.action_clear_all) {
            int itemCount = binding.rvLibrary.getAdapter().getItemCount();
            ArrayList<Long> keys = new ArrayList<>();
            for(int index = 0; index < itemCount; index++) {
                keys.add((long) index);
            }
            selectionTracker.setItemsSelected(keys, false);
        } else if(id == R.id.action_sort_ascending) {
            Collections.sort(imageDtos, (o1, o2) -> o1.getCreationDate().compareTo(o2.getCreationDate()));
            ((LibrarySelectionAdapter)binding.rvLibrary.getAdapter()).setImageData(imageDtos);
        } else if(id == R.id.action_sort_ascending) {
            imageDtos.sort(new Comparator<ImageDto>() {
                @Override
                public int compare(ImageDto o1, ImageDto o2) {
                    return o2.getCreationDate().compareTo(o1.getCreationDate());
                }
            });
            ((LibrarySelectionAdapter) binding.rvLibrary.getAdapter()).setImageData(imageDtos);
        }
            return true;
    }

    @Override
    public void onMenuClosed(@NonNull Menu menu) {
        MenuProvider.super.onMenuClosed(menu);
    }

}