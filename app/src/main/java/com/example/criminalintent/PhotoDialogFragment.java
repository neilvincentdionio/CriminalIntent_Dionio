package com.example.criminalintent;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.io.File;

public class PhotoDialogFragment extends DialogFragment {

    private static final String ARG_PHOTO_FILENAME = "photoFilename";

    public static PhotoDialogFragment newInstance(String photoFilename) {
        Bundle args = new Bundle();
        args.putString(ARG_PHOTO_FILENAME, photoFilename);

        PhotoDialogFragment fragment = new PhotoDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.dialog_photo, container, false);

        ImageView photoView = view.findViewById(R.id.crime_photo_zoomed);
        String photoFilename = getArguments() != null
                ? getArguments().getString(ARG_PHOTO_FILENAME)
                : null;

        if (photoFilename == null) {
            dismiss();
            return view;
        }

        File photoFile = new File(requireContext().getFilesDir(), photoFilename);
        if (!photoFile.exists() || photoFile.length() == 0L) {
            dismiss();
            return view;
        }

        int targetWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.95f);
        int targetHeight = (int) (getResources().getDisplayMetrics().heightPixels * 0.8f);
        Bitmap zoomedBitmap = PictureUtils.getScaledBitmap(
                photoFile.getPath(),
                targetWidth,
                targetHeight
        );
        photoView.setImageBitmap(zoomedBitmap);

        view.setOnClickListener(v -> dismiss());
        photoView.setOnClickListener(v -> dismiss());
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() == null || getDialog().getWindow() == null) {
            return;
        }

        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.95f);
        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.85f);
        getDialog().getWindow().setLayout(width, height);
    }
}
