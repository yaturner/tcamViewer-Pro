package com.darcangel.tcamViewer.ui.library;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.darcangel.tcamViewer.model.ImageDto;

import java.util.ArrayList;

public class LibraryViewModel extends ViewModel {
    private MutableLiveData<ArrayList<ImageDto>> selectedImages;
    private ImageDto playbackImageDto;
    private ArrayList<Long> frameOffset;
    private ArrayList<Integer> frameSize;


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

    public ImageDto getPlaybackImageDto() {
        return playbackImageDto;
    }

    public void setPlaybackImageDto(ImageDto playbackImageDto) {
        this.playbackImageDto = playbackImageDto;
    }

    public ArrayList<Long> getFrameOffset() {
        return frameOffset;
    }

    public void resetFrameOffset() {
        this.frameOffset = new ArrayList<Long>();
    }

    public ArrayList<Integer> getFrameSize() {
        return frameSize;
    }

    public void resetFrameSize() {
        this.frameSize = new ArrayList<Integer>();
    }
}