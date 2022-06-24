package com.darcangel.acam.ui.library;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

public class LibraryViewModel extends ViewModel {
    private MutableLiveData<String> selectedImage;

    @Inject
    public LibraryViewModel() {
    }

    public String getSelectedImage() {
        if(selectedImage == null) {
            selectedImage = new MutableLiveData<>("");
        }
        return selectedImage.getValue();
    }

    public void setSelectedImage(String value) {
        if(selectedImage == null) {
            selectedImage = new MutableLiveData<String>("");
        }
        if(!selectedImage.getValue().equals(value)) {
            selectedImage.setValue(value);
        }
    }
}