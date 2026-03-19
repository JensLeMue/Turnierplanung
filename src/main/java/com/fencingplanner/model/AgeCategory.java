package com.fencingplanner.model;

public enum AgeCategory {

VET,
SEN,
U23,
U20,
U17,
U15,
U13;

// Mindestabstand zwischen Turnieren dieser Altersklasse in Wochen.
// Defaultwert 3 greift als Fallback, falls die Kategorie in der CSV fehlt.
// Konfigurierbar über: src/main/resources/ageCategoryWeekGap.csv
// Wird von DataLoader.loadAgeCategoryWeekGaps() gesetzt und von
// ScheduleConstraintProvider.minWeeksBetweenTournaments() ausgewertet.
private int minWeeksBetweenTournaments = 3;

public int getMinWeeksBetweenTournaments() {
    return minWeeksBetweenTournaments;
}

public void setMinWeeksBetweenTournaments(int minWeeksBetweenTournaments) {
    this.minWeeksBetweenTournaments = minWeeksBetweenTournaments;
}

// Prüft, ob Athleten dieser Altersklasse auch in einer anderen Klasse startberechtigt sind.
// Wird von ScheduleConstraintProvider (athleteOverlap, qbEquivalentOverlap) verwendet,
// um Überlappungen auf demselben Wochenende zu verhindern.
//
// Startberechtigungen:
//   VET  -> darf auch bei SEN starten
//   SEN  -> keine weitere Startberechtigung
//   U23  -> darf auch bei SEN starten
//   U20  -> darf auch bei U23, SEN starten
//   U17  -> darf auch bei U20, U23, SEN starten
//   U15  -> darf auch bei U17 starten
//   U13  -> darf auch bei U15 starten
public boolean canStartIn(AgeCategory other){

if(this==other) return true;

switch(this){

case VET:
return other==SEN;

case U23:
return other==SEN;

case U20:
return other==U23 || other==SEN;

case U17:
return other==U20 || other==U23 || other==SEN;

case U15:
return other==U17;

case U13:
return other==U15;

default:
return false;

}

}

}