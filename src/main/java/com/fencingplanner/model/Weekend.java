package com.fencingplanner.model;

import java.time.LocalDate;

import java.util.Objects;

public class Weekend {  // KEINE @ProblemFact!
    private LocalDate date;
    private boolean blocked;

    public Weekend() {}

    public Weekend(LocalDate date) {
        this.date = date;
    }

    /**
     * equals/hashCode basiert auf dem Datum, nicht auf der Objekt-Referenz.
     * 
     * GRUND: Timefold/OptaPlanner klont Objekte intern während der Optimierung.
     * Dadurch entstehen mehrere Weekend-Instanzen für dasselbe Datum.
     * Wenn wir nur Referenzen (==) vergleichen würden, würden diese als VERSCHIEDEN
     * behandelt, obwohl sie dieselbe Kalenderwoche repräsentieren.
     * 
     * Dies führte dazu, dass die Fixed-Date-Constraints (fixedFIE, fixedEFC, fixedDM)
     * NICHT erkannten, dass ein Event auf seinem fixedWeekend liegt, und es daher
     * unnötig als "verletzt" bestraft wurde.
     * 
     * Mit equals/hashCode auf Datum-Basis funktioniert die Identität zuverlässig.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Weekend weekend = (Weekend) o;
        return Objects.equals(date, weekend.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date);
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
