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
import com.darcangel.tcamViewer.ui.library.LibrarySlideShowFragment;
import com.darcangel.tcamViewer.utils.CameraUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class LibrarySlideshowAdapter
        extends RecyclerView.Adapter<LibrarySlideshowAdapter.ViewHolder> {

    // Array of images
    private ArrayList<ImageDto> imageDtos;
    private Context ctx;
    private CameraUtils cameraUtils;

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
        ImageDto imageDto = imageDtos.get(position);
        assert imageDto != null;
        Bitmap bitmap = imageDto.drawHotspot();
        holder.ivImageView.setImageBitmap(bitmap);
        String path = imageDto.getFilename();
        String imageName = path.substring(path.lastIndexOf(File.separatorChar)+1).replace(".tjsn", "");
        holder.tvFilename.setText(imageName);
        holder.tvSpotmeterTemperature.setText(String.format("%.2f", imageDto.getMeanTemperatureAtSpotmeter()));
        holder.tvSpotmeterTemperature.setTextColor(MainActivity.getInstance().getResources().getColor(R.color.white, null));
        Bitmap colorbar = imageDto.createColorBar();
        holder.ivColorBar.setImageBitmap(colorbar);
        Pair<Float, Float> temps = imageDto.getTemperatures();
        holder.tvMaxTemperature.setText(String.format("%.2f", temps.second));
        holder.tvMinTemperature.setText(String.format("%.2f", temps.first));
    }

    // This Method returns the size of the Array
    @Override
    public int getItemCount() {
        return imageDtos.size();
    }

    // The ViewHolder class holds the view
    public static class ViewHolder extends RecyclerView.ViewHolder {
        String imageFilename;
        ImageView ivImageView;
        TextView tvSpotmeterTemperature;
        TextView tvMaxTemperature;
        ImageView ivColorBar;
        TextView tvMinTemperature;
        TextView tvFilename;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImageView = itemView.findViewById(R.id.ivCamera);
            tvSpotmeterTemperature = itemView.findViewById(R.id.tvSpotmeterTemperature);
            tvMaxTemperature = itemView.findViewById(R.id.tvMaxTemperature);
            ivColorBar = itemView.findViewById(R.id.ivColorBar);
            tvMinTemperature = itemView.findViewById(R.id.tvMinTemperature);
            tvFilename = itemView.findViewById(R.id.tvFilename);
        }
    }
}
