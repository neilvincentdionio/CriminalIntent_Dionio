package com.example.criminalintent;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CrimeRepository {
    private static CrimeRepository sInstance;

    private final List<Crime> crimes = new ArrayList<>();
    private final Map<UUID, Crime> crimesById = new HashMap<>();

    private CrimeRepository() { 
        // Add one solved crime
        Crime solvedCrime = new Crime(UUID.randomUUID(),
                "Crime #1", new Date(), true);
        crimes.add(solvedCrime);
        crimesById.put(solvedCrime.getId(), solvedCrime);

        // Add one unsolved crime that requires police
        Crime unsolvedCrime = new Crime(UUID.randomUUID(),
                "Crime #2", new Date(), false);
        unsolvedCrime.setRequiresPolice(true);
        crimes.add(unsolvedCrime);
        crimesById.put(unsolvedCrime.getId(), unsolvedCrime);
    }

    public static CrimeRepository get() {
        if (sInstance == null) {
            sInstance = new CrimeRepository();
        }
        return sInstance;
    }

    public List<Crime> getCrimes() {
        return crimes;
    }

    public Crime getCrime(UUID id) {
        if (id == null) return null;
        return crimesById.get(id);
    }

    public void addCrime(Crime crime) {
        if (crime == null) return;
        crimes.add(crime);
        crimesById.put(crime.getId(), crime);
    }

    public void deleteCrime(Crime crime) {
        if (crime == null) return;
        crimes.remove(crime);
        crimesById.remove(crime.getId());
    }
}
