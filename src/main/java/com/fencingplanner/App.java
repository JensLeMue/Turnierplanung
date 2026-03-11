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

        solution.getEvents().stream()
                .sorted(Comparator.comparing(e ->
                        e.getWeekend() == null ?
                                java.time.LocalDate.MAX :
                                e.getWeekend().getDate()))
                .forEach(e -> {

                    String date = e.getWeekend() == null
                            ? "UNASSIGNED"
                            : e.getWeekend().getDate().toString();

                    System.out.println(
                            String.format("%-25s %-6s %-4s -> %s",
                                    e.getName(),
                                    e.getType(),
                                    e.getAgeCategory(),
                                    date)
                    );
                });
    }
}