package com.darcangel.acam.adapters;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.darcangel.acam.R;
import com.darcangel.acam.viewholders.LibraryItemViewHolder;

import java.util.Arrays;
import java.util.List;

import io.github.luizgrp.sectionedrecyclerviewadapter.Section;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;
import io.github.luizgrp.sectionedrecyclerviewadapter.utils.EmptyViewHolder;

public class LibrarySection extends Section {
    List<String> itemList = Arrays.asList("Item1", "Item2", "Item3");

    public LibrarySection() {
        // call constructor with layout resources for this Section header and items
        super(SectionParameters.builder()
                .itemResourceId(R.layout.library_item_view)
                .headerResourceId(R.layout.library_item_header)
                .build());
    }

    @Override
    public int getContentItemsTotal() {
        return itemList.size(); // number of items of this section
    }

    @Override
    public RecyclerView.ViewHolder getItemViewHolder(View view) {
        // return a custom instance of ViewHolder for the items of this section
        return new LibraryItemViewHolder(view);
    }

    @Override
    public void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position) {
        LibraryItemViewHolder itemHolder = (LibraryItemViewHolder) holder;

        // bind your view here
        //itemHolder.tvItem.setText(itemList.get(position));
    }

    @Override
    public RecyclerView.ViewHolder getHeaderViewHolder(View view) {
        // return an empty instance of ViewHolder for the headers of this section
        return new EmptyViewHolder(view);
    }
}