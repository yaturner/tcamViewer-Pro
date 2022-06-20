package com.darcangel.acam.adapters;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.R;
import com.darcangel.acam.utils.CameraUtils;
import com.darcangel.acam.viewholders.LibraryHeaderViewHolder;
import com.darcangel.acam.viewholders.LibraryItemViewHolder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.luizgrp.sectionedrecyclerviewadapter.Section;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;
import io.github.luizgrp.sectionedrecyclerviewadapter.utils.EmptyViewHolder;

public class LibrarySection extends Section {
    ArrayList<String> imageFile;
    String imageFolder;
    AssetManager assetManager;
    MainActivity mainActivity;
    CameraUtils cameraUtils;
    int itemCount;
    Pattern PATTERN = Pattern.compile("^img_([0-9_]*)\\.tjsn$");


    public LibrarySection(String imageFolder) {
        // call constructor with layout resources for this Section header and items
        super(SectionParameters.builder()
                .itemResourceId(R.layout.library_item_view)
                .headerResourceId(R.layout.library_item_header)
                .build());
        this.imageFolder = imageFolder;

        mainActivity = MainActivity.getInstance();
        cameraUtils = mainActivity.getCameraUtils();
        assetManager = mainActivity.getAssets();
        try {
            imageFile = new ArrayList<String>();
            itemCount = 0;
            String[] files = assetManager.list("test_images/" + imageFolder);
            for (int iFile = 0; iFile < files.length; iFile++) {
                imageFile.add("test_images/" + imageFolder + "/" + files[iFile]);
                itemCount++;
            }
        } catch (IOException e) {
            //TODO handle error
            e.printStackTrace();
        }
    }

    @Override
    public int getContentItemsTotal() {
        return itemCount;
    }

    @Override
    public RecyclerView.ViewHolder getItemViewHolder(View view) {
        // return a custom instance of ViewHolder for the items of this section
        return new LibraryItemViewHolder(view);
    }

    @Override
    public void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position) {
        LibraryItemViewHolder itemHolder = (LibraryItemViewHolder) holder;

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
                itemHolder.getImageView().setImageBitmap(image);
                Matcher matcher = PATTERN.matcher(imageFile.get(position));
                if (matcher.find()) {
                    itemHolder.getTitleView().setText(matcher.group(1));
                } else {
                    itemHolder.getTitleView().setText("");
                }
            } catch (JSONException e) {
                e.printStackTrace();
                //TODO handle error
            }
        }
    }

    @Override
    public void onBindHeaderViewHolder(final RecyclerView.ViewHolder holder) {
        final LibraryHeaderViewHolder headerHolder = (LibraryHeaderViewHolder) holder;
        String dateString = imageFolder.replace("tcam_", "");
        headerHolder.getTitleView().setText(dateString);
        headerHolder.getCountView().setText(""+itemCount+" image(s)");
    }

    @Override
    public RecyclerView.ViewHolder getHeaderViewHolder(View view) {
        return new LibraryHeaderViewHolder(view);
    }
}