package com.darcangel.acam.viewholders;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.darcangel.acam.R;

public class LibraryHeaderViewHolder extends RecyclerView.ViewHolder {
    private final TextView titleView;
    private final TextView countView;

    public LibraryHeaderViewHolder(@NonNull View itemView) {
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