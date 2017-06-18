package com.mcodefactory.popularmovies.utils;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Picasso Target class used to save the large movie poster locally
 */

public class PicassoTarget implements Target {

    private File directory;
    private String imageName;

    public PicassoTarget(File directory, String imageName) {
        this.directory = directory;
        this.imageName = imageName;
    }

    @Override
    public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final File myImageFile = new File(directory, imageName); // Create image file
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(myImageFile);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Log.i("image", "image saved to >>>" + myImageFile.getAbsolutePath());

            }
        }).start();
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {

    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
        if (placeHolderDrawable != null) {}
    }
}
