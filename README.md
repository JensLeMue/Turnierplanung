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
- Nationale Qualifikationsturniere bei bestimmten EFC/FIE-Events
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
club,type,ageCategory
Leverkusen,QB,SEN
Reutlingen,QB,SEN
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



Dr. Ing. h.c. F. Porsche Aktiengesellschaft
Sitz der Gesellschaft: Stuttgart
Registergericht: Amtsgericht Stuttgart HRB-Nr. 730623
Vorsitzender des Aufsichtsrats: Dr. Wolfgang Porsche
Vorstand: Dr. Michael Leiters, Vorsitzender
Dr. Michael Steiner, stellvertretender Vorsitzender
Matthias Becker, Dr. Jochen Breckner, Sajjad Khan,
Albrecht Reimold, Vera Schalwig, Joachim Scharnagl
Informationen zum Umgang mit Ihren Daten finden Sie in unseren Datenschutzhinweisen.
Die vorgenannten Angaben werden jeder E-Mail automatisch hinzugefügt. Dies ist kein Anerkenntnis, dass es sich beim Inhalt dieser E-Mail um eine rechtsverbindliche Erklärung der Porsche AG handelt. Erklärungen, die die Porsche AG verpflichten, bedürfen jeweils der Unterschrift durch zwei zeichnungsberechtigte Personen der AG.
