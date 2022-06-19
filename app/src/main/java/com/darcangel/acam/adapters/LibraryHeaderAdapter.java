package com.darcangel.acam.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.darcangel.acam.R;

import java.io.StringBufferInputStream;

public class LibraryHeaderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private String[] imageFolder;
    private String folderName;
    private int imageCount;

    public LibraryHeaderAdapter(String[] imageFolder) {
        this.imageFolder = imageFolder;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.library_item_header,parent, false);
        return new LibraryHeaderAdapter.HeaderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        LibraryHeaderAdapter.HeaderViewHolder headerViewHolder = (LibraryHeaderAdapter.HeaderViewHolder) holder;
        headerViewHolder.getTitleView().setText("This is a header");
        headerViewHolder.getCountView().setText("this is a count");
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final TextView countView;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = (TextView) itemView.findViewById(R.id.tvLibraryFolderName);
            countView = (TextView) itemView.findViewById(R.id.tvNumberImages);
        }

        public TextView getTitleView() {
            return titleView;
        }

        public TextView getCountView() {
            return countView;
        }
    }
}