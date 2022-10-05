package com.darcangel.tcamViewer.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.ImageView;

        import androidx.annotation.NonNull;
        import androidx.recyclerview.widget.RecyclerView;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;
import com.darcangel.tcamViewer.ui.library.LibrarySlideShowFragment;
import com.darcangel.tcamViewer.utils.CameraUtils;

import java.util.ArrayList;

public class LibrarySlideshowAdapter
        extends RecyclerView.Adapter<LibrarySlideshowAdapter.ViewHolder> {

    // Array of images
    private ArrayList<Bitmap> images;
    private Context ctx;
    private CameraUtils cameraUtils;

    // Constructor of our ViewPager2Adapter class
    public LibrarySlideshowAdapter(Context ctx, ArrayList<Bitmap> images) {
        this.ctx = ctx;
        this.images = images;
        cameraUtils = MainActivity.getInstance().getCameraUtils();
    }

    // This method returns our layout
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(ctx).inflate(R.layout.fragment_slideshow_item, parent, false);
        return new ViewHolder(view);
    }

    // This method binds the screen with the view
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.ivImageView.setImageBitmap(images.get(position));
    }

    // This Method returns the size of the Array
    @Override
    public int getItemCount() {
        return images.size();
    }

    // The ViewHolder class holds the view
    public static class ViewHolder extends RecyclerView.ViewHolder {
        String imageFilename;
        ImageView ivImageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImageView = itemView.findViewById(R.id.ivCamera);
        }
    }
}
