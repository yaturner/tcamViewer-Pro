package com.darcangel.tcamViewer.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.darcangel.tcamViewer.constants.Constants;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUtils {
    // Method to generate a filename based on date and time
    public static String generateFileName() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return "VIDEO_" + timeStamp + ".mp4";
    }

    // Method to save the video file in the video directory using MediaStore
    public static Uri saveVideoFile(ContentResolver contentResolver, File videoFile) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.getName());
        contentValues.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES);

        return contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
    }

    public static String generateNewFilename(boolean isMovie) {
        Date now = new Date();
        if (isMovie) {
            return "mov_" + Constants.simpleDateFormatFile.format(now);
        } else {
            return "img_" + Constants.simpleDateFormatFile.format(now);
        }
    }

    public static String generateNewPath() {
        Date now = new Date();
        return Constants.simpleDateFormatFolder.format(now);
    }
}
