package com.darcangel.tcamViewer.ui.library;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavDirections;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;
import com.darcangel.tcamViewer.adapters.LibrarySlideshowAdapter;
import com.darcangel.tcamViewer.constants.Constants;
import com.darcangel.tcamViewer.databinding.FragmentLibrarySlideshowBinding;
import com.darcangel.tcamViewer.model.ImageDto;
import com.darcangel.tcamViewer.model.Settings;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Objects;

import timber.log.Timber;


public class LibrarySlideShowFragment extends Fragment implements MenuProvider, View.OnClickListener {
    private ViewGroup container;
    private ViewPager2 viewPager;
    private ArrayList<ImageDto> imageDtos;
    private LibrarySlideshowAdapter slideshowAdapter;
    private FragmentLibrarySlideshowBinding binding;
    private LibraryViewModel libraryViewModel;
    private MainActivity mainActivity;
    private Settings settings;
    private BottomNavigationView navBar;
    private View root;

    private ActivityResultLauncher<Intent> shareActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //ACTION_SEND always returns RESULT_CANCELLED, ignore it
                    // There are no request codes
                    File imagePath = mainActivity.getCacheDir();
                    File newFile = new File(imagePath, Constants.SHARED_IMAGE_FILENAME);
                    if (newFile.exists()) {
                        newFile.delete();
                    }
                }
            });


    public LibrarySlideShowFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = MainActivity.getInstance();
        settings = mainActivity.getSettings();
        libraryViewModel = mainActivity.getLibraryViewModel();
        this.imageDtos = libraryViewModel.getSelectedImages().getValue();
        slideshowAdapter = new LibrarySlideshowAdapter(getContext(), imageDtos);
        slideshowAdapter.setOnItemClickListener(new LibrarySlideshowAdapter.ClickListener() {
            @Override
            public void onItemClick(ImageDto imageDto, int position, View v) {
                Timber.d("clicked on colorbar");
                imageDto.rotateColormap();
                v.getRootView().invalidate();
                //slideshowAdapter.notifyDataSetChanged();
                slideshowAdapter.notifyItemChanged(position);
            }

            @Override
            public void onItemLongClick(ImageDto imageDto, int position, View v) {

            }
        });
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.container = container;

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        binding = FragmentLibrarySlideshowBinding.inflate(inflater, container, false);
        binding.vpSlideshow.setAdapter(slideshowAdapter);
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navBar = getActivity().findViewById(R.id.nav_view);
        if(navBar != null) {
            navBar.setVisibility(View.GONE);
        }

    }

    private void shareImage(final int position) {
        String filename = "";
        String tjsnString = "";
        JSONObject jsonObject = null;
        ImageDto imageDto = imageDtos.get(position);
        try {
            filename = imageDto.getFilename();
            Intent shareIntent = new Intent();
            jsonObject = imageDto.getJsonObject();
            Bitmap bitmap = imageDto.getBitmap();
            File imagePath = mainActivity.getCacheDir();
            File newFile = new File(imagePath, Constants.SHARED_IMAGE_FILENAME);
            if (bitmap != null) {
                if (newFile.exists()) {
                    newFile.delete();
                }
                imageDto.saveBitmapToFile(newFile);
                Uri imageUri = FileProvider.getUriForFile(mainActivity, "com.darcangel.fileprovider", newFile);
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                shareIntent.setType(mainActivity.getContentResolver().getType(imageUri));
                shareIntent.setData(imageUri);
                shareIntent.setClipData(ClipData.newRawUri("", imageUri));
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, Constants.SHARED_IMAGE_FILENAME);
                shareIntent.addFlags(
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareActivityResultLauncher.launch(shareIntent);
            }
        } catch (IOException e) {
            e.printStackTrace();
            //TODO handle error
        }
    }

    private void exportImage(final int position) {
        String imageFilename;
        ImageDto imageDto = imageDtos.get(position);
        Bitmap bitmap = createExportImage(imageDto);
        String path = imageDto.getFilename();
        String imageName = path.substring(path.lastIndexOf(File.separatorChar)+1).replace(".tjsn", "");
        int[] widths = mainActivity.getResources().getIntArray(R.array.resolution_widths);
        int[] heights = mainActivity.getResources().getIntArray(R.array.resolution_heights);
        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/tcamViewer/";
        File dir = new File(path);
        if(!dir.exists()) {
            dir.mkdirs();
        }
        path = path + imageName + ".png";
        OutputStream out = null;
        File imageFile = new File(path);

        try {
            int res = settings.getExportResolution().getValue();
            int h = bitmap.getHeight();
            int w = bitmap.getWidth();
            int eh = heights[res];
            int ew = widths[res];
            if(h != eh && w != ew) {
                //resize the bitmap
                bitmap = Bitmap.createScaledBitmap(bitmap, ew, eh, false);
            }
            out = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
    }

    private Bitmap createExportImage(ImageDto imageDto) {
        ImageView ivImageView;
        TextView tvSpotmeterTemperature;
        TextView tvMaxTemperature;
        ImageView ivColorBar;
        TextView tvMinTemperature;
        TextView tvFilename;

        View inflatedFrame = getLayoutInflater().inflate(R.layout.export_library_image, null);
        ConstraintLayout constraintLayout = (ConstraintLayout) inflatedFrame.findViewById(R.id.clItemLayout) ;
        constraintLayout.setDrawingCacheEnabled(true);
        constraintLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        constraintLayout.layout(0, 0, constraintLayout.getMeasuredWidth(), constraintLayout.getMeasuredHeight());
        constraintLayout.buildDrawingCache(true);
        ivImageView = constraintLayout.findViewById(R.id.ivCamera);
        tvSpotmeterTemperature = constraintLayout.findViewById(R.id.tvSpotmeterTemperature);
        ImageView ivSpotmeterTemperature = constraintLayout.findViewById(R.id.ivSpotmeterTemperature);
        writeTextonImageView(ivSpotmeterTemperature, R.id.ivSpotmeterTemperature, "32.0");
        tvMaxTemperature = constraintLayout.findViewById(R.id.tvMaxTemperature);
        ivColorBar = constraintLayout.findViewById(R.id.ivColorBar);
        tvMinTemperature = constraintLayout.findViewById(R.id.tvMinTemperature);
        tvFilename = constraintLayout.findViewById(R.id.tvFilename);
        Bitmap bitmap = imageDto.drawHotspot();
        ivImageView.setImageBitmap(bitmap);
        Bitmap colorbar = imageDto.createColorBar();
        ivColorBar.setImageBitmap(colorbar);
        Pair<Float, Float> temps = imageDto.getTemperatures();
        tvMaxTemperature.setText(String.format("%.2f", temps.second));
        tvMinTemperature.setText(String.format("%.2f", temps.first));
        bitmap = Bitmap.createBitmap(
                constraintLayout.getWidth(), constraintLayout.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        constraintLayout.draw(canvas);
        return bitmap;
    }

    private void writeTextonImageView(ImageView imageView, int resId, String text) {
        Bitmap.createBitmap(
                imageView.getWidth(), imageView.getHeight(), Bitmap.Config.ARGB_8888);
        imageView.setDrawingCacheEnabled(true);
        Drawable drawable = imageView.getBackground();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
//        Bitmap.Config config = bm.getConfig();
//        int width = bm.getWidth();
//        int height = bm.getHeight();
//
//        Bitmap newImage = Bitmap.createBitmap(width, height, config);
//
//        Canvas c = new Canvas(newImage);
//        c.drawBitmap(bm, 0, 0, null);
//
//        Paint paint = new Paint();
//        paint.setColor(Color.BLACK);
//        paint.setStyle(Paint.Style.FILL);
//        paint.setTextSize(20);
//        c.drawText(text, 0, 25, paint);
//
//        imageView.setImageBitmap(newImage);
    }

    private void deleteImage(final int position) {
        slideshowAdapter.removeItem(position);
        if (slideshowAdapter.getItemCount() == 0) {
            NavDirections navDirections = LibrarySlideShowFragmentDirections.actionLibrarySlideShowFragmentToNavigationLibrary();
            mainActivity.getNavController().navigate(navDirections);
        } else {
            binding.vpSlideshow.setAdapter(slideshowAdapter);
        }
    }

    private void setMenuItems(Menu menu) {
        MenuItem itemDelete = menu.findItem(R.id.action_item_delete);
        MenuItem itemSlideShow = menu.findItem(R.id.action_item_share);
    }


    @Override
    public void onPrepareMenu(@NonNull Menu menu) {
        MenuProvider.super.onPrepareMenu(menu);
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.library_slideshow_item_menu, menu);
        setMenuItems(menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        // command switch
        int position = binding.vpSlideshow.getCurrentItem();
        int id = menuItem.getItemId();
        if (id == R.id.action_item_delete) {
            deleteImage(position);
            return true;
        } else if (id == R.id.action_item_share) {
            shareImage(position);
            return true;
        } else if (id == R.id.action_item_export) {
            exportImage(position);
            return true;
       } else if(id == android.R.id.home) {
            NavDirections navDirections = LibrarySlideShowFragmentDirections.actionLibrarySlideShowFragmentToNavigationLibrary();
            mainActivity.getNavController().navigate(navDirections);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onMenuClosed(@NonNull Menu menu) {
        MenuProvider.super.onMenuClosed(menu);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        libraryViewModel.clearAllSelectedImages();
        if(navBar != null) {
            navBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View v) {

    }
}

