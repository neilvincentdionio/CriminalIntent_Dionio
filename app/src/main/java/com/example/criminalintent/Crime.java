package com.example.criminalintent;

import java.util.Date;
import java.util.UUID;

public class Crime {
    private final UUID id;
    private String title;
    private Date date;
    private boolean solved;
    private boolean requiresPolice;

    public Crime(UUID id) {
        this.id = id;
        this.title = "";
        this.date = new Date();
        this.solved = false;
        this.requiresPolice = false;
    }

    public Crime(UUID id, String title, Date date, boolean solved) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.solved = solved;
        this.requiresPolice = false;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public boolean isSolved() {
        return solved;
    }

    public void setSolved(boolean solved) {
        this.solved = solved;
    }

    public boolean isRequiresPolice() {
        return requiresPolice;
    }

    public void setRequiresPolice(boolean requiresPolice) {
        this.requiresPolice = requiresPolice;
    }
}
