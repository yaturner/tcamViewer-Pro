package com.darcangel.acam.viewholders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.R;

public class LibraryItemViewHolder extends RecyclerView.ViewHolder {
    private final ImageView imageView;
    private final TextView titleView;

    public LibraryItemViewHolder(@NonNull View itemView) {
        super(itemView);
        imageView = (ImageView) itemView.findViewById(R.id.ivLibraryItem);
        titleView = (TextView) itemView.findViewById(R.id.tvLibraryFolderName);

        itemView.setOnClickListener((v) -> {
            MainActivity.getInstance().runOnUiThread(() -> {
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
}