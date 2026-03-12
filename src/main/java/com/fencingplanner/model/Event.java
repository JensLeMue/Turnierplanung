package com.fencingplanner.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class Event {

@PlanningId
private long id;

private String name;
private AgeCategory ageCategory;
private Club club;
private String type;

private Weekend fixedWeekend;

@PlanningVariable(valueRangeProviderRefs = {"weekends"})  // Array statt String

private Weekend weekend;

public Event(){}

public Event(long id, String name, AgeCategory ageCategory, Club club, String type){

this.id=id;
this.name=name;
this.ageCategory=ageCategory;
this.club=club;
this.type=type;

}

public long getId(){
return id;
}

public String getName(){
return name;
}

public AgeCategory getAgeCategory(){
return ageCategory;
}

public Club getClub(){
return club;
}

public String getType(){
return type;
}

public Weekend getWeekend(){
return weekend;
}

public void setWeekend(Weekend weekend){
this.weekend=weekend;
}

public Weekend getFixedWeekend(){
return fixedWeekend;
}

public void setFixedWeekend(Weekend fixedWeekend){
this.fixedWeekend=fixedWeekend;
}

}