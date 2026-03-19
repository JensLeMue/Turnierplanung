package com.fencingplanner.model;

import ai.timefold.solver.core.api.domain.entity.PinningFilter;

/**
 * PinningFilter für Events mit Festgelegte Termine (fixedWeekend).
 *
 * Ein gepinntes Event wird vom Solver NIEMALS verschoben - es bleibt unverändert auf seinem
 * fixedWeekend. Dies ist notwendig, weil:
 *
 * 1) Die Hard-Constraints für "fixed date" (fixedFIE, fixedEFC, fixedDM) nicht immer ausreichen,
 *    um den Solver vollständig daran zu hindern, ein Event zu verschieben, wenn andere Hard-Constraints
 *    (z.B. Athleten-Overlap) stärkerer Druck ausüben.
 *
 * 2) Durch das Pinning wird das Event von vornherein aus dem Optimierungsprozess ausgeschlossen,
 *    nicht nur durch Constraints bestraft.
 *
 * Dies ist die bevorzugte Lösung für wahrhaft unveränderliche Termine (z.B. externe FIE/EFC-Events).
 */
public class FixedEventPinningFilter implements PinningFilter<Schedule, Event> {

    @Override
    public boolean accept(Schedule schedule, Event event) {
        // Events mit einem fixedWeekend MÜSSEN gepinnt werden (sind mit "fixedWeekend != null" gekennzeichnet).
        // Der Solver wird diese Events niemals verschieben und auch nicht in die Optimierung einbeziehen.
        return event.getFixedWeekend() != null;
    }
}
