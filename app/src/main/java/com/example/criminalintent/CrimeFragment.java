package com.example.criminalintent;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class CrimeFragment extends Fragment {

    private static final String ARG_CRIME_ID = "crime_id";
    private static final String DIALOG_PHOTO = "DialogPhoto";
    private static final String[] CONTACT_QUERY_FIELDS = new String[]{
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME
    };
    private static final String[] PHONE_QUERY_FIELDS = new String[]{
            ContactsContract.CommonDataKinds.Phone.NUMBER
    };

    public static CrimeFragment newInstance(UUID crimeId) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeId);

        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private Crime crime;
    private Crime originalCrime;
    private boolean isNewCrime;

    private EditText titleField;
    private Button dateButton;
    private CheckBox solvedCheckBox;
    private TextView statusTextView;
    private Button reportButton;
    private Button suspectButton;
    private Button callSuspectButton;
    private Button photoButton;
    private ImageButton photoView;
    private Button saveButton;
    private ViewTreeObserver.OnGlobalLayoutListener photoViewLayoutListener;
    private ActivityResultLauncher<String> requestReadContactsPermissionLauncher;
    private ActivityResultLauncher<Intent> selectSuspectLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        requestReadContactsPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchContactPicker();
                    } else if (isAdded()) {
                        Toast.makeText(requireContext(), R.string.read_contacts_permission_denied, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        selectSuspectLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                        return;
                    }
                    Uri contactUri = result.getData().getData();
                    if (contactUri != null) {
                        updateSuspectFromContact(contactUri);
                    }
                }
        );

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                didTakePhoto -> {
                    if (!didTakePhoto) {
                        deleteEmptyPhotoFile();
                    }
                    updatePhotoViewWhenReady();
                }
        );

        UUID crimeId = null;
        Bundle args = getArguments();
        if (args != null) {
            Object serializable = args.getSerializable(ARG_CRIME_ID);
            if (serializable instanceof UUID) {
                crimeId = (UUID) serializable;
            }
        }
        if (crimeId == null) return;

        originalCrime = CrimeRepository.get().getCrime(crimeId);
        if (originalCrime != null) {
            isNewCrime = false;
            crime = new Crime(originalCrime.getId());
            crime.setTitle(originalCrime.getTitle());
            crime.setDate(originalCrime.getDate());
            crime.setSolved(originalCrime.isSolved());
            crime.setRequiresPolice(originalCrime.isRequiresPolice());
            crime.setSuspect(originalCrime.getSuspect());
            crime.setSuspectPhoneNumber(originalCrime.getSuspectPhoneNumber());
        } else {
            isNewCrime = true;
            crime = new Crime(crimeId);
            // Set default title for new crimes
            int crimeCount = CrimeRepository.get().getCrimes().size();
            crime.setTitle("Crime #" + (crimeCount + 1));
        }

        getParentFragmentManager().setFragmentResultListener(
                DatePickerFragment.REQUEST_KEY_DATE,
                this,
                (requestKey, result) -> {
                    Object serializable = result.getSerializable(DatePickerFragment.BUNDLE_KEY_DATE);
                    if (serializable instanceof Date && crime != null) {
                        crime.setDate((Date) serializable);
                        updateDateTime();
                    }
                }
        );

        getParentFragmentManager().setFragmentResultListener(
                TimePickerFragment.REQUEST_KEY_TIME,
                this,
                (requestKey, result) -> {
                    Object serializable = result.getSerializable(TimePickerFragment.BUNDLE_KEY_TIME);
                    if (serializable instanceof Date && crime != null) {
                        crime.setDate((Date) serializable);
                        updateDateTime();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        if (crime == null) return null;
        View view = inflater.inflate(R.layout.fragment_crime, container, false);

        titleField = view.findViewById(R.id.crime_title);
        dateButton = view.findViewById(R.id.crime_date);
        solvedCheckBox = view.findViewById(R.id.crime_solved);
        statusTextView = view.findViewById(R.id.crime_status);
        reportButton = view.findViewById(R.id.crime_report);
        suspectButton = view.findViewById(R.id.crime_suspect);
        callSuspectButton = view.findViewById(R.id.crime_call_suspect);
        photoButton = view.findViewById(R.id.crime_camera);
        photoView = view.findViewById(R.id.crime_photo);
        saveButton = view.findViewById(R.id.crime_save);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (crime == null) return;

        titleField.setText(crime.getTitle());
        titleField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (crime != null) crime.setTitle(s != null ? s.toString() : "");
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        updateDateTime();
        dateButton.setEnabled(true);
        dateButton.setOnClickListener(v -> {
            if (crime == null) return;
            
            // Create a dialog to choose between date and time
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
            builder.setTitle("Change Date or Time");
            builder.setItems(new CharSequence[]{"Change Date", "Change Time"}, (dialog, which) -> {
                if (which == 0) {
                    // Change Date
                    DatePickerFragment dateDialog = DatePickerFragment.newInstance(crime.getDate());
                    dateDialog.show(getParentFragmentManager(), "DatePickerFragment");
                } else if (which == 1) {
                    // Change Time
                    TimePickerFragment timeDialog = TimePickerFragment.newInstance(crime.getDate());
                    timeDialog.show(getParentFragmentManager(), "TimePickerFragment");
                }
            });
            builder.show();
        });

        solvedCheckBox.setChecked(crime.isSolved());
        solvedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (crime != null) {
                crime.setSolved(isChecked);
                updateStatusAndButtonColor();
            }
        });

        // Initialize status and button color
        updateStatusAndButtonColor();
        updateSuspectButtons();
        configurePhotoControls();

        reportButton.setOnClickListener(v -> ShareCompat.IntentBuilder.from(requireActivity())
                .setType("text/plain")
                .setSubject(getString(R.string.crime_report_subject))
                .setText(getCrimeReport())
                .setChooserTitle(R.string.send_report)
                .startChooser());

        suspectButton.setOnClickListener(v -> selectSuspect());
        callSuspectButton.setOnClickListener(v -> dialSuspect());

        saveButton.setOnClickListener(v -> {
            if (isNewCrime) {
                CrimeRepository.get().addCrime(crime);
            } else if (originalCrime != null) {
                originalCrime.setTitle(crime.getTitle());
                originalCrime.setDate(crime.getDate());
                originalCrime.setSolved(crime.isSolved());
                originalCrime.setRequiresPolice(crime.isRequiresPolice());
                originalCrime.setSuspect(crime.getSuspect());
                originalCrime.setSuspectPhoneNumber(crime.getSuspectPhoneNumber());
            }
            finishCrimeScreen();
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_crime, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_item_delete_crime) {
            showDeleteCrimeConfirmation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteCrimeConfirmation() {
        if (!isAdded() || crime == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_crime)
                .setMessage(R.string.delete_crime_confirmation)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    deleteCrime();
                    finishCrimeScreen();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteCrime() {
        if (crime == null) return;

        deletePhotoFile();

        Crime storedCrime = originalCrime != null
                ? originalCrime
                : CrimeRepository.get().getCrime(crime.getId());
        if (storedCrime != null) {
            CrimeRepository.get().deleteCrime(storedCrime);
        }
    }

    private void finishCrimeScreen() {
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void selectSuspect() {
        if (!isAdded()) return;

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            launchContactPicker();
        } else {
            requestReadContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
        }
    }

    private void launchContactPicker() {
        if (!isAdded()) return;

        Intent pickContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        try {
            selectSuspectLauncher.launch(pickContact);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(requireContext(), R.string.no_contacts_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSuspectFromContact(@NonNull Uri contactUri) {
        if (!isAdded() || crime == null) return;

        try (Cursor contactCursor = requireActivity().getContentResolver().query(
                contactUri,
                CONTACT_QUERY_FIELDS,
                null,
                null,
                null
        )) {
            if (contactCursor == null || !contactCursor.moveToFirst()) {
                return;
            }

            long contactId = contactCursor.getLong(
                    contactCursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            );
            String suspectName = contactCursor.getString(
                    contactCursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
            );
            String suspectPhoneNumber = queryPhoneNumber(contactId);

            crime.setSuspect(suspectName);
            crime.setSuspectPhoneNumber(suspectPhoneNumber);
            updateSuspectButtons();

            if (suspectPhoneNumber == null || suspectPhoneNumber.trim().isEmpty()) {
                Toast.makeText(requireContext(), R.string.suspect_has_no_phone, Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException exception) {
            Toast.makeText(requireContext(), R.string.read_contacts_permission_denied, Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    private String queryPhoneNumber(long contactId) {
        if (!isAdded()) return null;

        String selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?";
        String[] selectionArgs = {String.valueOf(contactId)};
        try (Cursor phoneCursor = requireActivity().getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                PHONE_QUERY_FIELDS,
                selection,
                selectionArgs,
                null
        )) {
            if (phoneCursor == null || !phoneCursor.moveToFirst()) {
                return null;
            }
            return phoneCursor.getString(
                    phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            );
        }
    }

    private void dialSuspect() {
        if (!isAdded() || crime == null) return;

        String suspectPhoneNumber = crime.getSuspectPhoneNumber();
        if (suspectPhoneNumber == null || suspectPhoneNumber.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.suspect_has_no_phone, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent dialIntent = new Intent(Intent.ACTION_DIAL);
        dialIntent.setData(Uri.parse("tel:" + Uri.encode(suspectPhoneNumber)));
        try {
            startActivity(dialIntent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(requireContext(), R.string.no_dialer_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSuspectButtons() {
        if (crime == null || suspectButton == null || callSuspectButton == null) return;

        String suspect = crime.getSuspect();
        boolean hasSuspect = suspect != null && !suspect.trim().isEmpty();
        suspectButton.setText(hasSuspect ? suspect : getString(R.string.choose_suspect));

        boolean hasPhoneNumber = crime.getSuspectPhoneNumber() != null
                && !crime.getSuspectPhoneNumber().trim().isEmpty();
        callSuspectButton.setEnabled(hasPhoneNumber);
        callSuspectButton.setText(hasSuspect
                ? getString(R.string.call_suspect, suspect)
                : getString(R.string.call_suspect_default));
    }

    private void configurePhotoControls() {
        if (photoButton == null || photoView == null || !isAdded()) {
            return;
        }

        boolean canTakePhoto = canResolveCameraIntent();
        photoButton.setEnabled(canTakePhoto);
        photoButton.setOnClickListener(v -> launchCamera());
        photoView.setOnClickListener(v -> showPhotoDialog());
        updatePhotoViewWhenReady();
    }

    private boolean canResolveCameraIntent() {
        if (!isAdded()) {
            return false;
        }

        Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        return captureImage.resolveActivity(requireActivity().getPackageManager()) != null;
    }

    private void launchCamera() {
        if (!isAdded() || crime == null) {
            return;
        }

        File photoFile = getPhotoFile();
        Uri photoUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                photoFile
        );
        takePictureLauncher.launch(photoUri);
    }

    private void showPhotoDialog() {
        if (crime == null || !hasPhoto()) {
            return;
        }

        if (getParentFragmentManager().findFragmentByTag(DIALOG_PHOTO) != null) {
            return;
        }

        PhotoDialogFragment.newInstance(crime.getPhotoFilename())
                .show(getParentFragmentManager(), DIALOG_PHOTO);
    }

    private void updatePhotoViewWhenReady() {
        if (photoView == null || !isAdded()) {
            return;
        }

        removePhotoViewLayoutListener();

        if (!hasPhoto()) {
            updatePhotoView();
            return;
        }

        // Make the view participate in layout so we can use its measured bounds.
        photoView.setVisibility(View.INVISIBLE);

        if (photoView.getWidth() > 0 && photoView.getHeight() > 0) {
            updatePhotoView();
            return;
        }

        photoViewLayoutListener = () -> {
            if (photoView == null) {
                return;
            }
            if (photoView.getWidth() <= 0 || photoView.getHeight() <= 0) {
                return;
            }
            removePhotoViewLayoutListener();
            updatePhotoView();
        };

        ViewTreeObserver observer = photoView.getViewTreeObserver();
        if (observer.isAlive()) {
            observer.addOnGlobalLayoutListener(photoViewLayoutListener);
        }
    }

    private void updatePhotoView() {
        if (photoView == null || !isAdded()) {
            return;
        }

        if (!hasPhoto()) {
            removePhotoViewLayoutListener();
            photoView.setImageDrawable(null);
            photoView.setVisibility(View.VISIBLE);
            photoView.setEnabled(false);
            return;
        }

        int targetWidth = photoView.getWidth();
        int targetHeight = photoView.getHeight();
        if (targetWidth <= 0 || targetHeight <= 0) {
            updatePhotoViewWhenReady();
            return;
        }

        photoView.setVisibility(View.VISIBLE);
        photoView.setEnabled(true);
        photoView.setImageBitmap(
                PictureUtils.getScaledBitmap(
                        getPhotoFile().getPath(),
                        targetWidth,
                        targetHeight
                )
        );
    }

    private boolean hasPhoto() {
        File photoFile = getPhotoFile();
        return photoFile.exists() && photoFile.length() > 0L;
    }

    @NonNull
    private File getPhotoFile() {
        return new File(requireContext().getFilesDir(), crime.getPhotoFilename());
    }

    private void deleteEmptyPhotoFile() {
        if (!isAdded() || crime == null) {
            return;
        }

        File photoFile = getPhotoFile();
        if (photoFile.exists() && photoFile.length() == 0L) {
            photoFile.delete();
        }
    }

    private void deletePhotoFile() {
        if (!isAdded() || crime == null) {
            return;
        }

        File photoFile = getPhotoFile();
        if (photoFile.exists()) {
            photoFile.delete();
        }
    }

    private void removePhotoViewLayoutListener() {
        if (photoView == null || photoViewLayoutListener == null) {
            return;
        }

        ViewTreeObserver observer = photoView.getViewTreeObserver();
        if (observer.isAlive()) {
            observer.removeOnGlobalLayoutListener(photoViewLayoutListener);
        }
        photoViewLayoutListener = null;
    }

    private String getCrimeReport() {
        if (crime == null) return "";

        String solvedString = getString(
                crime.isSolved() ? R.string.crime_report_solved : R.string.crime_report_unsolved
        );
        String title = crime.getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = getString(R.string.untitled_crime);
        }
        String dateString = new SimpleDateFormat(
                "EEE MMM dd yyyy, hh:mm a",
                Locale.getDefault()
        ).format(crime.getDate());

        return getString(R.string.crime_report, title, dateString, solvedString);
    }

    private void updateDateTime() {
        if (crime == null) return;
        if (dateButton != null) {
            SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("EEE MMM dd yyyy, hh:mm a", Locale.getDefault());
            dateButton.setText(dateTimeFormatter.format(crime.getDate()));
        }
    }

    private void updateStatusAndButtonColor() {
        if (crime == null || statusTextView == null || saveButton == null) return;
        
        if (crime.isSolved()) {
            statusTextView.setText("Case Closed");
            saveButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark, null));
        } else {
            statusTextView.setText("Case Open");
            saveButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark, null));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        removePhotoViewLayoutListener();
        if (photoView != null) {
            photoView.setImageDrawable(null);
        }
        titleField = null;
        dateButton = null;
        solvedCheckBox = null;
        statusTextView = null;
        reportButton = null;
        suspectButton = null;
        callSuspectButton = null;
        photoButton = null;
        photoView = null;
        saveButton = null;
    }
}
