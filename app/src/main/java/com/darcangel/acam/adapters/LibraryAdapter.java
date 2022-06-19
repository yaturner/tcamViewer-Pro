package com.darcangel.acam.adapters;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.darcangel.acam.MainActivity;
import com.darcangel.acam.R;
import com.darcangel.acam.container.Settings;
import com.darcangel.acam.utils.CameraUtils;
import com.darcangel.acam.viewholders.LibraryItemViewHolder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LibraryAdapter extends RecyclerView.Adapter<LibraryItemViewHolder> {
    private int itemCount = 0;
    private CameraUtils cameraUtils;
    private AssetManager assetManager;
    private MainActivity mainActivity;
    private Settings settings;
    private String[] imageFolder;
    private String[] imageFile;
    private int nFolders = 0;

    public LibraryAdapter() {
        mainActivity = MainActivity.getInstance();
        cameraUtils = mainActivity.getCameraUtils();
        assetManager = mainActivity.getAssets();
        settings = mainActivity.getSettings();

        try {
            imageFolder = assetManager.list("test_images");
            nFolders = imageFolder.length;
            imageFile = assetManager.list("test_images/"+imageFolder[2]);
            itemCount = imageFile.length;
        } catch(IOException e) {
            //TODO handle error
            e.printStackTrace();
        }
    }
    @NonNull
    @Override
    public LibraryItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.library_item_view,parent, false);
        return new LibraryItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LibraryItemViewHolder holder, int position) {
        String json = new String();
        String line;
        Pattern pattern = Pattern.compile("^img_([0-9_]*)\\.tjsn$");

        try {
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(assetManager.open("test_images/"+imageFolder[2] + "/" + imageFile[position])));
            do {
                line = bufferedReader.readLine();
                if (line != null) {
                    json = json + line;
                }
            } while(line != null);
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
                holder.getImageView().setImageBitmap(image);
                Matcher matcher = pattern.matcher(imageFile[position]);
                if(matcher.find()) {
                    holder.getTitleView().setText(matcher.group(1));
                } else {
                    holder.getTitleView().setText("");
                }
            } catch (JSONException e) {
                e.printStackTrace();
                //TODO handle error
            }
        }
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }
}