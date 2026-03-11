package com.fencingplanner.model;

import java.time.LocalDate;

public class Weekend {  // KEINE @ProblemFact!
    private LocalDate date;
    private boolean blocked;

    public Weekend() {}

    public Weekend(LocalDate date) {
        this.date = date;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}
