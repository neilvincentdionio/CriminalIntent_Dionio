package com.example.criminalintent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;
import java.util.Date;

public class DatePickerFragment extends DialogFragment {

    public static final String REQUEST_KEY_DATE = "requestKeyDate";
    public static final String BUNDLE_KEY_DATE = "bundleKeyDate";
    public static final String EXTRA_DATE = "com.example.criminalintent.date";

    private static final String ARG_DATE = "argDate";

    private DatePicker datePicker;

    public static DatePickerFragment newInstance(Date date) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_DATE, date);
        DatePickerFragment fragment = new DatePickerFragment();
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
        View view = inflater.inflate(R.layout.dialog_date, container, false);

        Date date = new Date();
        Bundle args = getArguments();
        if (args != null) {
            Object serializable = args.getSerializable(ARG_DATE);
            if (serializable instanceof Date) {
                date = (Date) serializable;
            }
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        datePicker = view.findViewById(R.id.dialog_date_picker);
        datePicker.init(year, month, day, null);

        Button okButton = view.findViewById(R.id.dialog_date_ok);
        okButton.setOnClickListener(v -> {
            Calendar resultCalendar = Calendar.getInstance();
            resultCalendar.set(Calendar.YEAR, datePicker.getYear());
            resultCalendar.set(Calendar.MONTH, datePicker.getMonth());
            resultCalendar.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());

            if (args != null) {
                Object serializable = args.getSerializable(ARG_DATE);
                if (serializable instanceof Date) {
                    Calendar original = Calendar.getInstance();
                    original.setTime((Date) serializable);
                    resultCalendar.set(Calendar.HOUR_OF_DAY, original.get(Calendar.HOUR_OF_DAY));
                    resultCalendar.set(Calendar.MINUTE, original.get(Calendar.MINUTE));
                }
            }
            resultCalendar.set(Calendar.SECOND, 0);
            resultCalendar.set(Calendar.MILLISECOND, 0);
            sendResult(resultCalendar.getTime());
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

    private void sendResult(Date date) {
        if (getShowsDialog()) {
            Bundle result = new Bundle();
            result.putSerializable(BUNDLE_KEY_DATE, date);
            getParentFragmentManager().setFragmentResult(REQUEST_KEY_DATE, result);
            dismiss();
            return;
        }

        if (getActivity() != null) {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DATE, date);
            getActivity().setResult(Activity.RESULT_OK, intent);
            getActivity().finish();
        }
    }
}
