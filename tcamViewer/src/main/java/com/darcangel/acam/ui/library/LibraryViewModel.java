package com.darcangel.acam.ui.library;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.darcangel.acam.container.SelectedItem;
import com.darcangel.acam.viewholders.LibraryItemViewHolder;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

public class LibraryViewModel extends ViewModel {
    private MutableLiveData<SelectedItem> selectedImage;

    @Inject
    public LibraryViewModel() {
        if(selectedImage == null) {
            selectedImage = new MutableLiveData<SelectedItem>();
        }
    }

    public void clearSelectedItems() {
        selectedImage = new MutableLiveData<SelectedItem>();
    }

    public SelectedItem getSelectedImage() {
        return selectedImage.getValue();
    }

    public void setSelectedImage(SelectedItem selectedImage) {
        this.selectedImage.setValue(selectedImage);
    }
}