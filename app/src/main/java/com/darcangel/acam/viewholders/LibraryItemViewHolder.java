package com.darcangel.acam.viewholders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.R;
import com.darcangel.acam.ui.library.LibraryFragment;
import com.darcangel.acam.ui.library.LibraryViewModel;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

public class LibraryItemViewHolder extends RecyclerView.ViewHolder {
    private final ImageView imageView;
    private final TextView titleView;
    private String imagePath;
    private boolean selected = false;
    private MainActivity mainActivity;

    public LibraryItemViewHolder(@NonNull View itemView) {
        super(itemView);
        mainActivity = MainActivity.getInstance();
        imageView = (ImageView) itemView.findViewById(R.id.ivLibraryItem);
        titleView = (TextView) itemView.findViewById(R.id.tvLibraryFolderName);

        itemView.setOnClickListener((v) -> {
            mainActivity.runOnUiThread(() -> {
                if(!selected) {
                    selected = true;
                    imageView.setBackground(MainActivity.getInstance().getResources().
                            getDrawable(R.drawable.image_border, null));
                    mainActivity.getLibraryViewModel().setSelectedImage(imagePath);
                } else {
                    selected = false;
                    imageView.setBackground(null);
                    mainActivity.getLibraryViewModel().setSelectedImage("");
                }
                String msg = String.format("image %s clicked", titleView.getText());
                Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_LONG).show();
            });
        });
    }

    public ImageView getImageView() {
        return imageView;
    }

    public TextView getTitleView() {
        return titleView;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}