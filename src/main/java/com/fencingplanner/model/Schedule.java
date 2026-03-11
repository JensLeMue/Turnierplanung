package com.fencingplanner.model;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.solution.PlanningScore; // RICHTIG  // ← WICHTIG!
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import java.util.List;

@PlanningSolution
public class Schedule {

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "weekends")
    private List<Weekend> weekends;

    @PlanningEntityCollectionProperty
    private List<Event> events;

    @PlanningScore  // ← MANDATORISCH für Timefold 1.26.0!
    private HardSoftScore score;

    public Schedule() {}

    public Schedule(List<Weekend> weekends, List<Event> events) {
        this.weekends = weekends;
        this.events = events;
    }

    public List<Weekend> getWeekends() { return weekends; }
    public void setWeekends(List<Weekend> weekends) { this.weekends = weekends; }

    public List<Event> getEvents() { return events; }
    public void setEvents(List<Event> events) { this.events = events; }

    public HardSoftScore getScore() { return score; }
    public void setScore(HardSoftScore score) { this.score = score; }
}
