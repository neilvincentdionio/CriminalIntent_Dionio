package com.example.criminalintent;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import java.util.List;
import java.util.UUID;

public class CrimePagerActivity extends AppCompatActivity {

    public static final String EXTRA_CRIME_ID = "com.example.criminalintent.crime_id";

    private ViewPager2 viewPager;
    private Button firstButton;
    private Button lastButton;
    private List<Crime> crimes;

    public static Intent newIntent(Context context, UUID crimeId) {
        Intent intent = new Intent(context, CrimePagerActivity.class);
        intent.putExtra(EXTRA_CRIME_ID, crimeId);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crime_pager);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        crimes = CrimeRepository.get().getCrimes();
        viewPager = findViewById(R.id.crime_view_pager);
        firstButton = findViewById(R.id.button_first);
        lastButton = findViewById(R.id.button_last);

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return CrimeFragment.newInstance(crimes.get(position).getId());
            }

            @Override
            public int getItemCount() {
                return crimes.size();
            }
        });

        UUID crimeId = null;
        if (getIntent() != null) {
            Object extra = getIntent().getSerializableExtra(EXTRA_CRIME_ID);
            if (extra instanceof UUID) {
                crimeId = (UUID) extra;
            }
        }

        if (savedInstanceState == null) {
            int startIndex = 0;
            if (crimeId != null) {
                for (int i = 0; i < crimes.size(); i++) {
                    if (crimeId.equals(crimes.get(i).getId())) {
                        startIndex = i;
                        break;
                    }
                }
            }
            viewPager.setCurrentItem(startIndex, false);
        }

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateJumpButtons(position);
            }
        });
        viewPager.post(() -> updateJumpButtons(viewPager.getCurrentItem()));

        firstButton.setOnClickListener(v -> viewPager.setCurrentItem(0, false));
        lastButton.setOnClickListener(v -> {
            int lastIndex = Math.max(0, crimes.size() - 1);
            viewPager.setCurrentItem(lastIndex, false);
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateJumpButtons(int position) {
        int lastIndex = Math.max(0, crimes.size() - 1);
        firstButton.setEnabled(position > 0);
        lastButton.setEnabled(position < lastIndex);
    }
}

