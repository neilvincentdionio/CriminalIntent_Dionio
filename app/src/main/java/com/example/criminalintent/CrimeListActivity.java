package com.example.criminalintent;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import java.util.List;
import java.util.UUID;

public class CrimeListActivity extends AppCompatActivity
        implements CrimeListFragment.Callbacks, CrimeFragment.Callbacks {

    private static final String SAVED_SELECTED_CRIME_ID = "selected_crime_id";

    private boolean isTablet;
    private UUID selectedCrimeId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isTablet = getResources().getBoolean(R.bool.is_tablet);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }

        if (savedInstanceState != null) {
            Object savedCrimeId = savedInstanceState.getSerializable(SAVED_SELECTED_CRIME_ID);
            if (savedCrimeId instanceof UUID) {
                selectedCrimeId = (UUID) savedCrimeId;
            }
        }

        if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new CrimeListFragment())
                    .commitNow();
        }

        CrimeListFragment listFragment = getCrimeListFragment();
        if (listFragment != null) {
            listFragment.setSelectedCrimeId(selectedCrimeId);
        }

        if (isTablet && selectedCrimeId != null) {
            showCrimeDetail(selectedCrimeId, false);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(SAVED_SELECTED_CRIME_ID, selectedCrimeId);
    }

    @Override
    public void onCrimeSelected(@NonNull UUID crimeId) {
        if (isTablet) {
            selectedCrimeId = crimeId;
            CrimeListFragment listFragment = getCrimeListFragment();
            if (listFragment != null) {
                listFragment.setSelectedCrimeId(selectedCrimeId);
            }
            showCrimeDetail(crimeId, false);
            return;
        }

        startActivity(CrimePagerActivity.newIntent(this, crimeId));
    }

    @Override
    public void onCreateCrimeRequested(@NonNull UUID crimeId) {
        if (isTablet) {
            selectedCrimeId = crimeId;
            CrimeListFragment listFragment = getCrimeListFragment();
            if (listFragment != null) {
                listFragment.setSelectedCrimeId(selectedCrimeId);
            }
            showCrimeDetail(crimeId, false);
            return;
        }

        startActivity(CrimeActivity.newIntent(this, crimeId));
    }

    @Override
    public void onCrimeSelectionCleared() {
        if (!isTablet) {
            return;
        }

        selectedCrimeId = null;
        CrimeListFragment listFragment = getCrimeListFragment();
        if (listFragment != null) {
            listFragment.setSelectedCrimeId(null);
        }
        clearCrimeDetail();
    }

    @Override
    public void onCrimeSaved(@NonNull UUID crimeId) {
        if (!isTablet) {
            return;
        }

        selectedCrimeId = crimeId;
        CrimeListFragment listFragment = getCrimeListFragment();
        if (listFragment != null) {
            listFragment.refreshList();
            listFragment.setSelectedCrimeId(selectedCrimeId);
        }
        showCrimeDetail(crimeId, true);
    }

    @Override
    public void onCrimeDeleted(@NonNull UUID crimeId) {
        if (!isTablet) {
            return;
        }

        CrimeListFragment listFragment = getCrimeListFragment();
        if (listFragment != null) {
            listFragment.refreshList();
        }

        if (selectedCrimeId == null || !selectedCrimeId.equals(crimeId)) {
            return;
        }

        List<Crime> crimes = CrimeRepository.get().getCrimes();
        selectedCrimeId = crimes.isEmpty() ? null : crimes.get(0).getId();

        if (listFragment != null) {
            listFragment.setSelectedCrimeId(selectedCrimeId);
        }

        if (selectedCrimeId != null) {
            showCrimeDetail(selectedCrimeId, false);
        } else {
            clearCrimeDetail();
        }
    }

    @Nullable
    private CrimeListFragment getCrimeListFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof CrimeListFragment) {
            return (CrimeListFragment) fragment;
        }
        return null;
    }

    private void showCrimeDetail(@NonNull UUID crimeId, boolean forceReplace) {
        if (!isTablet) {
            return;
        }

        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.detail_container);
        if (!forceReplace && currentFragment instanceof CrimeFragment) {
            Bundle arguments = currentFragment.getArguments();
            if (arguments != null) {
                Object currentCrimeId = arguments.getSerializable("crime_id");
                if (crimeId.equals(currentCrimeId)) {
                    return;
                }
            }
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.detail_container, CrimeFragment.newInstance(crimeId))
                .commit();
    }

    private void clearCrimeDetail() {
        if (!isTablet) {
            return;
        }

        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.detail_container);
        if (currentFragment == null) {
            return;
        }

        getSupportFragmentManager().beginTransaction()
                .remove(currentFragment)
                .commit();
    }
}
