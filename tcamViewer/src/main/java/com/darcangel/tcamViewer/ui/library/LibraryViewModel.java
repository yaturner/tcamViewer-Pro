package com.darcangel.tcamViewer.ui.library;

import android.graphics.Bitmap;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.darcangel.tcamViewer.container.SelectedItem;

import java.util.ArrayList;
import java.util.Map;

public class LibraryViewModel extends ViewModel {
    private MutableLiveData<ArrayList<Bitmap>> selectedImages;

    public LibraryViewModel() {
        clearAllSelectedImages();
    }

     public void clearAllSelectedImages() {
        selectedImages = new MutableLiveData<ArrayList<Bitmap>>(new ArrayList<>());
    }

    public MutableLiveData<ArrayList<Bitmap>> getSelectedImages() {
        return selectedImages;
    }

    public void setSelectedImages(ArrayList<Bitmap> selectedImages) {
        this.selectedImages.setValue(selectedImages);
    }
}