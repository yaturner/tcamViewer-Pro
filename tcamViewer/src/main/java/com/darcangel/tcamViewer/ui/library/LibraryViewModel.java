package com.darcangel.tcamViewer.ui.library;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.darcangel.tcamViewer.container.SelectedItem;

import java.util.Map;

public class LibraryViewModel extends ViewModel {
    private MutableLiveData<SelectedItem> selectedImage;
    private MutableLiveData<Map<String, SelectedItem>> selectedImageMap;

    public LibraryViewModel() {
        clearAllSelectedImages();
    }

     public void clearAllSelectedImages() {
        selectedImage = new MutableLiveData<SelectedItem>();
        selectedImageMap = new MutableLiveData<Map<String, SelectedItem>>();
    }

    public MutableLiveData<SelectedItem> getSelectedImage() {
        return selectedImage;
    }

    public void setSelectedImage(MutableLiveData<SelectedItem> selectedImage) {
        this.selectedImage = selectedImage;
    }

    public MutableLiveData<Map<String, SelectedItem>> getSelectedImageMap() {
        return selectedImageMap;
    }

    public void setSelectedImageMap(MutableLiveData<Map<String, SelectedItem>> selectedImageMap) {
        this.selectedImageMap = selectedImageMap;
    }
}