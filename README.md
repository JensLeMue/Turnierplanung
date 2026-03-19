# Fencing Planner – Nationale Fechtsaison-Turnierplanung

Automatisierte Erstellung eines nationalen Turnierplans für die deutsche Fechtsaison mithilfe von Constraint-Solving ([Timefold Solver](https://timefold.ai/)).

## Problemstellung

Jede Saison müssen nationale Fecht-Turniere auf die verfügbaren Wochenenden verteilt werden. Dabei gelten zahlreiche Randbedingungen, die eine manuelle Planung fehleranfällig und zeitaufwendig machen. Dieses Tool löst das Planungsproblem automatisch als Constraint-Satisfaction-Problem.

## Features

- **Fixe internationale Termine**: FIE- und EFC-Turniere werden als unveränderliche Vorgaben übernommen (via Pinning-Filter vom Solver nicht verschiebbar). DMs werden zum Saisonende nach den Q-Turnieren und vor der EM/WM platziert.
- **Altersklassen**: VET, SEN, U23, U20, U17, U15, U13 werden berücksichtigt, inkl. Startberechtigungen über Altersklassen hinweg (z. B. U17 darf auch bei U20/U23/SEN starten)
- **Bewerbungen der Ausrichter**: Vereine bewerben sich als Ausrichter und werden auf die Saison verteilt. Verfügbare Hallentermine und Wunschtermine werden berücksichtigt.
- **Konflikterkennung**: Überlappende Altersklassen (z. B. U20-Athleten dürfen auch bei SEN starten) werden nie auf dasselbe Wochenende gelegt
- **Gesperrte Wochenenden**: Feiertage/Ferien können als blockiert markiert werden (FIE/EFC-Events ausgenommen)
- **Excel-Export**: Automatische Erstellung einer farbcodierten Excel-Datei (`Turnierplanung.xlsx`) mit Kalenderwochen und Altersklassen-Matrix
- **Score-Erklärung**: Zusammenfassung des Optimierungsergebnisses wird in `score_explanation.txt` geschrieben
- **Konsolenausgabe**: Detaillierte Übersicht aller Wochenenden (inkl. Feiertage) und Events nach der Optimierung

### Implementierte Constraints (Hard)

| Constraint | Beschreibung |
|---|---|
| Blocked Weekend | Kein Turnier auf gesperrten Wochenenden (FIE/EFC ausgenommen) |
| FIE Fixed | FIE-Turniere bleiben auf ihrem fixen Datum |
| EFC Fixed | EFC-Turniere bleiben auf ihrem fixen Datum |
| DM Fixed | DM-Turniere bleiben auf ihrem fixen Datum |
| QB Equivalent Overlap | Keine Überlappung von Qualifikationsturnieren bei startberechtigten Altersklassen |
| Venue Availability | Events nur auf verfügbaren Wochenenden des Ausrichters |
| Athlete Overlap | Keine zwei Turniere am selben Wochenende, wenn Athleten in beiden Altersklassen startberechtigt wären |
| DM Weekend Constraints | U15+U20 DMs und U13+U17 DMs am gleichen Wochenende |
| DM Before EM/WM | Deutsche Meisterschaften vor der EM/WM derselben Altersklasse |
| DM After Q | Deutsche Meisterschaften nach den Qualifikationsturnieren |

Zusätzlich werden Events mit festen Terminen (`fixedWeekend`) über einen **Pinning-Filter** (`FixedEventPinningFilter`) komplett aus der Optimierung ausgeschlossen, um externe Termine zuverlässig zu bewahren.

### Implementierte Constraints (Soft)

| Constraint | Gewicht | Beschreibung |
|---|---|---|
| Min Weeks Between Tournaments | ×15 | Mindestabstand zwischen Turnieren derselben Altersklasse (konfigurierbar über `ageCategoryWeekGap.csv`) |
| Events Before DM | ×10 | Nationale/Regionale Turniere sollen vor der DM stattfinden |
| Preferred Date Reward | +20 | Belohnung für Zuweisung auf Wunschtermine des Veranstalters |
| Even Monthly Distribution | ×1 | Gleichmäßige Verteilung der Turniere über die Saison (bestraft Lücken >4 Wochen) |

## Voraussetzungen

- **Java** 17+
- **Maven** 3.8+

## Build & Ausführung

```bash
# Projekt bauen
mvn clean package

# Ausführen
mvn exec:java -Dexec.mainClass="com.fencingplanner.App"
```

Der Solver läuft ca. 120 Sekunden (Construction Heuristic + 60 s Tabu Search) und gibt den optimierten Turnierplan auf der Konsole aus. Zusätzlich werden `Turnierplanung.xlsx` und `score_explanation.txt` im Projektverzeichnis erzeugt.

## Eingabedaten (CSV)

Die Eingabedaten liegen unter `src/main/resources/`:

| Datei | Beschreibung | Spalten |
|---|---|---|
| `weekends.csv` | Verfügbare Wochenenden der Saison | `date, blocked` |
| `events.csv` | Fixe internationale Turniere (FIE/EFC) | `name, type, ageCategory, fixedDate, qbEquivalent` |
| `applications.csv` | Bewerbungen nationaler Ausrichter | `club, type, ageCategory[, venueAvailability]` |
| `clubs.csv` | Alle beteiligten Vereine/Organisationen | `name` |
| `ageCategoryWeekGap.csv` | Mindestabstand in Wochen zwischen Turnieren pro Altersklasse | `ageCategory, minWeeksBetweenTournaments` |

### Beispiel `events.csv`

```csv
name,type,ageCategory,fixedDate,qbEquivalent
FIE_Vancouver,FIE,SEN,2026-12-05,false
EFC_Dublin,EFC,SEN,2026-10-18,false
```

### Beispiel `applications.csv`

```csv
club,type,ageCategory,venueAvailability
Leverkusen,QB,SEN,all
Reutlingen,QB,SEN,2026-10-03*;2026-10-10*;2026-10-17;2026-10-24;2026-10-31
Heidenheim,QB,U17,all
```

- **`venueAvailability`**: Verfügbare Termine des Ausrichters, mit `;` getrennt. `all` = an allen Wochenenden verfügbar.
- **Wunschtermine**: Termine mit `*` markieren (z.B. `2026-10-03*`). Der Solver bevorzugt diese Termine (Soft-Constraint, +20 Reward), weicht aber bei Konflikten auf andere verfügbare Termine aus.

### Beispiel `ageCategoryWeekGap.csv`

```csv
ageCategory,minWeeksBetweenTournaments
VET,2
SEN,2
U17,2
U15,3
U13,4
```

## Ausgabe

Nach der Optimierung werden drei Ausgaben erzeugt:

1. **Konsolenausgabe**: Tabellarische Übersicht aller Wochenenden (mit Feiertag-Info) und aller Events sortiert nach Datum
2. **`Turnierplanung.xlsx`**: Farbcodierte Excel-Datei mit Kalenderwochen als Zeilen und Altersklassen als Spalten. Event-Typen sind farblich gekennzeichnet (FIE=Rot, EFC=Blau, National=Grün, Regional=Orange).
3. **`score_explanation.txt`**: Zusammenfassung des Hard- und Soft-Scores

## Projektstruktur

```
src/main/java/com/fencingplanner/
├── App.java                             # Einstiegspunkt, startet den Solver, Konsolenausgabe
├── DataLoader.java                      # Liest CSV-Dateien und erstellt das Planungsproblem
├── ExcelScheduleExporter.java           # Excel-Export mit Farbcodierung (Apache POI)
├── constraint/
│   └── ScheduleConstraintProvider.java  # Definiert Hard/Soft-Constraints
└── model/
    ├── AgeCategory.java                 # Enum der Altersklassen mit Startberechtigung
    ├── Club.java                        # Verein / Organisation
    ├── Event.java                       # Turnier (Planning Entity mit Pinning)
    ├── FixedEventPinningFilter.java     # Pinning-Filter für fixe Termine
    ├── Schedule.java                    # Gesamtplan (Planning Solution)
    └── Weekend.java                     # Wochenende (equals/hashCode auf Datum-Basis)
```

## Technologie-Stack

- **Java 17**
- **Timefold Solver 1.26.0** – Constraint-Satisfaction / Optimierung
- **Apache POI 5.2.3** – Excel-Export
- **SLF4J 2.0.9** – Logging
- **Maven** – Build-Management
- **Algorithmen**: First Fit (Konstruktion) + Tabu Search (lokale Suche)

## Solver-Konfiguration

Die Solver-Parameter sind in `src/main/resources/solverConfig.xml` konfigurierbar:

- **Termination**: 120 s gesamt, davon 60 s lokale Suche
- **Construction Heuristic**: FIRST_FIT
- **Local Search**: TABU_SEARCH

## Lizenz
*to do* Lizenz ergänzen.
