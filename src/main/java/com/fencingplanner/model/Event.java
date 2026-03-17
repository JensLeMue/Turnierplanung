package com.fencingplanner.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity(
    // PinningFilter: Events mit fixedWeekend != null werden vom Solver nicht verschoben.
    // Dies ist notwendig, da Hard-Constraints allein nicht zuverlässig genug sind,
    // um externe/feste Termine (FIE, EFC) zu bewahren, wenn andere Constraints Druck ausüben.
    // Pinning ist stärker: gepinnte Entities werden von der Optimierung völlig ausgeschlossen.
    pinningFilter = FixedEventPinningFilter.class
)
public class Event {

@PlanningId
private long id;

private String name;
private AgeCategory ageCategory;
private Club club;
private String type;
private boolean countsAsNationalQ;
private String venueAvailability;

private Weekend fixedWeekend;

@PlanningVariable(valueRangeProviderRefs = {"weekends"})  // Array statt String

private Weekend weekend;

/**
 * Default constructor.
 */
public Event(){}

/**
 * Constructs an Event with the specified parameters.
 * @param id the unique identifier
 * @param name the name of the event
 * @param ageCategory the age category
 * @param club the organizing club
 * @param type the type of the event
 */
public Event(long id, String name, AgeCategory ageCategory, Club club, String type){

this.id=id;
this.name=name;
this.ageCategory=ageCategory;
this.club=club;
this.type=type;
this.countsAsNationalQ=false;

}

/**
 * Returns the unique identifier of the event.
 * @return the id
 */
public long getId(){
return id;
}

/**
 * Returns the name of the event.
 * @return the name
 */
public String getName(){
return name;
}

/**
 * Returns the age category of the event.
 * @return the age category
 */
public AgeCategory getAgeCategory(){
return ageCategory;
}

/**
 * Returns the organizing club of the event.
 * @return the club
 */
public Club getClub(){
return club;
}

/**
 * Returns the type of the event.
 * @return the type
 */
public String getType(){
return type;
}

/**
 * Returns the assigned weekend for the event.
 * @return the weekend
 */
public Weekend getWeekend(){
return weekend;
}

/**
 * Sets the assigned weekend for the event.
 * @param weekend the weekend to set
 */
public void setWeekend(Weekend weekend){
this.weekend=weekend;
}

/**
 * Returns the fixed weekend for the event, if any.
 * @return the fixed weekend
 */
public Weekend getFixedWeekend(){
return fixedWeekend;
}

/**
 * Sets the fixed weekend for the event.
 * @param fixedWeekend the fixed weekend to set
 */
public void setFixedWeekend(Weekend fixedWeekend){
this.fixedWeekend=fixedWeekend;
}

/**
 * Returns whether this event counts as a national qualification.
 * @return true if it counts as national qualification, false otherwise
 */
public boolean isCountsAsNationalQ(){
return countsAsNationalQ;
}

/**
 * Sets whether this event counts as a national qualification.
 * @param countsAsNationalQ true if it should count as national qualification
 */
public void setCountsAsNationalQ(boolean countsAsNationalQ){
this.countsAsNationalQ=countsAsNationalQ;
}

/**
 * Returns the venue availability for the event.
 * @return the venue availability
 */
public String getVenueAvailability(){
return venueAvailability;
}

/**
 * Sets the venue availability for the event.
 * @param venueAvailability the venue availability to set
 */
public void setVenueAvailability(String venueAvailability){
this.venueAvailability=venueAvailability;
}

}