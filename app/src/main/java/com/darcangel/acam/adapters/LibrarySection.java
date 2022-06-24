package com.darcangel.acam.adapters;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.R;
import com.darcangel.acam.container.Settings;
import com.darcangel.acam.utils.CameraUtils;
import com.darcangel.acam.viewholders.LibraryHeaderViewHolder;
import com.darcangel.acam.viewholders.LibraryItemViewHolder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.luizgrp.sectionedrecyclerviewadapter.Section;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;

public class LibrarySection extends Section {
    ArrayList<String> imageFile;
    String imageFolder;
    AssetManager assetManager;
    MainActivity mainActivity;
    CameraUtils cameraUtils;
    Settings settings;
    int itemCount;
    Pattern PATTERN = Pattern.compile("\\.*img_([0-9_]*)\\.tjsn$");


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
        settings = mainActivity.getSettings();

        try {
            imageFile = new ArrayList<>();
            File folder = new File(imageFolder);
            String files[] = folder.list();
            //For free version, filter out movies
            String file;
            for(int i = 0; i < files.length; i++) {
                file = files[i];
                if(file.substring(file.lastIndexOf(".")).equals(".tjsn")) {
                    imageFile.add(folder + "/" + file);
                }
            }
        } catch (Exception e) {
            //TODO handle error
            e.printStackTrace();
        }
    }

    @Override
    public int getContentItemsTotal() {
        return imageFile.size();
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
        String  imageName;
        String path = imageFile.get(position);

        imageName = path.substring(path.lastIndexOf(File.separatorChar)+1);
        try {
            BufferedReader bufferedReader = new BufferedReader(
                    new FileReader(new File(path)));
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
                        mainActivity.getPaletteFactory().getPaletteByName(settings.getPalette()));
                itemHolder.getImageView().setImageBitmap(image);
                itemHolder.setImagePath(path);
                if (imageName != null && !imageName.isEmpty()) {
                    Matcher matcher = PATTERN.matcher(imageName);
                    if(matcher.find()) {
                        itemHolder.getTitleView().setText(matcher.group(1));
                    } else {
                        itemHolder.getTitleView().setText("");
                    }
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
        String dateString = imageFolder.substring(imageFolder.lastIndexOf(File.separatorChar)+1).replace("tcam_", "");
        headerHolder.getTitleView().setText(dateString);
        headerHolder.getCountView().setText("" + imageFile.size() + " image(s)");
    }

    @Override
    public RecyclerView.ViewHolder getHeaderViewHolder(View view) {
        return new LibraryHeaderViewHolder(view);
    }
}