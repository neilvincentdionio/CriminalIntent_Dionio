package com.example.criminalintent;

import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import java.util.Date;

public class DatePickerActivity extends SingleFragmentActivity {

    public static Intent newIntent(Context context, Date date) {
        Intent intent = new Intent(context, DatePickerActivity.class);
        intent.putExtra(DatePickerFragment.EXTRA_DATE, date);
        return intent;
    }

    @Override
    protected Fragment createFragment() {
        Date date = new Date();
        Object extra = getIntent().getSerializableExtra(DatePickerFragment.EXTRA_DATE);
        if (extra instanceof Date) {
            date = (Date) extra;
        }
        return DatePickerFragment.newInstance(date);
    }
}

