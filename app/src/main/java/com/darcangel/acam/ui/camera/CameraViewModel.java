package com.darcangel.acam.ui.camera;

import android.graphics.Bitmap;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.darcangel.acam.MainActivity;

import io.reactivex.rxjava3.disposables.Disposable;
import timber.log.Timber;

public class CameraViewModel extends ViewModel {

    private MutableLiveData<Boolean> isCameraConnected;
    private MutableLiveData<String> selectedPalette;
    private MutableLiveData<Bitmap> image;
    private CameraService cameraService;

    public CameraViewModel() {
        setIsCameraConnected(false);
        setSelectedPalette("Fusion"); //TODO get from SharedPrefs
        cameraService = MainActivity.getInstance().getCameraService();
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
        if(image == null) {
            
        }
        return image.getValue();
    }

    public MutableLiveData<Bitmap> getImageLiveData() {
        if(image == null) {
            image = new MutableLiveData<Bitmap>(null);
        }
        return image;
    }

    public void setImage(Bitmap image) {
        this.image.setValue(image);
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