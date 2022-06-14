package com.darcangel.acam.ui.camera;

import android.graphics.Bitmap;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CameraViewModel extends ViewModel {

    private MutableLiveData<Boolean> isCameraConnected;
    private MutableLiveData<String> selectedPalette;
    private Bitmap image;

    public CameraViewModel() {
        setIsCameraConnected(false);
        setSelectedPalette("Fusion"); //TODO get from SharedPrefs
    }

    public Boolean getIsCameraConnected() {
        if(isCameraConnected == null) {
            isCameraConnected = new MutableLiveData<>();
        }
        return isCameraConnected.getValue();
    }

    public void setIsCameraConnected(Boolean value) {
        if(isCameraConnected == null) {
            isCameraConnected = new MutableLiveData<>();
        }
        isCameraConnected.postValue(value);
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public String getSelectedPalette() {
        if(selectedPalette == null) {
            selectedPalette = new MutableLiveData<>();
        }
        return selectedPalette.getValue();
    }

    public void setSelectedPalette(String value) {
        if(selectedPalette == null) {
            selectedPalette = new MutableLiveData<>();
        }
        selectedPalette.setValue(value);
    }
}