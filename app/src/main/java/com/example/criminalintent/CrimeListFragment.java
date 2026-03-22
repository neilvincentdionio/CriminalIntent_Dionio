package com.example.criminalintent;

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
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;
import java.util.UUID;

public class CrimeListFragment extends Fragment {

    private static final String SAVED_SUBTITLE_VISIBLE = "subtitle_visible";
    private static final int MAX_CRIMES = 10;

    private RecyclerView recyclerView;
    private CrimeAdapter adapter;
    private ExtendedFloatingActionButton fabAddCrime;
    private View emptyStateContainer;
    private Button emptyStateButton;
    private boolean isSubtitleVisible;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            isSubtitleVisible = savedInstanceState.getBoolean(SAVED_SUBTITLE_VISIBLE, false);
        }
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

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            activity.setSupportActionBar(toolbar);
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle(R.string.app_name);
            }
        }

        recyclerView = view.findViewById(R.id.crime_recycler_view);
        emptyStateContainer = view.findViewById(R.id.empty_state_container);
        emptyStateButton = view.findViewById(R.id.empty_state_button);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CrimeAdapter(CrimeRepository.get().getCrimes(), crime -> {
            if (getContext() == null) return;
            Intent intent = CrimePagerActivity.newIntent(getContext(), crime.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

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
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateUi();
    }

    private void launchCreateCrime() {
        if (CrimeRepository.get().getCrimes().size() >= MAX_CRIMES) {
            return;
        }

        UUID id = UUID.randomUUID();
        if (getContext() != null) {
            startActivity(CrimeActivity.newIntent(getContext(), id));
        }
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

    private static class CrimeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_TYPE_NORMAL = 0;
        private static final int VIEW_TYPE_POLICE = 1;

        private final List<Crime> crimes;
        private final OnCrimeClickListener onCrimeClick;

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
            if (holder instanceof PoliceCrimeHolder) {
                ((PoliceCrimeHolder) holder).bind(crime, onCrimeClick);
            } else if (holder instanceof CrimeHolder) {
                ((CrimeHolder) holder).bind(crime, onCrimeClick);
            }
        }

        @Override
        public int getItemCount() {
            return crimes.size();
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

            void bind(Crime crime, OnCrimeClickListener onCrimeClick) {
                this.crime = crime;
                this.onCrimeClick = onCrimeClick;

                titleTextView.setText(crime.getTitle());
                dateTextView.setText(DateFormat.format("EEEE, MMM dd, yyyy", crime.getDate()));
                solvedIcon.setVisibility(crime.isSolved() ? View.VISIBLE : View.GONE);
                
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

            void bind(Crime crime, OnCrimeClickListener onCrimeClick) {
                this.crime = crime;
                this.onCrimeClick = onCrimeClick;

                titleTextView.setText(crime.getTitle());
                dateTextView.setText(DateFormat.format("EEEE, MMM dd, yyyy", crime.getDate()));
                solvedIcon.setVisibility(crime.isSolved() ? View.VISIBLE : View.GONE);
                contactPoliceButton.setVisibility(crime.isSolved() ? View.GONE : View.VISIBLE);
                
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
