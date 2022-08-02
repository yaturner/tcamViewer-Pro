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

    public LibrarySelectionAdapter() {
        super();
    }

    static public class KeyProvider extends ItemKeyProvider<String> {

        public KeyProvider(RecyclerView.Adapter adapter) {
            super(ItemKeyProvider.SCOPE_MAPPED);
        }

        @Nullable
        @Override
        public String getKey(int position) {
            return null; //JMT (long) position;
        }

        @Override
        public int getPosition(@NonNull String key) {
            String value = key;
            return 0; //JMT (int) value;
        }
    }

    static public class DetailsLookup extends ItemDetailsLookup<String> {

        private RecyclerView recyclerView;

        public DetailsLookup(RecyclerView recyclerView) {
            this.recyclerView = recyclerView;
        }

        @Nullable
        @Override
        public ItemDetails<String> getItemDetails(@NonNull MotionEvent e) {
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

    static public class Predicate extends SelectionTracker.SelectionPredicate<String> {

        @Override
        public boolean canSetStateForKey(@NonNull String key, boolean nextState) {
            return false;
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