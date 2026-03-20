# Fencing Planner – Nationale Fechtsaison-Turnierplanung

Automatisierte Erstellung eines nationalen Turnierplans für die deutsche Fechtsaison mithilfe von Constraint-Solving ([Timefold Solver](https://timefold.ai/)).

## Problemstellung

Jede Saison müssen nationale Fecht-Turniere auf die verfügbaren Wochenenden verteilt werden. Dabei gelten zahlreiche Randbedingungen, die eine manuelle Planung fehleranfällig und zeitaufwendig machen. Dieses Tool löst das Planungsproblem automatisch als Constraint-Satisfaction-Problem.

## Features

- **Fixe internationale Termine**: FIE- und EFC-Turniere werden als unveränderliche Vorgaben übernommen, DMs zum Saisonende nach den Q-Turnieren, vor der EM/WM. Da muss man die Daten für haben.
- **Altersklassen**: VET, SEN, U23, U20, U17, U15, U14, U13 werden berücksichtigt
- **Bewerbungen der Ausrichter**: Vereine bewerben sich als Ausrichter und werden auf die Saison verteilt
- **Bevorzugte Wochenenden**: Ausrichter können in ihrer Bewerbung einzelne Termine als bevorzugt (`*`) markieren – der Solver versucht, diese Wunschtermine bevorzugt zuzuweisen (+20 Soft-Score-Bonus)
- **Konflikterkennung**: Überlappende Altersklassen (z. B. U20-Athleten dürfen auch bei SEN starten) werden nie auf dasselbe Wochenende gelegt
- **Gesperrte Wochenenden**: Feiertage/Ferien können als blockiert markiert werden (z.B. Ostern, Totensonntag)

### Implementierte Constraints (Hard)

| Constraint | Beschreibung |
|---|---|
| Blocked Weekend | Kein nationales Turnier auf gesperrten Wochenenden (Feiertage/Ferien). FIE- und EFC-Events sind ausgenommen, da deren Termine extern vom Weltverband vorgegeben werden. |
| FIE Fixed | FIE-Turniere (Weltverband) müssen auf ihrem festen Termin bleiben. Diese Termine werden international festgelegt und sind nicht verhandelbar. |
| EFC Fixed | EFC-Turniere (Europäischer Verband) müssen auf ihrem festen Termin bleiben. Wie bei FIE sind diese Termine extern vorgegeben. |
| DM Fixed | Deutsche Meisterschaften müssen auf ihrem festen Termin bleiben, sofern ein fester Termin vorgegeben ist (z.B. DM_VET mit fixem Hallentermin). |
| Athlete Overlap | Keine zwei verschiebbaren Turniere am selben Wochenende, wenn Athleten in beiden Altersklassen startberechtigt wären (z.B. U17-Athleten dürfen auch bei U20/U23/SEN starten). Gilt nur wenn mindestens ein Event verschiebbar ist. |
| QB Equivalent Overlap | Qualifikationsrelevante Turniere (QB, CHALLENGE, EFC mit QB-Wertung) dürfen nicht am selben Wochenende stattfinden, wenn Athleten aufgrund überlappender Altersklassen an beiden teilnehmen könnten. Verhindert, dass Athleten sich zwischen zwei Qualifikationsturnieren entscheiden müssen. |
| Venue Availability | Turnier darf nur auf ein Wochenende gelegt werden, an dem der Ausrichter seine Halle zur Verfügung hat. Die Verfügbarkeit wird pro Bewerbung als Datumsliste oder `all` angegeben. |
| DM Weekend Constraints | Bestimmte DM-Paarungen müssen am selben Wochenende stattfinden: U15+U20 gemeinsam und U13+U17 gemeinsam. Organisatorisch und logistisch sinnvoll, da diese Altersklassen traditionell zusammen ausgetragen werden. |
| DM Before EM/WM | Deutsche Meisterschaften müssen zeitlich vor den zugehörigen EM/WM-Terminen liegen. Die DM ist Qualifikation für die internationale Meisterschaft – sie muss vorher abgeschlossen sein. |
| DM After Q | Deutsche Meisterschaften müssen zeitlich nach allen Qualifikations- und Challenge-Turnieren derselben Altersklasse liegen. Die Qualifikationsphase muss abgeschlossen sein, bevor die DM stattfindet. Ausnahme: U17 darf bewusst ein Turnier nach der DM haben (Folgesaison-Logik). |
| U17 September Qualifier | Für U17 muss mindestens ein QB/Challenge-Turnier im September liegen. |
| U17 At Most One September Qualifier | Für U17 darf es im September höchstens ein QB/Challenge-Turnier geben (damit zusammen mit der Regel oben effektiv genau eins im September liegt). |
| U17 Post-DM Qualifier | Für U17 muss mindestens ein QB/Challenge-Turnier nach der U17-DM liegen (Auftakt der Folgesaison). |
| SEN November Tournament | Für SEN muss mindestens ein qualifikationsrelevantes SEN-Turnier (`qbEquivalent=true`) im November liegen. |
| SEN January Tournament | Für SEN muss mindestens ein qualifikationsrelevantes SEN-Turnier (`qbEquivalent=true`) im Januar liegen. |
| SEN March Tournament | Für SEN muss mindestens ein qualifikationsrelevantes SEN-Turnier (`qbEquivalent=true`) im März liegen. |
| U15 Challenge Winter Anchor | Für U15-Challenge muss mindestens ein Turnier im Dezember oder Januar liegen, damit keine große Herbst-Winter-Lücke entsteht. |

### Implementierte Constraints (Soft)

| Constraint | Gewicht | Beschreibung |
|---|---|---|
| Preferred Date Reward | ×20 | Belohnt die Zuweisung auf einen bevorzugten Wunschtermin des Ausrichters (+20 pro Treffer). Ausrichter markieren Wunschtermine in ihrer Bewerbung mit `*` – so wird deren Präferenz berücksichtigt, ohne andere Constraints zu verletzen. |
| Min Weeks Between Tournaments | ×15 | Bestraft Unterschreitungen des Mindestabstands (in Wochen) zwischen Turnieren derselben Altersklasse. Athleten brauchen Erholungs- und Vorbereitungszeit zwischen Wettkämpfen; die Mindestabstände sind pro Altersklasse konfigurierbar (z.B. U15=3 Wochen, U13=4 Wochen). |
| Min Weeks Between U15 Challenges | ×20 | Zusätzliche, strengere Mindestabstandsregel nur für `CHALLENGE` in U15 (mindestens 4 Wochen Abstand), um Ballungen im U15-Challenge-Kalender zu vermeiden. |
| Max Weeks Between U15 Challenges | ×15 | Bestraft zu große Lücken zwischen aufeinanderfolgenden U15-Challenges (Lücke > 7 Wochen), damit U15 gleichmäßiger über die Saison verteilt wird. |
| Events Before DM | ×10 | Bestraft nationale/regionale Turniere (QB, CHALLENGE), die zeitlich nach der DM derselben Altersklasse liegen. Qualifikationsturniere sollen logischerweise vor der Deutschen Meisterschaft stattfinden, damit die Qualifikation abgeschlossen ist. |
| Seasonal Gap Distribution | ×1 | Bestraft zu große Lücken zwischen aufeinanderfolgenden Turnieren derselben Altersklasse (nicht mehr alle Event-Paare). Damit wird über den Saisonverlauf optimiert statt rein „monatlich“. Die zulässige Lücke ist pro Altersklasse feinjustierbar. |
| U17 Post-DM Qualifier Timing | ×30 | Bestraft U17-Q/Challenge-Turniere, die zu lange nach der U17-DM liegen. Ziel ist ein zeitnahes Post-DM-Turnier (Richtwert: innerhalb von 4 Wochen). |
| SEN National Q Preferred Months | ×120 | Starke Präferenz für verschiebbare SEN-Turniere mit `qbEquivalent=true` in den Monaten November, Januar und März. |
| Athlete Overlap (fixed, info) | ×1 | Warnung wenn zwei fixe internationale Events (FIE/EFC) mit überlappenden Altersklassen am selben Wochenende liegen. Diese Konflikte sind durch die extern vorgegebenen Termine unvermeidbar und dienen nur zur Information. |
| QB Equivalent Overlap (fixed, info) | ×1 | Warnung wenn zwei fixe qualifikationsrelevante Events am selben Wochenende mit überlappenden Altersklassen liegen. Wie oben: rein informativ, da die Termine nicht verschiebbar sind. |

### Aktuelle Feintuning-Hinweise

- **VET vs. SEN**: VET wird aktuell **nicht** mehr als automatische Überschneidung zu SEN behandelt. Dadurch können VET- und SEN-Termine parallel liegen, wenn das organisatorisch gewünscht ist.
- **Saison statt Monats-Paare**: Die frühere reine Monats-/Paarlogik wurde durch eine saisonale Nachbarlogik ersetzt. Entscheidend sind Lücken zwischen aufeinanderfolgenden Events.
- **Feinjustierung pro Altersklasse**:
  - Untere Grenze (Mindestabstand) über `ageCategoryWeekGap.csv`
  - Obere Grenze (zulässige Saisonlücke) im Constraint-Code (`getMaxSeasonGapWeeks`)

### Parameter-Quick-Reference

| Ziel | Stellschraube | Ort |
|---|---|---|
| Mindestabstand einer Altersklasse erhöhen/verringern | `minWeeksBetweenTournaments` pro Altersklasse | `src/main/resources/ageCategoryWeekGap.csv` |
| Große Saisonlücken einer Altersklasse stärker/schwächer bestrafen | `getMaxSeasonGapWeeks(AgeCategory)` anpassen | `src/main/java/com/fencingplanner/constraint/ScheduleConstraintProvider.java` |
| SEN-Q stärker auf Nov/Jan/März ziehen | Gewicht in `senNationalQPreferredMonths` anpassen | `src/main/java/com/fencingplanner/constraint/ScheduleConstraintProvider.java` |
| U17: genau ein September-Turnier | `u17SeptemberQualifier` + `u17AtMostOneSeptemberQualifier` | `src/main/java/com/fencingplanner/constraint/ScheduleConstraintProvider.java` |
| U17: Post-DM-Turnier näher hinter DM positionieren | Zielabstand/Gewicht in `u17PostDmQualifierTiming` anpassen | `src/main/java/com/fencingplanner/constraint/ScheduleConstraintProvider.java` |
| U15-Challenge entzerren (zu dicht) | `minWeeksBetweenU15Challenges` (Mindestabstand, Gewicht) | `src/main/java/com/fencingplanner/constraint/ScheduleConstraintProvider.java` |
| U15-Challenge glätten (zu große Lücken) | `maxWeeksBetweenU15Challenges` (max. Lücke, Gewicht) | `src/main/java/com/fencingplanner/constraint/ScheduleConstraintProvider.java` |
| U15-Winterlücke vermeiden | `u15ChallengeWinterAnchor` (Dez/Jan-Pflicht) | `src/main/java/com/fencingplanner/constraint/ScheduleConstraintProvider.java` |
| VET/SEN dürfen parallel liegen oder nicht | `AgeCategory.canStartIn()` für `VET` | `src/main/java/com/fencingplanner/model/AgeCategory.java` |

Empfohlener Ablauf beim Tuning:

1. Nur einen Regler gleichzeitig ändern.
2. Solver laufen lassen und `score_explanation.txt` vergleichen.
3. Erst danach den nächsten Regler anpassen.

### Geplante Erweiterungen

- Damen- und Herrenwettbewerbe am selben Wochenende
- Streckenoptimierung für gleichmäßige geografische Verteilung
- U14-Turniere einpflegen

## Voraussetzungen

- **Java** 25+
- **Maven** 3.8+

## Build & Ausführung

```bash
# Projekt bauen
mvn clean package

# Ausführen
mvn exec:java -Dexec.mainClass="com.fencingplanner.App"
```

Der Solver läuft ca. 30 Sekunden (Construction Heuristic + 30 s Tabu Search) und gibt den optimierten Turnierplan auf der Konsole aus.

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
    ├── Event.java                    # Turnier (Planning Entity, @PlanningPin für fixe Events)
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

- **Java 25**
- **Timefold Solver 1.32.0** – Constraint-Satisfaction / Optimierung
- **Apache POI 5.5.1** – Excel-Export
- **Maven** – Build-Management
- **Algorithmen**: First Fit (Konstruktion) + Tabu Search (lokale Suche)

## Solver-Konfiguration

Die Solver-Parameter sind in `src/main/resources/solverConfig.xml` konfigurierbar:

- **Termination**: 30 s Konstruktion + 30 s lokale Suche
- **Construction Heuristic**: FIRST_FIT
- **Local Search**: TABU_SEARCH

## Lizenz
*to do* Lizenz ergänzen.
