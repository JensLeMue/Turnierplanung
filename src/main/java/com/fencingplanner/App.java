package com.fencingplanner;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fencingplanner.model.Event;
import com.fencingplanner.model.Schedule;
import com.fencingplanner.model.Weekend;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatch;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;

public class App {

    // Feiertage für die Anzeige in der Wochenend-Übersicht.
    // Das tatsächliche Blocken der Wochenenden erfolgt über weekends.csv (blocked=true).
    private static final Map<java.time.LocalDate, String> HOLIDAYS = new HashMap<>();

    static {
        HOLIDAYS.put(java.time.LocalDate.of(2026, 11, 21), "Totensonntag");
        HOLIDAYS.put(java.time.LocalDate.of(2026, 12, 26), "2. Weihnachtsfeiertag");
        // 2027-01-01 (Neujahr) is a Friday, the Saturday 2027-01-02 is not blocked
        HOLIDAYS.put(java.time.LocalDate.of(2027, 4, 17), "Karfreitag");
        HOLIDAYS.put(java.time.LocalDate.of(2027, 5, 29), "Pfingstmontag");
    }

    private static String getHolidayName(java.time.LocalDate date) {
        return HOLIDAYS.getOrDefault(date, "");
    }

    /**
     * The main method to run the fencing tournament planning application.
     * It loads the schedule data, solves the planning problem, and exports the results.
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {

        DataLoader loader = new DataLoader();

        Schedule problem = loader.loadSchedule();

        SolverFactory<Schedule> factory =
                SolverFactory.createFromXmlResource("solverConfig.xml");

        Solver<Schedule> solver = factory.buildSolver();

        Schedule solution = solver.solve(problem);

        // Constraint-Aufschlüsselung via SolutionManager
        SolutionManager<Schedule, HardSoftScore> solutionManager = SolutionManager.create(factory);
        Map<String, ConstraintMatchTotal<HardSoftScore>> constraintMap =
                solutionManager.explain(solution).getConstraintMatchTotalMap();

        // Hard- und Soft-Constraints getrennt, nach Auswirkung sortiert
        List<ConstraintMatchTotal<HardSoftScore>> hardConstraints = constraintMap.values().stream()
                .filter(c -> c.getScore().hardScore() != 0)
                .sorted(Comparator.comparingInt(c -> c.getScore().hardScore()))
                .collect(Collectors.toList());

        List<ConstraintMatchTotal<HardSoftScore>> softConstraints = constraintMap.values().stream()
                .filter(c -> c.getScore().softScore() != 0)
                .sorted(Comparator.comparingInt(c -> c.getScore().softScore()))
                .collect(Collectors.toList());

        // Konsolen-Ausgabe der Constraint-Aufschlüsselung
        System.out.println("\n===== CONSTRAINT-AUFSCHLÜSSELUNG =====\n");
        System.out.println("Overall Score: " + solution.getScore());

        System.out.println("\n--- Hard-Constraints (größte Verletzungen zuerst) ---");
        if (hardConstraints.isEmpty()) {
            System.out.println("  Keine Hard-Constraint-Verletzungen!");
        } else {
            System.out.println(String.format("  %-35s %10s %10s", "Constraint", "Score", "Matches"));
            for (ConstraintMatchTotal<HardSoftScore> c : hardConstraints) {
                System.out.println(String.format("  %-35s %10d %10d",
                        c.getConstraintRef().constraintName(), c.getScore().hardScore(), c.getConstraintMatchCount()));
            }            // Detailausgabe: welche Event-Paare sind betroffen?
            System.out.println("\n--- Hard-Constraint-Details (betroffene Events) ---");
            for (ConstraintMatchTotal<HardSoftScore> c : hardConstraints) {
                System.out.println("\n  [" + c.getConstraintRef().constraintName() + "]");
                for (ConstraintMatch<HardSoftScore> match : c.getConstraintMatchSet()) {
                    List<Object> justification = match.getIndictedObjectList();
                    StringBuilder sb = new StringBuilder("    ");
                    for (Object obj : justification) {
                        if (obj instanceof Event ev) {
                            String pinned = ev.getFixedWeekend() != null ? " [FIXED]" : "";
                            String date = ev.getWeekend() != null ? ev.getWeekend().getDate().toString() : "UNASSIGNED";
                            sb.append(ev.getName()).append(" (").append(ev.getType()).append(" ").append(ev.getAgeCategory())
                              .append(", ").append(date).append(pinned).append(")  vs  ");
                        }
                    }
                    // Trailing " vs " entfernen
                    String detail = sb.toString();
                    if (detail.endsWith("  vs  ")) {
                        detail = detail.substring(0, detail.length() - 6);
                    }
                    System.out.println(detail);
                }
            }        }

        System.out.println("\n--- Soft-Constraints (größte Faktoren zuerst) ---");
        if (softConstraints.isEmpty()) {
            System.out.println("  Keine Soft-Constraint-Verletzungen.");
        } else {
            System.out.println(String.format("  %-35s %10s %10s", "Constraint", "Score", "Matches"));
            for (ConstraintMatchTotal<HardSoftScore> c : softConstraints) {
                System.out.println(String.format("  %-35s %10d %10d",
                        c.getConstraintRef().constraintName(), c.getScore().softScore(), c.getConstraintMatchCount()));
            }
        }

        // Score-Erklärung in Datei schreiben
        try (java.io.PrintWriter writer = new java.io.PrintWriter("score_explanation.txt")) {
            writer.println("===== SCORE EXPLANATION =====\n");
            writer.println("Overall Score: " + solution.getScore());
            writer.println("  Hard-Score: " + solution.getScore().hardScore());
            writer.println("  Soft-Score: " + solution.getScore().softScore());

            writer.println("\n--- Hard-Constraints (größte Verletzungen zuerst) ---");
            if (hardConstraints.isEmpty()) {
                writer.println("  Keine Hard-Constraint-Verletzungen!");
            } else {
                writer.println(String.format("  %-35s %10s %10s", "Constraint", "Score", "Matches"));
                for (ConstraintMatchTotal<HardSoftScore> c : hardConstraints) {
                    writer.println(String.format("  %-35s %10d %10d",
                            c.getConstraintRef().constraintName(), c.getScore().hardScore(), c.getConstraintMatchCount()));
                }
            }

            writer.println("\n--- Soft-Constraints (größte Faktoren zuerst) ---");
            if (softConstraints.isEmpty()) {
                writer.println("  Keine Soft-Constraint-Verletzungen.");
            } else {
                writer.println(String.format("  %-35s %10s %10s", "Constraint", "Score", "Matches"));
                for (ConstraintMatchTotal<HardSoftScore> c : softConstraints) {
                    writer.println(String.format("  %-35s %10d %10d",
                            c.getConstraintRef().constraintName(), c.getScore().softScore(), c.getConstraintMatchCount()));
                }
                writer.println("\n  --- Details ---");
                for (ConstraintMatchTotal<HardSoftScore> c : softConstraints) {
                    writer.println("\n  [" + c.getConstraintRef().constraintName()
                            + "] (Score: " + c.getScore().softScore() + ", Matches: " + c.getConstraintMatchCount() + ")");
                    for (ConstraintMatch<HardSoftScore> match : c.getConstraintMatchSet()) {
                        writer.println("    " + formatMatchDetail(match));
                    }
                }
            }

            writer.println("\n--- Legende ---");
            writer.println("Score:   Gesamtauswirkung des Constraints auf den Score");
            writer.println("         Hard < 0 = Verletzung (muss gelöst werden)");
            writer.println("         Soft < 0 = Strafe (Optimierungspotenzial)");
            writer.println("         Soft > 0 = Belohnung (z.B. Wunschtermine)");
            writer.println("Matches: Anzahl betroffener Event-Kombinationen");
            writer.println("[FIXED]: Event mit festem Termin (nicht verschiebbar)");
        } catch (java.io.FileNotFoundException e) {
            System.err.println("Fehler beim Schreiben der Score-Erklärung: " + e.getMessage());
        }

        // Debug: print any hard-constraint violations for fixed events
        solution.getEvents().stream()
                .filter(e -> e.getFixedWeekend() != null &&
                        (e.getWeekend() == null || !e.getWeekend().equals(e.getFixedWeekend())))
                .forEach(e -> System.out.println("[FIXED VIOLATION] " + e.getName() + " fixed " +
                        e.getFixedWeekend().getDate() + " but assigned " +
                        (e.getWeekend() == null ? "UNASSIGNED" : e.getWeekend().getDate())));

        System.out.println("\n===== RESULT =====\n");
        System.out.println("Score: " + solution.getScore());

        // Liste aller Wochenenden
        System.out.println("\n===== ALL WEEKENDS =====\n");
        System.out.println(String.format("%-12s %-8s %-20s %s", "Date", "Blocked", "Holiday", "Events"));
        System.out.println("-----------------------------------------------------------------");

        solution.getWeekends().stream()
                .sorted(Comparator.comparing(Weekend::getDate))
                .forEach(w -> {
                    String blockedStr = w.isBlocked() ? "YES" : "NO";
                    String holidayStr = getHolidayName(w.getDate());
                    String eventsStr = solution.getEvents().stream()
                            .filter(e -> e.getWeekend() != null && e.getWeekend().equals(w))
                            .map(e -> e.getName() + " (" + e.getClub().getName() + ")")
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("None");
                    System.out.println(String.format("%-12s %-8s %-20s %s",
                            w.getDate().toString(), blockedStr, holidayStr, eventsStr));
                });

        System.out.println("\n===== EVENTS =====\n");
        System.out.println(String.format("%-20s %-25s %-6s %-4s -> %s", "Club", "Event", "Type", "Age", "Date"));
        System.out.println("--------------------------------------------------------------------");

        solution.getEvents().stream()
                .sorted(Comparator.comparing(e ->
                        e.getWeekend() == null ?
                                java.time.LocalDate.MAX :
                                e.getWeekend().getDate()))
                .forEach(e -> {

                    String date = e.getWeekend() == null
                            ? "UNASSIGNED"
                            : e.getWeekend().getDate().toString();
                    String clubName = (e.getClub() == null) ? "UNKNOWN" : e.getClub().getName();

                    System.out.println(
                            String.format("%-20s %-25s %-6s %-4s -> %s",
                                    clubName,
                                    e.getName(),
                                    e.getType(),
                                    e.getAgeCategory(),
                                    date)
                    );
                });

        // Excel-Export
        try {
            ExcelScheduleExporter exporter = new ExcelScheduleExporter(solution, "Turnierplanung.xlsx");
            exporter.export();
        } catch (java.io.IOException e) {
            System.err.println("Fehler beim Excel-Export: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String formatMatchDetail(ConstraintMatch<HardSoftScore> match) {
        List<Object> objects = match.getIndictedObjectList();
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object obj : objects) {
            if (obj instanceof Event ev) {
                if (!first) sb.append("  vs  ");
                first = false;
                String pinned = ev.getFixedWeekend() != null ? " [FIXED]" : "";
                String date = ev.getWeekend() != null ? ev.getWeekend().getDate().toString() : "UNASSIGNED";
                sb.append(ev.getName()).append(" (").append(ev.getType()).append(" ").append(ev.getAgeCategory())
                  .append(", ").append(date).append(pinned).append(")");
            }
        }
        // Score des einzelnen Matches anhängen
        HardSoftScore score = match.getScore();
        if (score.hardScore() != 0) {
            sb.append("  → ").append(score.hardScore()).append(" hard");
        }
        if (score.softScore() != 0) {
            sb.append("  → ").append(score.softScore() > 0 ? "+" : "").append(score.softScore()).append(" soft");
        }
        return sb.toString();
    }
}