package com.fencingplanner.constraint;

import com.fencingplanner.model.*;

import ai.timefold.solver.core.api.score.stream.*;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;

public class ScheduleConstraintProvider implements ConstraintProvider {

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
dmWeekendConstraints(factory),
dmBeforeEmWm(factory),
dmAfterQ(factory)

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
&& e.getWeekend()!=e.getFixedWeekend())

.penalize(HardSoftScore.ONE_HARD).asConstraint("FIE fixed");

}

private Constraint fixedEFC(ConstraintFactory factory){

return factory.forEach(Event.class)

.filter(e ->
e.getClub().getName().equals("EFC")
&& e.getFixedWeekend()!=null
&& e.getWeekend()!=e.getFixedWeekend())

.penalize(HardSoftScore.ONE_HARD).asConstraint("EFC fixed");

}

private Constraint fixedDM(ConstraintFactory factory){

return factory.forEach(Event.class)

.filter(e ->
e.getType().equals("DM")
&& e.getFixedWeekend()!=null
&& e.getWeekend()!=e.getFixedWeekend())

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

.filter((dm, q) -> 
    dm.getType().equals("DM") && q.getType().equals("Q") &&
    dm.getAgeCategory() == q.getAgeCategory() &&
    (dm.getAgeCategory() == AgeCategory.SEN || dm.getAgeCategory() == AgeCategory.U23 || 
     dm.getAgeCategory() == AgeCategory.U17 || dm.getAgeCategory() == AgeCategory.U15 ||
     dm.getAgeCategory() == AgeCategory.U13 || dm.getAgeCategory() == AgeCategory.U20) &&
    dm.getWeekend() != null && q.getWeekend() != null &&
    dm.getWeekend().getDate().isBefore(q.getWeekend().getDate())
)

.penalize(HardSoftScore.ONE_HARD).asConstraint("DM after Q");

}

}