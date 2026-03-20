package com.fencingplanner.constraint;

import java.time.temporal.ChronoUnit;

import com.fencingplanner.model.AgeCategory;
import com.fencingplanner.model.Event;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;

/**
 * Definiert alle Hard- und Soft-Constraints für die Turnierplanung.
 *
 * <h3>Hard-Constraints (müssen erfüllt sein):</h3>
 * <ul>
 *   <li>{@code blockedWeekend} – Keine Events auf gesperrten Wochenenden</li>
 *   <li>{@code fixedFIE} – FIE-Events auf ihren festen Terminen</li>
 *   <li>{@code fixedEFC} – EFC-Events auf ihren festen Terminen</li>
 *   <li>{@code fixedDM} – DM-Events auf ihren festen Terminen</li>
 *   <li>{@code qbEquivalentOverlap} – Keine Überlappung von Qualifikationsturnieren gleicher Startberechtigung</li>
 *   <li>{@code venueAvailability} – Events nur an verfügbaren Hallenterminen</li>
 *   <li>{@code athleteOverlap} – Keine zwei Events mit überlappenden Altersklassen am selben Wochenende</li>
 *   <li>{@code dmWeekendConstraints} – DM-Paarungen (U15+U20, U13+U17) am selben Wochenende</li>
 *   <li>{@code dmBeforeEmWm} – DMs vor den zugehörigen EM/WM-Terminen</li>
 *   <li>{@code dmAfterQ} – DMs nach den Qualifikations- und Challenge-Turnieren</li>
 * </ul>
 *
 * <h3>Soft-Constraints (Optimierungsziele, gewichtet):</h3>
 * <ul>
 *   <li>{@code minWeeksBetweenTournaments} – Mindestabstand zwischen Turnieren pro Altersklasse (Gewicht: ×15)</li>
 *   <li>{@code evenMonthlyDistribution} – Gleichmäßige Verteilung über die Saison (Gewicht: ×1)</li>
 *   <li>{@code eventsBeforeDm} – Nationale/Regionale Turniere sollen vor der DM liegen (Gewicht: ×10)</li>
 *   <li>{@code preferredDateReward} – Belohnung für Zuweisung auf Wunschtermine des Veranstalters (Gewicht: ×20)</li>
 * </ul>
 */
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
qbEquivalentOverlapFixed(factory),
venueAvailability(factory),
athleteOverlap(factory),
athleteOverlapFixed(factory),
            minWeeksBetweenTournaments(factory),
dmWeekendConstraints(factory),
dmBeforeEmWm(factory),
dmAfterQ(factory),
evenMonthlyDistribution(factory),
eventsBeforeDm(factory),
preferredDateReward(factory)

};

}

    /**
     * HARD: Keine Events auf gesperrten Wochenenden.
     * Gesperrte Wochenenden sind in weekends.csv mit blocked=true markiert.
     * FIE- und EFC-Events sind ausgenommen, da deren Termine extern vorgegeben sind.
     */
    private Constraint blockedWeekend(ConstraintFactory factory){

return factory.forEach(Event.class)

.filter(e -> e.getWeekend()!=null && e.getWeekend().isBlocked()
    && !e.getType().equals("EFC") && !e.getType().equals("FIE"))

.penalize(HardSoftScore.ONE_HARD).asConstraint("blocked weekend");

}

    /**
     * HARD: FIE-Events (Weltverband) müssen auf ihrem festen Termin bleiben.
     * Nutzt .equals() statt Referenzvergleich, da Timefold Objekte klont.
     */
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

    /**
     * HARD: EFC-Events (Europäischer Verband) müssen auf ihrem festen Termin bleiben.
     */
    private Constraint fixedEFC(ConstraintFactory factory){

return factory.forEach(Event.class)

.filter(e ->
    e.getClub().getName().equals("EFC")
    && e.getFixedWeekend()!=null
    // Nutze .equals() statt !=, da Timefold/OptaPlanner Objekte klont.
    && !e.getWeekend().equals(e.getFixedWeekend()))

.penalize(HardSoftScore.ONE_HARD).asConstraint("EFC fixed");

}

    /**
     * HARD: DM-Events (Deutsche Meisterschaften) müssen auf ihrem festen Termin bleiben,
     * sofern ein fester Termin vorgegeben ist.
     */
    private Constraint fixedDM(ConstraintFactory factory){

return factory.forEach(Event.class)

.filter(e ->
    e.getType().equals("DM")
    && e.getFixedWeekend()!=null
    // Nutze .equals() statt !=, da Timefold/OptaPlanner Objekte klont.
    && !e.getWeekend().equals(e.getFixedWeekend()))

.penalize(HardSoftScore.ONE_HARD).asConstraint("DM fixed");

}

    /**
     * HARD: Qualifikationsturniere (QB) dürfen nicht am selben Wochenende stattfinden,
     * wenn Athleten aufgrund überlappender Altersklassen an beiden teilnehmen könnten.
     * Gilt nur wenn mindestens eines der Events verschiebbar ist.
     * FIE-Events ohne QB-Wertung sind ausgenommen.
     */
    private Constraint qbEquivalentOverlap(ConstraintFactory factory){

return factory.forEachUniquePair(Event.class,
Joiners.equal(Event::getWeekend))

.filter((a,b)->
// Mindestens ein Event muss verschiebbar sein
(a.getFixedWeekend() == null || b.getFixedWeekend() == null)
&&
(a.isCountsAsNationalQ() || b.isCountsAsNationalQ())
&&
!( (a.getClub().getName().equals("FIE") && !a.isCountsAsNationalQ()) || (b.getClub().getName().equals("FIE") && !b.isCountsAsNationalQ()) )
&&
(a.getAgeCategory().canStartIn(b.getAgeCategory())
||
b.getAgeCategory().canStartIn(a.getAgeCategory()))

)

.penalize(HardSoftScore.ONE_HARD).asConstraint("qb equivalent overlap");

}

    /**
     * SOFT: Warnung für QB-Überlappung zwischen zwei fixen Events (beide nicht verschiebbar).
     */
    private Constraint qbEquivalentOverlapFixed(ConstraintFactory factory){

return factory.forEachUniquePair(Event.class,
Joiners.equal(Event::getWeekend))

.filter((a,b)->
// Beide Events sind fix
a.getFixedWeekend() != null && b.getFixedWeekend() != null
&&
(a.isCountsAsNationalQ() || b.isCountsAsNationalQ())
&&
!( (a.getClub().getName().equals("FIE") && !a.isCountsAsNationalQ()) || (b.getClub().getName().equals("FIE") && !b.isCountsAsNationalQ()) )
&&
(a.getAgeCategory().canStartIn(b.getAgeCategory())
||
b.getAgeCategory().canStartIn(a.getAgeCategory()))

)

.penalize(HardSoftScore.ONE_SOFT).asConstraint("qb equivalent overlap (fixed, info)");

}

    /**
     * HARD: Events dürfen nur an Wochenenden stattfinden, an denen die Halle verfügbar ist.
     * Die Verfügbarkeit wird pro Event in applications.csv als Semikolon-getrennte Datumsliste
     * oder "all" (immer verfügbar) angegeben.
     */
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

    /** Prüft ob das zugewiesene Wochenende in der Verfügbarkeitsliste des Events enthalten ist. */
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

    /**
     * HARD: Keine zwei Events mit überlappenden Altersklassen am selben Wochenende.
     * Gilt nur wenn mindestens eines der Events verschiebbar ist.
     * Überlappung wird über AgeCategory.canStartIn() bestimmt (z.B. U17-Athleten
     * dürfen auch bei U20/U23/SEN starten → diese Events dürfen nicht kollidieren).
     */
    private Constraint athleteOverlap(ConstraintFactory factory){

return factory.forEachUniquePair(Event.class,
Joiners.equal(Event::getWeekend))

.filter((a,b)->
// Mindestens ein Event muss verschiebbar sein
(a.getFixedWeekend() == null || b.getFixedWeekend() == null)
&&
(a.getAgeCategory().canStartIn(b.getAgeCategory())
||
b.getAgeCategory().canStartIn(a.getAgeCategory()))

)

.penalize(HardSoftScore.ONE_HARD).asConstraint("athlete overlap");

}

    /**
     * SOFT: Warnung für Altersklassen-Überlappung zwischen zwei fixen Events (beide nicht verschiebbar).
     * Dient als Info-Constraint – diese Konflikte sind durch die externen Termine unvermeidbar.
     */
    private Constraint athleteOverlapFixed(ConstraintFactory factory){

return factory.forEachUniquePair(Event.class,
Joiners.equal(Event::getWeekend))

.filter((a,b)->
// Beide Events sind fix
a.getFixedWeekend() != null && b.getFixedWeekend() != null
&&
(a.getAgeCategory().canStartIn(b.getAgeCategory())
||
b.getAgeCategory().canStartIn(a.getAgeCategory()))

)

.penalize(HardSoftScore.ONE_SOFT).asConstraint("athlete overlap (fixed, info)");

}

    /**
     * SOFT (Gewicht ×15): Mindestabstand in Wochen zwischen Turnieren derselben Altersklasse.
     * Werte aus ageCategoryWeekGap.csv (z.B. U17=2, U15=3, U13=4 Wochen).
     * Strafe = (minWeeks - tatsächlicherAbstand) × 15.
     * Gewicht 15 stellt sicher, dass dieser Constraint wichtiger ist als die Monatsverteilung.
     */
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
                // Faktor 15: Gleicht die Monatsverteilung aus, Mindestabstand ist etwas wichtiger
                return (int) (minWeeks - weeks) * 15;
            })
            .asConstraint("min weeks between tournaments");

}

/**
 * HARD: Bestimmte DM-Paarungen müssen am selben Wochenende stattfinden:
 * - U15 + U20 gemeinsam
 * - U13 + U17 gemeinsam
 */
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

/**
 * HARD: Deutsche Meisterschaften müssen zeitlich vor den zugehörigen EM/WM-Terminen liegen.
 * Zuordnung: DM SEN→EM/WM SEN, DM U23→EM/WM U23, DM U17→EM/WM U17,
 * DM U15→EM/WM U15, DM U13→EM/WM U17, DM U20→EM/WM U15.
 */
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

/**
 * HARD: Deutsche Meisterschaften müssen zeitlich nach den Qualifikationsturnieren (QB)
 * und Challenge-Events derselben Altersklasse liegen. Reihenfolge: QB/CHALLENGE → DM → EM/WM.
 */
private Constraint dmAfterQ(ConstraintFactory factory){

return factory.forEachUniquePair(Event.class)

.filter((a, b) -> {
    boolean aIsDm = a.getType().equals("DM");
    boolean bIsDm = b.getType().equals("DM");
    boolean aIsPreDm = a.getType().equals("QB") || a.getType().equals("CHALLENGE");
    boolean bIsPreDm = b.getType().equals("QB") || b.getType().equals("CHALLENGE");
    return ((aIsDm && bIsPreDm) || (bIsDm && aIsPreDm)) &&
        a.getAgeCategory() == b.getAgeCategory() &&
        a.getWeekend() != null && b.getWeekend() != null;
})

.filter((a, b) -> {
    Event dm = a.getType().equals("DM") ? a : b;
    Event preDm = a.getType().equals("DM") ? b : a;
    return dm.getWeekend().getDate().isBefore(preDm.getWeekend().getDate());
})

.penalize(HardSoftScore.ONE_HARD).asConstraint("DM after Q");

}

/**
 * SOFT (Gewicht ×1): Gleichmäßige Verteilung der Turniere über die Saison.
 * Bestraft Eventpaare derselben Altersklasse, die mehr als 4 Wochen (28 Tage) auseinander liegen.
 * Strafe = Tage über 28 hinaus. Verhindert große Lücken im Turnierkalender.
 */
private Constraint evenMonthlyDistribution(ConstraintFactory factory) {
    return factory.forEachUniquePair(Event.class, Joiners.equal(Event::getAgeCategory))
        .filter((a, b) -> a.getWeekend() != null && b.getWeekend() != null)
        .penalize(HardSoftScore.ONE_SOFT, (a, b) -> {
            long daysBetween = ChronoUnit.DAYS.between(a.getWeekend().getDate(), b.getWeekend().getDate());
            return daysBetween > 28 ? (int) (daysBetween - 28) : 0;
        })
        .asConstraint("even monthly distribution");
}

/**
 * SOFT (Gewicht ×10): Nationale und regionale Turniere sollen vor der DM
 * derselben Altersklasse stattfinden.
 * Feste internationale Events (FIE, EFC, EM, WM) sind ausgenommen.
 * Strafe = Anzahl Wochen nach der DM × 10.
 */
private Constraint eventsBeforeDm(ConstraintFactory factory) {
    return factory.forEachUniquePair(Event.class,
            Joiners.equal(Event::getAgeCategory))
        .filter((a, b) -> a.getWeekend() != null && b.getWeekend() != null)
        .filter((a, b) -> {
            Event dm = null, other = null;
            if (a.getType().equals("DM") && !b.getType().equals("DM")) {
                dm = a; other = b;
            } else if (b.getType().equals("DM") && !a.getType().equals("DM")) {
                dm = b; other = a;
            } else {
                return false;
            }
            // Feste internationale Events (FIE, EFC, EM, WM) überspringen
            String type = other.getType();
            if (type.equals("FIE") || type.equals("EFC") || type.equals("EM") || type.equals("WM")) {
                return false;
            }
            return other.getWeekend().getDate().isAfter(dm.getWeekend().getDate());
        })
        .penalize(HardSoftScore.ONE_SOFT, (a, b) -> {
            Event dm = a.getType().equals("DM") ? a : b;
            Event other = a.getType().equals("DM") ? b : a;
            return (int) ChronoUnit.WEEKS.between(dm.getWeekend().getDate(), other.getWeekend().getDate()) * 10;
        })
        .asConstraint("events before DM");
}

/**
 * Soft-Constraint: Belohnt die Zuweisung eines Events auf einen Wunschtermin des Veranstalters.
 * Wunschtermine werden in applications.csv mit * markiert (z.B. 2026-09-05*).
 * Belohnung = 20 pro Event auf Wunschtermin.
 */
private Constraint preferredDateReward(ConstraintFactory factory) {
    return factory.forEach(Event.class)
        .filter(e -> e.getWeekend() != null
                && e.getPreferredDates() != null)
        .filter(e -> {
            String weekendDate = e.getWeekend().getDate().toString();
            for (String preferred : e.getPreferredDates().split(";")) {
                if (preferred.trim().equals(weekendDate)) {
                    return true;
                }
            }
            return false;
        })
        .reward(HardSoftScore.ONE_SOFT, e -> 20)
        .asConstraint("preferred date reward");
}

}