package com.darcangel.tcamViewer.container;

import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;

public class LibraryItemDetails extends ItemDetailsLookup.ItemDetails<Long> {
    private int position;

    public LibraryItemDetails(final int position) {
        this.position = position;
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Nullable
    @Override
    public Long getSelectionKey() {
        return Long.valueOf(position);
    }

    @Override
    public boolean inSelectionHotspot(@NonNull MotionEvent e) {
        return true;
    }
}