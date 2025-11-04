package com.darcangel.tcamViewer.adapters;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.RecyclerView;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;
import com.darcangel.tcamViewer.constants.Constants;
import com.darcangel.tcamViewer.model.ImageDto;
import com.darcangel.tcamViewer.model.Settings;
import com.darcangel.tcamViewer.utils.CameraUtils;
import com.darcangel.tcamViewer.viewholders.LibraryItemViewHolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LibrarySelectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener {
    private SelectionTracker<Long> selectionTracker;
    private ArrayList<ImageDto> imageDtos;

    private final AssetManager assetManager;
    private final MainActivity mainActivity;
    private final CameraUtils cameraUtils;
    private final Settings settings;
    private int itemCount;

    private final Pattern PATTERN = Pattern.compile("\\.*img_([0-9_]*)\\.tjsn$");

    public LibrarySelectionAdapter(ArrayList<ImageDto> imageDtos) {
        super();
        this.imageDtos = imageDtos;
        mainActivity = MainActivity.getInstance();
        cameraUtils = mainActivity.getCameraUtils();
        assetManager = mainActivity.getAssets();
        settings = mainActivity.getSettings();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mainActivity.getLayoutInflater().inflate(R.layout.library_item_view, parent, false);
        return new LibraryItemViewHolder(view, selectionTracker);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        LibraryItemViewHolder itemHolder = (LibraryItemViewHolder) holder;
        String json = "";
        String line;
        String  imageName;
        ImageDto imageDto = imageDtos.get(position);
        String path = imageDto.getFilename();
        String[] words = path.split("/");
        int nWords = words.length;
        String imageDate = words[nWords-2].replace("_", "/");
        imageName = path.substring(path.lastIndexOf(File.separatorChar)+1);
        Bitmap image = imageDto.getBitmap();
        itemHolder.getImageView().setImageBitmap(image);
        itemHolder.setImagePath(path);
        itemHolder.setImageDto(imageDto);
        if (!imageName.isEmpty()) {
            Matcher matcher = PATTERN.matcher(imageName);
            if(matcher.find()) {
                String imageTime = matcher.group(1).replace("_", ":");
                itemHolder.getTitleView().setText(imageDate + '\n' + imageTime);
            } else {
                itemHolder.getTitleView().setText("");
            }
        } else {
            itemHolder.getTitleView().setText("");
        }
//        Timber.d("\\\\onBindItemViewHolder\\\\ title = %s, position = %d, selected = %s",
//                itemHolder.getTitleView().getText(), position, (itemHolder.isSelected()?"true":"false"));


        //TODO this is the position within the section
        itemHolder.bind(Long.valueOf(position));

    }

    @Override
    public int getItemCount() {
        return imageDtos.size();
    }

    public void setSelectionTracker(SelectionTracker<Long> selectionTracker) {
        this.selectionTracker = selectionTracker;
    }

    public SelectionTracker<Long> getSelectionTracker() {
        return selectionTracker;
    }

    @Override
    public void onClick(View v) {

    }

    public void removeAt(int position) {
        imageDtos.remove(position);
        notifyItemRemoved(position);
    }

    public void setImageData(ArrayList<ImageDto> imageDtos) {
        this.imageDtos = imageDtos;
        notifyDataSetChanged();
    }

    static public class KeyProvider extends ItemKeyProvider<Long> {
        private LibrarySelectionAdapter adapter;

        public KeyProvider(RecyclerView.Adapter adapter) {
            super(ItemKeyProvider.SCOPE_MAPPED);
            this.adapter = (LibrarySelectionAdapter) adapter;
        }

        private void sortImageDto(ArrayList<ImageDto> imageDtos, final int order) {
            Collections.sort(imageDtos, new Comparator<ImageDto>() {
                @Override
                public int compare(ImageDto o1, ImageDto o2) {
                    if (order == Constants.SORT_ORDER_ASCENDING) {
                        return o1.getCreationDate().compareTo(o2.getCreationDate());
                    } else {
                        return o2.getCreationDate().compareTo(o1.getCreationDate());
                    }
                }
            });
        }

        @Nullable
        @Override
        public Long getKey(int position) {
            return Long.valueOf(position);
        }

        @Override
        public int getPosition(@NonNull Long key) {
            long value = key;
            return (int)value;
        }
    }

    static public class DetailsLookup extends ItemDetailsLookup<Long> {

        private RecyclerView recyclerView;

        public DetailsLookup(RecyclerView recyclerView) {
            this.recyclerView = recyclerView;
        }

        @Nullable
        @Override
        public ItemDetails<Long> getItemDetails(@NonNull MotionEvent e) {
            View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
            if (view != null) {
                RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(view);
                if (viewHolder instanceof LibraryItemViewHolder) {
                    return ((LibraryItemViewHolder) viewHolder).getItemDetails();
                }
            }
            return null;
        }
    }

    static public class Predicate extends SelectionTracker.SelectionPredicate<Long> {

        @Override
        public boolean canSetStateForKey(@NonNull Long key, boolean nextState) {
            return true;
        }

        @Override
        public boolean canSetStateAtPosition(int position, boolean nextState) {
            return true;
        }

        @Override
        public boolean canSelectMultiple() {
            return true;
        }
    }
}