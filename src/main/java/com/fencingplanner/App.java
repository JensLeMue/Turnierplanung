package com.fencingplanner;

import com.fencingplanner.model.*;

import ai.timefold.solver.core.api.solver.*;

import java.util.Comparator;

public class App {

    public static void main(String[] args) {

        DataLoader loader = new DataLoader();

        Schedule problem = loader.loadSchedule();

        SolverFactory<Schedule> factory =
                SolverFactory.createFromXmlResource("solverConfig.xml");

        Solver<Schedule> solver = factory.buildSolver();

        Schedule solution = solver.solve(problem);

        System.out.println("\n===== RESULT =====\n");
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