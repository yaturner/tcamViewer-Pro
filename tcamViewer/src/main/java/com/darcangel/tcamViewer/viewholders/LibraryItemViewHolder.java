package com.darcangel.tcamViewer.viewholders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.RecyclerView;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;
import com.darcangel.tcamViewer.container.LibraryItemDetails;

public class LibraryItemViewHolder extends RecyclerView.ViewHolder {
    private final ImageView imageView;
    private final TextView titleView;
    private final View rootView;
    private ItemDetailsLookup.ItemDetails<Long> itemDetails;

    private String imagePath;
    private Long position;
    private SelectionTracker selectionTracker;
    private MainActivity mainActivity;

    public LibraryItemViewHolder(@NonNull View itemView, SelectionTracker selectionTracker) {
        super(itemView);
        this.selectionTracker = selectionTracker;

        mainActivity = MainActivity.getInstance();
        imageView = (ImageView) itemView.findViewById(R.id.ivLibraryItem);
        titleView = (TextView) itemView.findViewById(R.id.tvLibraryItemName);
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

    public void setPosition(Long position) {
        this.position = position;
    }

    public boolean isSelected() {
        if (selectionTracker != null) {
            return selectionTracker.isSelected((long) getAbsoluteAdapterPosition());
        } else {
            return false;
        }
    }

    public LibraryItemDetails getItemDetails() {
        return new LibraryItemDetails(position);
    }

    public void bind(final Long position) {
        this.position = (long) getAbsoluteAdapterPosition();
        if(selectionTracker.isSelected(this.position)) {
            imageView.setBackground(mainActivity.getResources().getDrawable(R.drawable.image_border));
            imageView.setActivated(true);
        } else {
            imageView.setBackgroundColor(mainActivity.getResources().getColor(R.color.white, null));
            imageView.setActivated(false);
        }
    }
}