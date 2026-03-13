package com.example.criminalintent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;
import java.util.Date;

public class TimePickerFragment extends DialogFragment {

    public static final String REQUEST_KEY_TIME = "requestKeyTime";
    public static final String BUNDLE_KEY_TIME = "bundleKeyTime";

    private static final String ARG_DATE = "argDate";

    public static TimePickerFragment newInstance(Date date) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_DATE, date);
        TimePickerFragment fragment = new TimePickerFragment();
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
        View view = inflater.inflate(R.layout.dialog_time, container, false);

        Date date = new Date();
        Bundle args = getArguments();
        if (args != null) {
            Object serializable = args.getSerializable(ARG_DATE);
            if (serializable instanceof Date) {
                date = (Date) serializable;
            }
        }
        final Date selectedDate = date;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(selectedDate);

        TimePicker timePicker = view.findViewById(R.id.dialog_time_picker);
        timePicker.setIs24HourView(false);
        timePicker.setHour(calendar.get(Calendar.HOUR_OF_DAY));
        timePicker.setMinute(calendar.get(Calendar.MINUTE));

        Button okButton = view.findViewById(R.id.dialog_time_ok);
        okButton.setOnClickListener(v -> {
            Calendar resultCalendar = Calendar.getInstance();
            resultCalendar.setTime(selectedDate);
            resultCalendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
            resultCalendar.set(Calendar.MINUTE, timePicker.getMinute());
            resultCalendar.set(Calendar.SECOND, 0);
            resultCalendar.set(Calendar.MILLISECOND, 0);

            Bundle result = new Bundle();
            result.putSerializable(BUNDLE_KEY_TIME, resultCalendar.getTime());
            getParentFragmentManager().setFragmentResult(REQUEST_KEY_TIME, result);
            dismiss();
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() == null || getDialog().getWindow() == null) return;
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.95f);
        getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
