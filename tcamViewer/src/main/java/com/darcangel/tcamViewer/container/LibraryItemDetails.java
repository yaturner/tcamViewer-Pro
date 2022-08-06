package com.darcangel.tcamViewer.container;

import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;

public class LibraryItemDetails extends ItemDetailsLookup.ItemDetails<String> {
    private String key;
    private int position;

    public LibraryItemDetails(final int position, final String key) {
        this.key = key;
        this.position = position;
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Nullable
    @Override
    public String getSelectionKey() {
        return key;
    }

    @Override
    public boolean inSelectionHotspot(@NonNull MotionEvent e) {
        return true;
    }
}