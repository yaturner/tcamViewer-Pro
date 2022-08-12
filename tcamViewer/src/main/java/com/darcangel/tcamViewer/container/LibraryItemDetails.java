package com.darcangel.tcamViewer.container;

import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;

public class LibraryItemDetails extends ItemDetailsLookup.ItemDetails<Long> {
    private Long position;

    public LibraryItemDetails(final Long position) {
        this.position = position;
    }

    @Override
    public int getPosition() {
        return position.intValue();
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