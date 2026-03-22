package com.example.criminalintent;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public final class PictureUtils {

    private PictureUtils() {
    }

    public static Bitmap getScaledBitmap(String path, int destWidth, int destHeight) {
        if (destWidth <= 0 || destHeight <= 0) {
            return BitmapFactory.decodeFile(path);
        }

        BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
        boundsOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, boundsOptions);

        int srcWidth = boundsOptions.outWidth;
        int srcHeight = boundsOptions.outHeight;
        int inSampleSize = 1;

        if (srcWidth > destWidth || srcHeight > destHeight) {
            int widthScale = Math.round((float) srcWidth / (float) destWidth);
            int heightScale = Math.round((float) srcHeight / (float) destHeight);
            inSampleSize = Math.max(1, Math.min(widthScale, heightScale));
        }

        BitmapFactory.Options scaledOptions = new BitmapFactory.Options();
        scaledOptions.inSampleSize = inSampleSize;
        return BitmapFactory.decodeFile(path, scaledOptions);
    }
}
