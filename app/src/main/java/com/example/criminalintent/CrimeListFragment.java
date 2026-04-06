package com.example.criminalintent;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.File;
import java.util.List;
import java.util.UUID;

public class CrimeListFragment extends Fragment {

    private static final String SAVED_SUBTITLE_VISIBLE = "subtitle_visible";
    private static final int MAX_CRIMES = 10;
    private static final String LANGUAGE_ENGLISH = "en";
    private static final String LANGUAGE_SPANISH = "es";

    private RecyclerView recyclerView;
    private CrimeAdapter adapter;
    private ExtendedFloatingActionButton fabAddCrime;
    private View emptyStateContainer;
    private Button emptyStateButton;
    private boolean isSubtitleVisible;
    private UUID selectedCrimeId;
    private Callbacks callbacks;

    public interface Callbacks {
        void onCrimeSelected(@NonNull UUID crimeId);
        void onCreateCrimeRequested(@NonNull UUID crimeId);
        void onCrimeSelectionCleared();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            isSubtitleVisible = savedInstanceState.getBoolean(SAVED_SUBTITLE_VISIBLE, false);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Callbacks) {
            callbacks = (Callbacks) context;
            return;
        }
        throw new IllegalStateException("Host activity must implement CrimeListFragment.Callbacks");
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_crime_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.crime_recycler_view);
        emptyStateContainer = view.findViewById(R.id.empty_state_container);
        emptyStateButton = view.findViewById(R.id.empty_state_button);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CrimeAdapter(CrimeRepository.get().getCrimes(), crime -> {
            if (callbacks != null) {
                callbacks.onCrimeSelected(crime.getId());
            }
        });
        adapter.setSelectedCrimeId(selectedCrimeId);
        recyclerView.setAdapter(adapter);
        attachSwipeToDismiss();

        fabAddCrime = view.findViewById(R.id.fab_add_crime);
        fabAddCrime.setOnClickListener(v -> launchCreateCrime());
        emptyStateButton.setOnClickListener(v -> launchCreateCrime());

        updateUi();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_crime_list, menu);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem subtitleItem = menu.findItem(R.id.menu_item_show_subtitle);
        if (subtitleItem != null) {
            subtitleItem.setTitle(isSubtitleVisible ? R.string.hide_subtitle : R.string.show_subtitle);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_item_show_subtitle) {
            isSubtitleVisible = !isSubtitleVisible;
            updateSubtitle();
            requireActivity().invalidateOptionsMenu();
            return true;
        } else if (item.getItemId() == R.id.menu_item_change_language) {
            showLanguagePicker();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_SUBTITLE_VISIBLE, isSubtitleVisible);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
    }

    private void launchCreateCrime() {
        if (CrimeRepository.get().getCrimes().size() >= MAX_CRIMES) {
            return;
        }

        UUID id = UUID.randomUUID();
        if (callbacks != null) {
            callbacks.onCreateCrimeRequested(id);
            return;
        }

        if (getContext() != null) {
            Intent intent = CrimeActivity.newIntent(getContext(), id);
            startActivity(intent);
        }
    }

    public void setSelectedCrimeId(@Nullable UUID crimeId) {
        selectedCrimeId = crimeId;
        if (adapter != null) {
            adapter.setSelectedCrimeId(crimeId);
        }
    }

    public void refreshList() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateUi();
    }

    private void updateUi() {
        updateSubtitle();

        int crimeCount = CrimeRepository.get().getCrimes().size();
        boolean hasCrimes = crimeCount > 0;
        boolean canAddCrime = crimeCount < MAX_CRIMES;

        recyclerView.setVisibility(hasCrimes ? View.VISIBLE : View.GONE);
        emptyStateContainer.setVisibility(hasCrimes ? View.GONE : View.VISIBLE);
        fabAddCrime.setVisibility(hasCrimes && canAddCrime ? View.VISIBLE : View.GONE);
        emptyStateButton.setVisibility(canAddCrime ? View.VISIBLE : View.GONE);
    }

    private void attachSwipeToDismiss() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(
                    @NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder,
                    @NonNull RecyclerView.ViewHolder target
            ) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION || adapter == null) {
                    refreshList();
                    return;
                }

                Crime deletedCrime = adapter.removeCrimeAt(position);
                if (deletedCrime == null) {
                    refreshList();
                    return;
                }

                deleteCrimePhoto(deletedCrime);
                updateUi();

                if (selectedCrimeId != null && selectedCrimeId.equals(deletedCrime.getId())) {
                    UUID replacementCrimeId = getReplacementCrimeId(position);
                    setSelectedCrimeId(replacementCrimeId);
                    if (replacementCrimeId != null && callbacks != null) {
                        callbacks.onCrimeSelected(replacementCrimeId);
                    } else if (callbacks != null) {
                        callbacks.onCrimeSelectionCleared();
                    }
                }
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
    }

    private void showLanguagePicker() {
        if (!isAdded()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.choose_language);
        builder.setItems(new CharSequence[]{
                getString(R.string.language_english),
                getString(R.string.language_spanish)
        }, (dialog, which) -> {
            if (which == 0) {
                applyLanguage(LANGUAGE_ENGLISH);
            } else if (which == 1) {
                applyLanguage(LANGUAGE_SPANISH);
            }
        });
        builder.show();
    }

    private void applyLanguage(@NonNull String languageTag) {
        String currentLanguageTag = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        if (languageTag.equals(currentLanguageTag)) {
            return;
        }

        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag));
        if (getActivity() != null) {
            getActivity().recreate();
        }
    }

    @Nullable
    private UUID getReplacementCrimeId(int deletedPosition) {
        List<Crime> crimes = CrimeRepository.get().getCrimes();
        if (crimes.isEmpty()) {
            return null;
        }

        if (deletedPosition < crimes.size()) {
            return crimes.get(deletedPosition).getId();
        }

        return crimes.get(crimes.size() - 1).getId();
    }

    private void deleteCrimePhoto(@NonNull Crime crime) {
        if (!isAdded()) {
            return;
        }

        File photoFile = new File(requireContext().getFilesDir(), crime.getPhotoFilename());
        if (photoFile.exists()) {
            photoFile.delete();
        }
    }

    private void updateSubtitle() {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity == null || activity.getSupportActionBar() == null) {
            return;
        }

        int crimeCount = CrimeRepository.get().getCrimes().size();
        String subtitle = getResources().getQuantityString(
                R.plurals.subtitle_plural,
                crimeCount,
                crimeCount
        );
        activity.getSupportActionBar().setSubtitle(isSubtitleVisible ? subtitle : null);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callbacks = null;
    }

    private static class CrimeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_TYPE_NORMAL = 0;
        private static final int VIEW_TYPE_POLICE = 1;

        private final List<Crime> crimes;
        private final OnCrimeClickListener onCrimeClick;
        private UUID selectedCrimeId;

        interface OnCrimeClickListener {
            void onCrimeClick(Crime crime);
        }

        CrimeAdapter(List<Crime> crimes, OnCrimeClickListener onCrimeClick) {
            this.crimes = crimes;
            this.onCrimeClick = onCrimeClick;
        }

        @Override
        public int getItemViewType(int position) {
            Crime crime = crimes.get(position);
            return crime.isSolved() ? VIEW_TYPE_NORMAL : VIEW_TYPE_POLICE;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == VIEW_TYPE_POLICE) {
                View view = inflater.inflate(R.layout.list_item_crime_police, parent, false);
                return new PoliceCrimeHolder(view);
            }
            View view = inflater.inflate(R.layout.list_item_crime, parent, false);
            return new CrimeHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Crime crime = crimes.get(position);
            boolean isSelected = crime.getId().equals(selectedCrimeId);
            if (holder instanceof PoliceCrimeHolder) {
                ((PoliceCrimeHolder) holder).bind(crime, onCrimeClick, isSelected);
            } else if (holder instanceof CrimeHolder) {
                ((CrimeHolder) holder).bind(crime, onCrimeClick, isSelected);
            }
        }

        @Override
        public int getItemCount() {
            return crimes.size();
        }

        void setSelectedCrimeId(@Nullable UUID crimeId) {
            selectedCrimeId = crimeId;
            notifyDataSetChanged();
        }

        @Nullable
        Crime removeCrimeAt(int position) {
            if (position < 0 || position >= crimes.size()) {
                return null;
            }

            Crime crime = crimes.get(position);
            CrimeRepository.get().deleteCrime(crime);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, crimes.size() - position);
            return crime;
        }

        static class CrimeHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            private final TextView titleTextView;
            private final TextView dateTextView;
            private final ImageView solvedIcon;
            private Crime crime;
            private OnCrimeClickListener onCrimeClick;

            CrimeHolder(@NonNull View itemView) {
                super(itemView);
                titleTextView = itemView.findViewById(R.id.crime_title);
                dateTextView = itemView.findViewById(R.id.crime_date);
                solvedIcon = itemView.findViewById(R.id.crime_solved_icon);
                itemView.setOnClickListener(this);
            }

            void bind(Crime crime, OnCrimeClickListener onCrimeClick, boolean isSelected) {
                this.crime = crime;
                this.onCrimeClick = onCrimeClick;

                titleTextView.setText(crime.getTitle());
                dateTextView.setText(CrimeDateFormatter.formatListDate(itemView.getContext(), crime.getDate()));
                solvedIcon.setVisibility(crime.isSolved() ? View.VISIBLE : View.GONE);
                itemView.setBackgroundColor(ContextCompat.getColor(
                        itemView.getContext(),
                        isSelected ? R.color.crime_row_selected_surface : R.color.crime_row_surface
                ));
                
                // Set text color to green for solved crimes
                if (crime.isSolved()) {
                    titleTextView.setTextColor(itemView.getResources().getColor(android.R.color.holo_green_dark, null));
                } else {
                    titleTextView.setTextColor(itemView.getResources().getColor(android.R.color.primary_text_light, null));
                }
            }

            @Override
            public void onClick(View v) {
                if (crime != null && onCrimeClick != null) {
                    onCrimeClick.onCrimeClick(crime);
                }
            }
        }

        static class PoliceCrimeHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            private final TextView titleTextView;
            private final TextView dateTextView;
            private final ImageView solvedIcon;
            private final Button contactPoliceButton;

            private Crime crime;
            private OnCrimeClickListener onCrimeClick;

            PoliceCrimeHolder(@NonNull View itemView) {
                super(itemView);
                titleTextView = itemView.findViewById(R.id.crime_title);
                dateTextView = itemView.findViewById(R.id.crime_date);
                solvedIcon = itemView.findViewById(R.id.crime_solved_icon);
                contactPoliceButton = itemView.findViewById(R.id.contact_police_button);

                itemView.setOnClickListener(this);
            }

            void bind(Crime crime, OnCrimeClickListener onCrimeClick, boolean isSelected) {
                this.crime = crime;
                this.onCrimeClick = onCrimeClick;

                titleTextView.setText(crime.getTitle());
                dateTextView.setText(CrimeDateFormatter.formatListDate(itemView.getContext(), crime.getDate()));
                solvedIcon.setVisibility(crime.isSolved() ? View.VISIBLE : View.GONE);
                contactPoliceButton.setVisibility(crime.isSolved() ? View.GONE : View.VISIBLE);
                itemView.setBackgroundColor(ContextCompat.getColor(
                        itemView.getContext(),
                        isSelected ? R.color.crime_row_selected_surface : R.color.crime_row_surface
                ));
                
                // Set text color to green for solved crimes
                if (crime.isSolved()) {
                    titleTextView.setTextColor(itemView.getResources().getColor(android.R.color.holo_green_dark, null));
                } else {
                    titleTextView.setTextColor(itemView.getResources().getColor(android.R.color.primary_text_light, null));
                }

                contactPoliceButton.setOnClickListener(v -> {
                    Toast.makeText(v.getContext(), v.getContext().getString(R.string.contact_police_toast), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onClick(View v) {
                if (crime != null && onCrimeClick != null) {
                    onCrimeClick.onCrimeClick(crime);
                }
            }
        }
    }
}
