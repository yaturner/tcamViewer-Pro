package com.darcangel.acam.adapters;

import android.graphics.Bitmap;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.darcangel.acam.R;
import com.darcangel.acam.viewholders.LibraryItemViewHolder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

import io.github.luizgrp.sectionedrecyclerviewadapter.Section;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;
import io.github.luizgrp.sectionedrecyclerviewadapter.utils.EmptyViewHolder;

public class LibrarySection extends Section {
    ArrayList<String> itemList;

    public LibrarySection(ArrayList<String> itemList) {
        // call constructor with layout resources for this Section header and items
        super(SectionParameters.builder()
                .itemResourceId(R.layout.library_item_view)
                .headerResourceId(R.layout.library_item_header)
                .build());
        this.itemList = itemList;
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
        String json = new String();
        String line;

        try {
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(assetManager.open(imageFile.get(position))));
            do {
                line = bufferedReader.readLine();
                if (line != null) {
                    json = json + line;
                }
            } while (line != null);
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            json = "";
        }
        if (!json.isEmpty()) {
            try {
                JSONObject jsonObject = new JSONObject(json);
                Bitmap image = cameraUtils.processImageResponse(jsonObject,
                        mainActivity.getPaletteFactory().getPaletteByName("Rainbow"));
                holder.getImageView().setImageBitmap(image);
                Matcher matcher = PATTERN.matcher(imageFile.get(position));
                if (matcher.find()) {
                    holder.getTitleView().setText(matcher.group(1));
                } else {
                    holder.getTitleView().setText("");
                }
            } catch (JSONException e) {
                e.printStackTrace();
                //TODO handle error
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder getHeaderViewHolder(View view) {
        // return an empty instance of ViewHolder for the headers of this section
        return new EmptyViewHolder(view);
    }
}