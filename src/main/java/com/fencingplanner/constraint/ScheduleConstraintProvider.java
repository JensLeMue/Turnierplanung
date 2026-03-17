package com.fencingplanner.constraint;

import com.fencingplanner.model.*;

import ai.timefold.solver.core.api.score.stream.*;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;

import java.time.temporal.ChronoUnit;
import java.time.Month;
import java.util.List;

public class ScheduleConstraintProvider implements ConstraintProvider {

public ScheduleConstraintProvider() {}

@Override
public Constraint[] defineConstraints(ConstraintFactory factory){

return new Constraint[]{

blockedWeekend(factory),
fixedFIE(factory),
fixedEFC(factory),
fixedDM(factory),
qbEquivalentOverlap(factory),
venueAvailability(factory),
athleteOverlap(factory),
            minWeeksBetweenTournaments(factory),
dmBeforeEmWm(factory),
dmAfterQ(factory),
evenMonthlyDistribution(factory)

};

}

private Constraint blockedWeekend(ConstraintFactory factory){

return factory.forEach(Event.class)

.filter(e -> e.getWeekend()!=null && e.getWeekend().isBlocked())

.penalize(HardSoftScore.ONE_HARD).asConstraint("blocked weekend");

}

private Constraint fixedFIE(ConstraintFactory factory){

return factory.forEach(Event.class)

.filter(e ->
    e.getClub().getName().equals("FIE")
    && e.getFixedWeekend()!=null
    // Nutze .equals() statt !=, da Timefold/OptaPlanner Objekte klont.
    // Dadurch entstehen mehrere Weekend-Instanzen für dasselbe Datum.
    // Mit Referenz-Vergleich (!=) würde das nicht funktionieren.
    && !e.getWeekend().equals(e.getFixedWeekend()))

.penalize(HardSoftScore.ONE_HARD).asConstraint("FIE fixed");

}

private Constraint fixedEFC(ConstraintFactory factory){

return factory.forEach(Event.class)

.filter(e ->
    e.getClub().getName().equals("EFC")
    && e.getFixedWeekend()!=null
    // Nutze .equals() statt !=, da Timefold/OptaPlanner Objekte klont.
    && !e.getWeekend().equals(e.getFixedWeekend()))

.penalize(HardSoftScore.ONE_HARD).asConstraint("EFC fixed");

}

private Constraint fixedDM(ConstraintFactory factory){

return factory.forEach(Event.class)

.filter(e ->
    e.getType().equals("DM")
    && e.getFixedWeekend()!=null
    // Nutze .equals() statt !=, da Timefold/OptaPlanner Objekte klont.
    && !e.getWeekend().equals(e.getFixedWeekend()))

.penalize(HardSoftScore.ONE_HARD).asConstraint("DM fixed");

}

private Constraint qbEquivalentOverlap(ConstraintFactory factory){

return factory.forEachUniquePair(Event.class,
Joiners.equal(Event::getWeekend))

.filter((a,b)->
(a.isCountsAsNationalQ() || b.isCountsAsNationalQ())
&&
(a.getAgeCategory().canStartIn(b.getAgeCategory())
||
b.getAgeCategory().canStartIn(a.getAgeCategory()))

)

.penalize(HardSoftScore.ONE_HARD).asConstraint("qb equivalent overlap");

}

private Constraint venueAvailability(ConstraintFactory factory){

return factory.forEach(Event.class)

.filter(e -> 
e.getWeekend() != null
&& e.getVenueAvailability() != null
&& !e.getVenueAvailability().equals("all")
&& !isWeekendAvailable(e)
)

.penalize(HardSoftScore.ONE_HARD).asConstraint("venue availability");

}

private boolean isWeekendAvailable(Event event){

String availability = event.getVenueAvailability();
String weekendDate = event.getWeekend().getDate().toString();

String[] availableDates = availability.split(";");

for(String date : availableDates){
if(date.trim().equals(weekendDate)){
return true;
}
}

return false;

}

private Constraint athleteOverlap(ConstraintFactory factory){

return factory.forEachUniquePair(Event.class,
Joiners.equal(Event::getWeekend))

.filter((a,b)->

a.getAgeCategory().canStartIn(b.getAgeCategory())
||
b.getAgeCategory().canStartIn(a.getAgeCategory())

)

.penalize(HardSoftScore.ONE_HARD).asConstraint("athlete overlap");

}

private Constraint minWeeksBetweenTournaments(ConstraintFactory factory) {

    return factory.forEachUniquePair(Event.class,
            Joiners.equal(Event::getAgeCategory))

            .filter((a, b) ->
                    a.getWeekend() != null && b.getWeekend() != null
            )

            .filter((a, b) -> {
                int minWeeks = a.getAgeCategory().getMinWeeksBetweenTournaments();
                long weeks = Math.abs(ChronoUnit.WEEKS.between(a.getWeekend().getDate(), b.getWeekend().getDate()));
                return weeks < minWeeks;
            })

            .penalize(HardSoftScore.ONE_SOFT, (a, b) -> {
                int minWeeks = a.getAgeCategory().getMinWeeksBetweenTournaments();
                long weeks = Math.abs(ChronoUnit.WEEKS.between(a.getWeekend().getDate(), b.getWeekend().getDate()));
                return (int) (minWeeks - weeks);
            })
            .asConstraint("min weeks between tournaments");

}

private Constraint dmWeekendConstraints(ConstraintFactory factory){

return factory.forEachUniquePair(Event.class,
Joiners.filtering((a, b) -> a.getType().equals("DM") && b.getType().equals("DM")))

.filter((a,b) -> {
    // U15 und U20 DMs sollen am gleichen Wochenende stattfinden
    if ((a.getAgeCategory() == AgeCategory.U15 && b.getAgeCategory() == AgeCategory.U20) ||
        (a.getAgeCategory() == AgeCategory.U20 && b.getAgeCategory() == AgeCategory.U15)) {
        return a.getWeekend() != null && b.getWeekend() != null && !a.getWeekend().equals(b.getWeekend());
    }
    // U13 und U17 DMs sollen am gleichen Wochenende stattfinden
    if ((a.getAgeCategory() == AgeCategory.U13 && b.getAgeCategory() == AgeCategory.U17) ||
        (a.getAgeCategory() == AgeCategory.U17 && b.getAgeCategory() == AgeCategory.U13)) {
        return a.getWeekend() != null && b.getWeekend() != null && !a.getWeekend().equals(b.getWeekend());
    }
    return false;
})

.penalize(HardSoftScore.ONE_HARD).asConstraint("DM weekend constraints");

}

private Constraint dmBeforeEmWm(ConstraintFactory factory){

return factory.forEachUniquePair(Event.class)

.filter((dm, emwm) -> 
    dm.getType().equals("DM") && (emwm.getType().equals("EM") || emwm.getType().equals("WM")) &&
    (
        (dm.getAgeCategory() == AgeCategory.SEN && emwm.getAgeCategory() == AgeCategory.SEN) ||
        (dm.getAgeCategory() == AgeCategory.U23 && emwm.getAgeCategory() == AgeCategory.U23) ||
        (dm.getAgeCategory() == AgeCategory.U17 && emwm.getAgeCategory() == AgeCategory.U17) ||
        (dm.getAgeCategory() == AgeCategory.U15 && emwm.getAgeCategory() == AgeCategory.U15) ||
        (dm.getAgeCategory() == AgeCategory.U13 && emwm.getAgeCategory() == AgeCategory.U17) ||
        (dm.getAgeCategory() == AgeCategory.U20 && emwm.getAgeCategory() == AgeCategory.U15)
    ) &&
    dm.getWeekend() != null && emwm.getWeekend() != null &&
    dm.getWeekend().getDate().isAfter(emwm.getWeekend().getDate())
)

.penalize(HardSoftScore.ONE_HARD).asConstraint("DM before EM or WM");

}

private Constraint dmAfterQ(ConstraintFactory factory){

return factory.forEachUniquePair(Event.class)

.filter((a, b) -> 
    ((a.getType().equals("DM") && b.getType().equals("QB")) ||
     (a.getType().equals("QB") && b.getType().equals("DM"))) &&
    a.getAgeCategory() == b.getAgeCategory() &&
    (a.getAgeCategory() == AgeCategory.SEN || a.getAgeCategory() == AgeCategory.U23 || 
     a.getAgeCategory() == AgeCategory.U17 || a.getAgeCategory() == AgeCategory.U15 ||
     a.getAgeCategory() == AgeCategory.U13 || a.getAgeCategory() == AgeCategory.U20) &&
    a.getWeekend() != null && b.getWeekend() != null
)

.filter((a, b) -> {
    Event dm = a.getType().equals("DM") ? a : b;
    Event q = a.getType().equals("QB") ? a : b;
    return dm.getWeekend().getDate().isBefore(q.getWeekend().getDate());
})

.penalize(HardSoftScore.ONE_HARD).asConstraint("DM after Q");

}

private Constraint evenMonthlyDistribution(ConstraintFactory factory) {
    return factory.forEachUniquePair(Event.class, Joiners.equal(Event::getAgeCategory))
        .filter((a, b) -> a.getWeekend() != null && b.getWeekend() != null)
        .penalize(HardSoftScore.ONE_SOFT, (a, b) -> {
            long daysBetween = ChronoUnit.DAYS.between(a.getWeekend().getDate(), b.getWeekend().getDate());
            // Bestrafe große Abstände (> 4 Wochen), um Häufungen zu vermeiden und gleichmäßige Verteilung zu fördern
            return daysBetween > 28 ? (int) (daysBetween - 28) : 0;
        })
        .asConstraint("even monthly distribution");
}

}