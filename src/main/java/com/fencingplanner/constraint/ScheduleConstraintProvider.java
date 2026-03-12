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
qbEquivalentOverlap(factory),
venueAvailability(factory),
athleteOverlap(factory)

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

}