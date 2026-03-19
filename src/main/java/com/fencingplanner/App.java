package com.fencingplanner;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.fencingplanner.model.Schedule;
import com.fencingplanner.model.Weekend;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;

public class App {

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

        // Score-Erklärung in Datei schreiben
        try (java.io.PrintWriter writer = new java.io.PrintWriter("score_explanation.txt")) {
            writer.println("===== SCORE EXPLANATION =====\n");
            writer.println("Overall Score: " + solution.getScore());
            writer.println("\nDieser Score setzt sich aus Hard- und Soft-Constraints zusammen:");
            writer.println("- Hard-Constraints (müssen erfüllt sein, z.B. feste Termine, keine Überlappungen): " + solution.getScore().hardScore());
            writer.println("- Soft-Constraints (optimierbar, z.B. Mindestabstände, gleichmäßige Verteilung): " + solution.getScore().softScore());
            writer.println("\nDie einzelnen Constraint-Verletzungen können in der Konsole oder durch Debugging analysiert werden.");
            writer.println("Beispiele für Constraints:");
            writer.println("- blocked weekend: Events nicht auf blockierten Wochenenden");
            writer.println("- FIE fixed: FIE-Events auf festen Terminen");
            writer.println("- qb equivalent overlap: Keine Überlappungen bei Qualifikationsturnieren");
            writer.println("- min weeks between tournaments: Mindestabstand zwischen Turnieren derselben Kategorie");
            writer.println("- even monthly distribution: Gleichmäßige Verteilung (neu hinzugefügt)");
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
}