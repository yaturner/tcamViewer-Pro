package com.darcangel.tcamViewer.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;
import com.darcangel.tcamViewer.model.ImageDto;
import com.darcangel.tcamViewer.utils.CameraUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class LibrarySlideshowAdapter
        extends RecyclerView.Adapter<LibrarySlideshowAdapter.ViewHolder> {
    private static ClickListener clickListener;

    // Array of images
    private final ArrayList<ImageDto> imageDtos;
    private final Context ctx;
    private final CameraUtils cameraUtils;

    // Constructor of our ViewPager2Adapter class
    public LibrarySlideshowAdapter(Context ctx, ArrayList<ImageDto> images) {
        this.ctx = ctx;
        this.imageDtos = images;
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
        ImageDto imageDto = imageDtos.get(holder.getAbsoluteAdapterPosition());
        assert imageDto != null;
        Bitmap bitmap = imageDto.drawHotspot();
        imageDto.remapImage();
        holder.ivImageView.setImageBitmap(bitmap);
        holder.ivImageView.setTag(this);
        String path = imageDto.getFilename();
        String imageName = path.substring(path.lastIndexOf(File.separatorChar) + 1).replace(".tjsn", "");
        holder.position = position;
        holder.tvFilename.setText(imageName);
        holder.tvSpotmeterTemperature.setText(String.format(Locale.US, "%.2f", imageDto.getMeanTemperatureAtSpotmeter()));
        holder.tvSpotmeterTemperature.setTextColor(MainActivity.getInstance().getResources().getColor(R.color.white, null));
        Bitmap colorbar = imageDto.createColorBar();
        holder.ivColorBar.setImageBitmap(colorbar);
        Pair<Float, Float> temps = imageDto.getTemperatures();
        holder.tvMaxTemperature.setText(String.format(Locale.US, "%.2f", temps.second));
        holder.tvMinTemperature.setText(String.format(Locale.US, "%.2f", temps.first));
        holder.imageDto = imageDto;

    }

    // This Method returns the size of the Array
    @Override
    public int getItemCount() {
        return imageDtos.size();
    }

    public void removeItem(final int position) {
        if (position > -1 && position < imageDtos.size()) {
            imageDtos.remove(position);
            notifyDataSetChanged();
        }
    }

    public void setOnItemClickListener(ClickListener clickListener) {
        LibrarySlideshowAdapter.clickListener = clickListener;
    }

    public interface ClickListener {
        void onItemClick(ImageDto imageDto, int position, View v);
        void onItemLongClick(ImageDto imageDto, int position, View v);
    }

    // The ViewHolder class holds the view
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener{
        int position;
        String imageFilename;
        ImageView ivImageView;
        TextView tvSpotmeterTemperature;
        TextView tvMaxTemperature;
        ImageView ivColorBar;
        TextView tvMinTemperature;
        TextView tvFilename;
        ImageDto imageDto;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImageView = itemView.findViewById(R.id.ivCamera);
            tvSpotmeterTemperature = itemView.findViewById(R.id.tvSpotmeterTemperature);
            tvMaxTemperature = itemView.findViewById(R.id.tvMaxTemperature);
            ivColorBar = itemView.findViewById(R.id.ivColorBar);
            tvMinTemperature = itemView.findViewById(R.id.tvMinTemperature);
            tvFilename = itemView.findViewById(R.id.tvFilename);
            ivColorBar.setOnClickListener(this);
            ivColorBar.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            LibrarySlideshowAdapter.clickListener.onItemClick(imageDto, position, v);
        }

        @Override
        public boolean onLongClick(View v) {
            return false;
        }
    }
}
