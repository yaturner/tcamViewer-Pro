package com.darcangel.tcamViewer.ui.settings;

import android.content.ContentValues;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModel;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;
import com.darcangel.tcamViewer.model.ImageDto;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class SettingsViewModel extends ViewModel {

    //Hints
    private String[] emissivityString;
    private int[] emissivityValue;

    public SettingsViewModel() {
        init();
    }

    private void init() {
        //set the default values

        if (emissivityString == null || emissivityString.length == 0) {
            emissivityString = MainActivity.getInstance().getResources().getStringArray(R.array.emissivity_strings);
        }
        if (emissivityValue == null || emissivityValue.length == 0) {
            emissivityValue = MainActivity.getInstance().getResources().getIntArray(R.array.emissivity_values);
        }

    }

    //misc
    @Override
    public void onCleared() {
        // Dispose All your Subscriptions to avoid memory leaks
        super.onCleared();
    }
}