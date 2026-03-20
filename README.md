# Fencing Planner – Nationale Fechtsaison-Turnierplanung

Automatisierte Erstellung eines nationalen Turnierplans für die deutsche Fechtsaison mithilfe von Constraint-Solving ([Timefold Solver](https://timefold.ai/)).

## Problemstellung

Jede Saison müssen nationale Fecht-Turniere auf die verfügbaren Wochenenden verteilt werden. Dabei gelten zahlreiche Randbedingungen, die eine manuelle Planung fehleranfällig und zeitaufwendig machen. Dieses Tool löst das Planungsproblem automatisch als Constraint-Satisfaction-Problem.

## Features

- **Fixe internationale Termine**: FIE- und EFC-Turniere werden als unveränderliche Vorgaben übernommen, DMs zum Saisonende nach den Q-Turnieren, vor der EM/WM. Da muss man die Daten für haben.
- **Altersklassen**: VET, SEN, U23, U20, U17, U15, U14 werden berücksichtigt
- **Bewerbungen der Ausrichter**: Vereine bewerben sich als Ausrichter und werden auf die Saison verteilt
- **Bevorzugte Wochenenden**: Ausrichter können in ihrer Bewerbung einzelne Termine als bevorzugt (`*`) markieren – der Solver versucht, diese Wunschtermine bevorzugt zuzuweisen (+20 Soft-Score-Bonus)
- **Konflikterkennung**: Überlappende Altersklassen (z. B. U20-Athleten dürfen auch bei SEN starten) werden nie auf dasselbe Wochenende gelegt
- **Gesperrte Wochenenden**: Feiertage/Ferien können als blockiert markiert werden

### Implementierte Constraints (Hard)

| Constraint | Beschreibung |
|---|---|
| Blocked Weekend | Kein Turnier auf gesperrten Wochenenden |
| FIE Fixed | FIE-Turniere bleiben auf ihrem fixen Datum |
| EFC Fixed | EFC-Turniere bleiben auf ihrem fixen Datum |
| Athlete Overlap | Keine zwei Turniere am selben Wochenende, wenn Athleten in beiden Altersklassen startberechtigt wären |
| Venue Availability | Turnier darf nur auf ein Wochenende gelegt werden, an dem der Ausrichter verfügbar ist |

### Implementierte Constraints (Soft)

| Constraint | Beschreibung |
|---|---|
| Preferred Date Reward | +20 Bonus, wenn ein Turnier auf einen bevorzugten Wunschtermin des Ausrichters gelegt wird |
| Min Weeks Between Tournaments | Mindestabstand in Wochen zwischen Turnieren derselben Altersklasse |
| Even Monthly Distribution | Gleichmäßige Verteilung der Turniere über die Monate |

### Geplante Erweiterungen

- Mindestabstand zwischen Turnieren derselben Altersklasse
- DM am Saisonende mit kombinierten Altersklassen (U20+U15 etc.)
- Damen- und Herrenwettbewerbe am selben Wochenende
- Streckenoptimierung für gleichmäßige geografische Verteilung
- SoftConstraints für überlappende Altersgruppen.
- Soft Constraints für bestimmte Feiertage (Totensonntag wenn es nicht anders geht)

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
| `ageCategoryWeekGap.csv` | Mindestabstand in Wochen zwischen Turnieren pro Altersklasse | `ageCategory`, `minWeeksBetweenTournaments` |

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
Reutlingen,QB,SEN,2026-10-03;2026-10-10;2026-11-07*;2026-11-14*
Solingen,QB,U17,2026-09-26*;2026-10-10*;2026-11-07*;2026-12-12
```

- `all` = Ausrichter ist an allen Wochenenden verfügbar
- Datum ohne `*` = verfügbar, aber kein Wunschtermin
- Datum mit `*` = bevorzugter Wunschtermin (Soft-Constraint belohnt Zuweisung)

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

## Score-Erklärung (`score_explanation.txt`)

Nach jedem Solver-Durchlauf wird automatisch die Datei `score_explanation.txt` erzeugt. Sie enthält eine detaillierte Aufschlüsselung des Optimierungsergebnisses:

- **Overall Score**: Gesamtwertung als `hard/soft` (z. B. `0hard/-4520soft`)
- **Hard-Constraints**: Auflistung aller verletzten Pflicht-Constraints, sortiert nach Schwere. Ein Hard-Score < 0 bedeutet, dass noch Regelverletzungen existieren (z. B. Turniere auf gesperrten Wochenenden).
- **Soft-Constraints**: Auflistung aller Optimierungs-Constraints mit Score und Anzahl betroffener Event-Paare (Matches). Negative Werte = Strafen (z. B. zu geringe Abstände), positive Werte = Belohnungen (z. B. Wunschtermine getroffen).

Beispielausgabe:

```
--- Hard-Constraints (größte Verletzungen zuerst) ---
  Constraint                              Score    Matches
  blocked weekend                            -2          2

--- Soft-Constraints (größte Faktoren zuerst) ---
  Constraint                              Score    Matches
  even monthly distribution               -5200         38
  min weeks between tournaments            -450          8
  preferred date reward                      60          3
```

Die gleiche Aufschlüsselung wird auch auf der Konsole ausgegeben.

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
*to do* Lizenz ergänzen.


- U14 Turniere müssen eingepflegt werdn in die csv
