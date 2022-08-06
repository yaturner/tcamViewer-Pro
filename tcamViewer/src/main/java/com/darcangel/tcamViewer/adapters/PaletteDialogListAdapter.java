package com.darcangel.tcamViewer.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;

public class PaletteDialogListAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private Context context;
    private String[] paletteStirng;
    private MainActivity mainActivity;

    public PaletteDialogListAdapter(Context context) {
        this.context = context;
        mainActivity = MainActivity.getInstance();
        paletteStirng = mainActivity.getResources().getStringArray(R.array.palette_names);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    @Override
    public int getCount() {
        return paletteStirng.length;
    }

    @Override
    public String getItem(int position) {
        return paletteStirng[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View listItem = inflater.inflate(R.layout.palette_list_item, parent, false);
        TextView tvPalette = listItem.findViewById(R.id.tvPalette);
        tvPalette.setTypeface(Typeface.MONOSPACE);
        tvPalette.setText(paletteStirng[position]);
        return tvPalette;

    }
}
