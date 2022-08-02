package com.darcangel.tcamViewer.viewholders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.widget.RecyclerView;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;

public class LibraryItemViewHolder
        extends RecyclerView.ViewHolder {
    private final ImageView imageView;
    private final TextView titleView;
    private final View rootView;

    private String imagePath;
    private boolean selected;
    private MainActivity mainActivity;

    public LibraryItemViewHolder(@NonNull View itemView) {
        super(itemView);
        mainActivity = MainActivity.getInstance();
        imageView = (ImageView) itemView.findViewById(R.id.ivLibraryItem);
        titleView = (TextView) itemView.findViewById(R.id.tvLibraryItemName);
        selected = false;
        rootView = itemView;
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

    public View getRootView() {
        return rootView;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public LibraryItemDetails getItemDetails() {
        return new LibraryItemDetails();
    }

    static class LibraryItemDetails extends ItemDetailsLookup.ItemDetails<String> {
        private boolean selected;
        private String key;

        public int getPosition() {
            return 0;
        }

        public String getSelectionKey() {
            return null;
        }
    }
}