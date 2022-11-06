package com.darcangel.tcamViewer.adapters;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.RecyclerView;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;
import com.darcangel.tcamViewer.model.ImageDto;
import com.darcangel.tcamViewer.model.Settings;
import com.darcangel.tcamViewer.utils.CameraUtils;
import com.darcangel.tcamViewer.viewholders.LibraryHeaderViewHolder;
import com.darcangel.tcamViewer.viewholders.LibraryItemViewHolder;

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
import timber.log.Timber;

public class LibrarySection extends Section {
    private ArrayList<String> imageFile;
    private ArrayList<ImageDto> imageDtos;
    private final String imageFolder;
    private final SelectionTracker<Long> selectionTracker;

    private final AssetManager assetManager;
    private final MainActivity mainActivity;
    private final CameraUtils cameraUtils;
    private final Settings settings;
    private int itemCount;

    private final Pattern PATTERN = Pattern.compile("\\.*img_([0-9_]*)\\.tjsn$");


    public LibrarySection(String imageFolder, SelectionTracker<Long> selectionTracker) {
        // call constructor with layout resources for this Section header and items
        super(SectionParameters.builder()
                .itemResourceId(R.layout.library_item_view)
                .headerResourceId(R.layout.library_item_header)
                .build());
        this.imageFolder = imageFolder;
        this.selectionTracker = selectionTracker;

        mainActivity = MainActivity.getInstance();
        cameraUtils = mainActivity.getCameraUtils();
        assetManager = mainActivity.getAssets();
        settings = mainActivity.getSettings();

        imageDtos = new ArrayList<ImageDto>();

        try {
            imageFile = new ArrayList<>();
            File folder = new File(imageFolder);
            String[] files = folder.list();
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
        return new LibraryItemViewHolder(view, selectionTracker);
    }

    @Override
    public void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position) {
        LibraryItemViewHolder itemHolder = (LibraryItemViewHolder) holder;
        String json = "";
        String line;
        String  imageName;
        String path = imageFile.get(position);
        ImageDto imageDto;

        imageName = path.substring(path.lastIndexOf(File.separatorChar)+1);
        imageDto = new ImageDto(path, settings.getPalette().getValue());
        imageDtos.add(position, imageDto);
        Bitmap image = imageDto.getBitmap();
        if(itemHolder.isSelected()) {
            itemHolder.getImageView().setBackground(mainActivity.getResources().
                    getDrawable(R.drawable.library_item_highlight_selector, null));
        } else {
            itemHolder.getImageView().setBackgroundColor(mainActivity.getResources().
                    getColor(R.color.white,null));

        }
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
        Timber.d("\\\\onBindItemViewHolder\\\\ title = %s, position = %d, selected = %s",
                itemHolder.getTitleView().getText(), position, (itemHolder.isSelected()?"true":"false"));


        itemHolder.bind(Long.valueOf(position));
    }

    @Override
    public void onBindHeaderViewHolder(final RecyclerView.ViewHolder holder) {
        final LibraryHeaderViewHolder headerHolder = (LibraryHeaderViewHolder) holder;
        String dateString = imageFolder.substring(imageFolder.lastIndexOf(File.separatorChar)+1).replace("tcam_", "");
        headerHolder.getTitleView().setText(dateString);
        headerHolder.getCountView().setText("" + imageFile.size() + " image(s)");
    }

    public void deleteItem(final int pos) {
        imageFile.remove(pos);
    }

    @Override
    public RecyclerView.ViewHolder getHeaderViewHolder(View view) {
        return new LibraryHeaderViewHolder(view);
    }

    public ArrayList<String> getImageFile() {
        return imageFile;
    }

    public String getImageFolder() {
        return imageFolder;
    }

    public ArrayList<ImageDto> getImages() {
        return imageDtos;
    }
}