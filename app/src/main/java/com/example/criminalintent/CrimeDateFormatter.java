package com.example.criminalintent;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Date;

final class CrimeDateFormatter {

    private CrimeDateFormatter() {
    }

    @NonNull
    static String formatListDate(@NonNull Context context, @NonNull Date date) {
        return android.text.format.DateFormat.getMediumDateFormat(context).format(date);
    }

    @NonNull
    static String formatDateTime(@NonNull Context context, @NonNull Date date) {
        String localizedDate = android.text.format.DateFormat.getLongDateFormat(context).format(date);
        String localizedTime = android.text.format.DateFormat.getTimeFormat(context).format(date);
        return context.getString(R.string.localized_date_time, localizedDate, localizedTime);
    }
}
