package com.darcangel.tcamViewer.ui.library;

import android.graphics.Bitmap;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.darcangel.tcamViewer.model.ImageDto;

import java.util.ArrayList;

public class LibraryViewModel extends ViewModel {
    private MutableLiveData<ArrayList<ImageDto>> selectedImages;

    public LibraryViewModel() {
        clearAllSelectedImages();
    }

    public void clearAllSelectedImages() {
        selectedImages = new MutableLiveData<ArrayList<ImageDto>>(new ArrayList<>());
    }

    public MutableLiveData<ArrayList<ImageDto>> getSelectedImages() {
        return selectedImages;
    }

    public void setSelectedImages(ArrayList<ImageDto> selectedImages) {
        this.selectedImages.setValue(selectedImages);
    }
}