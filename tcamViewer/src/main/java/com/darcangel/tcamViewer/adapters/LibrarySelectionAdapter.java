package com.darcangel.tcamViewer.adapters;

import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.RecyclerView;

import com.darcangel.tcamViewer.viewholders.LibraryItemViewHolder;

import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;

public class LibrarySelectionAdapter extends SectionedRecyclerViewAdapter {
    private SelectionTracker<Long> selectionTracker;

    public LibrarySelectionAdapter() {
        super();
    }

    public void setSelectionTracker(SelectionTracker<Long> selectionTracker) {
        this.selectionTracker = selectionTracker;
    }

    public SelectionTracker<Long> getSelectionTracker() {
        return selectionTracker;
    }

    static public class KeyProvider extends ItemKeyProvider<Long> {
        private LibrarySelectionAdapter adapter;

        public KeyProvider(RecyclerView.Adapter adapter) {
            super(ItemKeyProvider.SCOPE_MAPPED);
            this.adapter = (LibrarySelectionAdapter) adapter;
        }

        @Nullable
        @Override
        public Long getKey(int position) {
            return Long.valueOf(position);
        }

        @Override
        public int getPosition(@NonNull Long key) {
            return key.intValue();
        }
    }

    static public class DetailsLookup extends ItemDetailsLookup<Long> {

        private RecyclerView recyclerView;

        public DetailsLookup(RecyclerView recyclerView) {
            this.recyclerView = recyclerView;
        }

        @Nullable
        @Override
        public ItemDetails<Long> getItemDetails(@NonNull MotionEvent e) {
            View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
            if (view != null) {
                RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(view);
                if (viewHolder instanceof LibraryItemViewHolder) {
                    return ((LibraryItemViewHolder) viewHolder).getItemDetails();
                }
            }
            return null;
        }
    }

    static public class Predicate extends SelectionTracker.SelectionPredicate<Long> {

        @Override
        public boolean canSetStateForKey(@NonNull Long key, boolean nextState) {
            return true;
        }

        @Override
        public boolean canSetStateAtPosition(int position, boolean nextState) {
            return true;
        }

        @Override
        public boolean canSelectMultiple() {
            return true;
        }
    }
}