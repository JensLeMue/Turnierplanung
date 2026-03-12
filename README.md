# Fencing Planner – Nationale Fechtsaison-Turnierplanung

Automatisierte Erstellung eines nationalen Turnierplans für die deutsche Fechtsaison mithilfe von Constraint-Solving ([Timefold Solver](https://timefold.ai/)).

## Problemstellung

Jede Saison müssen nationale Fecht-Turniere auf die verfügbaren Wochenenden verteilt werden. Dabei gelten zahlreiche Randbedingungen, die eine manuelle Planung fehleranfällig und zeitaufwendig machen. Dieses Tool löst das Planungsproblem automatisch als Constraint-Satisfaction-Problem.

## Features

- **Fixe internationale Termine**: FIE- und EFC-Turniere werden als unveränderliche Vorgaben übernommen
- **Altersklassen**: VET, SEN, U23, U20, U17, U15, U14 werden berücksichtigt
- **Bewerbungen der Ausrichter**: Vereine bewerben sich als Ausrichter und werden auf die Saison verteilt
- **Konflikterkennung**: Überlappende Altersklassen (z. B. U20-Athleten dürfen auch bei SEN starten) werden nie auf dasselbe Wochenende gelegt
- **Gesperrte Wochenenden**: Feiertage/Ferien können als blockiert markiert werden

### Implementierte Constraints (Hard)

| Constraint | Beschreibung |
|---|---|
| Blocked Weekend | Kein Turnier auf gesperrten Wochenenden |
| FIE Fixed | FIE-Turniere bleiben auf ihrem fixen Datum |
| EFC Fixed | EFC-Turniere bleiben auf ihrem fixen Datum |
| Athlete Overlap | Keine zwei Turniere am selben Wochenende, wenn Athleten in beiden Altersklassen startberechtigt wären |

### Geplante Erweiterungen

- Mindestabstand zwischen Turnieren derselben Altersklasse
- DM am Saisonende mit kombinierten Altersklassen (U20+U15 etc.)
- Damen- und Herrenwettbewerbe am selben Wochenende
- Streckenoptimierung für gleichmäßige geografische Verteilung

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

Der Solver läuft ca. 90 Sekunden (30 s Construction Heuristic + 60 s Tabu Search) und gibt den optimierten Turnierplan auf der Konsole aus.

## Eingabedaten (CSV)

Die Eingabedaten liegen unter `src/main/resources/`:

| Datei | Beschreibung | Spalten |
|---|---|---|
| `weekends.csv` | Verfügbare Wochenenden der Saison | `date, blocked` |
| `events.csv` | Fixe internationale Turniere (FIE/EFC) | `name, type, ageCategory, fixedDate, qbEquivalent` |
| `applications.csv` | Bewerbungen nationaler Ausrichter | `club, type, ageCategory` |
| `clubs.csv` | Alle beteiligten Vereine/Organisationen | `name` |

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
Reutlingen,QB,SEN,2026-10-18;2026-11-15;2027-02-20
```

## Projektstruktur

```
src/main/java/com/fencingplanner/
├── App.java                          # Einstiegspunkt, startet den Solver
├── DataLoader.java                   # Liest CSV-Dateien und erstellt das Planungsproblem
├── constraint/
│   └── ScheduleConstraintProvider.java  # Definiert Hard/Soft-Constraints
└── model/
    ├── AgeCategory.java              # Enum der Altersklassen mit Startberechtigung
    ├── Club.java                     # Verein / Organisation
    ├── Event.java                    # Turnier (Planning Entity)
    ├── Schedule.java                 # Gesamtplan (Planning Solution)
    └── Weekend.java                  # Wochenende (Planning Variable Range)
```

## Technologie-Stack

- **Java 17**
- **Timefold Solver 1.26.0** – Constraint-Satisfaction / Optimierung
- **Maven** – Build-Management
- **Algorithmen**: First Fit (Konstruktion) + Tabu Search (lokale Suche)

## Solver-Konfiguration

Die Solver-Parameter sind in `src/main/resources/solverConfig.xml` konfigurierbar:

- **Termination**: 30 s Konstruktion + 60 s lokale Suche
- **Construction Heuristic**: FIRST_FIT
- **Local Search**: TABU_SEARCH mit Entity-Tabu-Größe 5

## Lizenz

*TODO: Lizenz ergänzen*

- SoftConstraints für überlappende Altersgruppen.
- Soft Constraints für bestimmte Feiertage (Totensonntag wenn es nicht anders geht)
- Output als Excelfile oder ähnliches zur Darstellung
- Ergänzung WM und EM als fixe Turniere. Dazu DM kurz vor WM zw em als Highlight der Saison.
- U14 Turniere müssen eingepflegt werdn in die csv
- Terminrestruktionen von Bewerben müssen in die CSV eingepfelgt werden.
