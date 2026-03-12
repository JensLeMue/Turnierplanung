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