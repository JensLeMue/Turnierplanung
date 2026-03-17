package com.fencingplanner;

import com.fencingplanner.model.*;

import ai.timefold.solver.core.api.solver.*;

import java.util.Comparator;

public class App {

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