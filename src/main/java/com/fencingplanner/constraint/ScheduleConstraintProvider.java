package com.fencingplanner.constraint;

import java.time.Month;
import java.time.temporal.ChronoUnit;

import com.fencingplanner.model.AgeCategory;
import com.fencingplanner.model.Event;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import org.jspecify.annotations.NonNull;

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
 *   <li>{@code seasonalGapDistribution} – Verteilung über die Saison anhand aufeinanderfolgender Events (Gewicht: ×1)</li>
 *   <li>{@code eventsBeforeDm} – Nationale/Regionale Turniere sollen vor der DM liegen (Gewicht: ×10)</li>
 *   <li>{@code u17SeptemberQualifier} – U17 soll mindestens ein QB/Challenge im September haben</li>
 *   <li>{@code u17PostDmQualifier} – U17 soll mindestens ein QB/Challenge nach der DM haben (Folgesaison)</li>
 *   <li>{@code preferredDateReward} – Belohnung für Zuweisung auf Wunschtermine des Veranstalters (Gewicht: ×20)</li>
 * </ul>
 */
public class ScheduleConstraintProvider implements ConstraintProvider {

    public ScheduleConstraintProvider() {}

    @Override
    public Constraint @NonNull[] defineConstraints(@NonNull ConstraintFactory factory){

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
minWeeksBetweenU15Challenges(factory),
maxWeeksBetweenU15Challenges(factory),
            u15ChallengeWinterAnchor(factory),
dmWeekendConstraints(factory),
dmBeforeEmWm(factory),
dmAfterQ(factory),
seasonalGapDistribution(factory),
eventsBeforeDm(factory),
u17SeptemberQualifier(factory),
u17AtMostOneSeptemberQualifier(factory),
u17PostDmQualifier(factory),
u17PostDmQualifierTiming(factory),
senNovemberTournament(factory),
senJanuaryTournament(factory),
senMarchTournament(factory),
senNationalQPreferredMonths(factory),
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
 * SOFT (streng): U15-Challenge-Turniere sollen mit größerem Abstand geplant werden.
 * Mindestabstand: 4 Wochen (nur CHALLENGE, nur U15).
 */
private Constraint minWeeksBetweenU15Challenges(ConstraintFactory factory) {
    return factory.forEachUniquePair(Event.class,
            Joiners.filtering((a, b) ->
                    a.getType().equals("CHALLENGE") && b.getType().equals("CHALLENGE") &&
                    a.getAgeCategory() == AgeCategory.U15 && b.getAgeCategory() == AgeCategory.U15))

            .filter((a, b) -> a.getWeekend() != null && b.getWeekend() != null)

            .filter((a, b) -> {
                long weeks = Math.abs(ChronoUnit.WEEKS.between(a.getWeekend().getDate(), b.getWeekend().getDate()));
                return weeks < 4;
            })

            .penalize(HardSoftScore.ONE_SOFT, (a, b) -> {
                long weeks = Math.abs(ChronoUnit.WEEKS.between(a.getWeekend().getDate(), b.getWeekend().getDate()));
                return (int) (4 - weeks) * 20;
            })
            .asConstraint("min weeks between u15 challenge tournaments");
}

        /**
         * SOFT (gleichmaessig): U15-Challenge-Turniere sollen nicht zu weit auseinander liegen.
         * Bewertet nur aufeinanderfolgende U15-Challenges und bestraft Luecken groesser als 7 Wochen.
         */
        private Constraint maxWeeksBetweenU15Challenges(ConstraintFactory factory) {
            return factory.forEachUniquePair(Event.class,
                Joiners.filtering((a, b) ->
                    a.getType().equals("CHALLENGE") && b.getType().equals("CHALLENGE") &&
                    a.getAgeCategory() == AgeCategory.U15 && b.getAgeCategory() == AgeCategory.U15))

                .filter((a, b) ->
                    a.getWeekend() != null &&
                    b.getWeekend() != null &&
                    a.getWeekend().getDate().isBefore(b.getWeekend().getDate()))

                .ifNotExists(Event.class,
                    Joiners.filtering((a, b, c) ->
                        c.getType().equals("CHALLENGE") &&
                        c.getAgeCategory() == AgeCategory.U15 &&
                        c.getWeekend() != null &&
                        c.getWeekend().getDate().isAfter(a.getWeekend().getDate()) &&
                        c.getWeekend().getDate().isBefore(b.getWeekend().getDate())))

                .penalize(HardSoftScore.ONE_SOFT, (a, b) -> {
                long weeks = ChronoUnit.WEEKS.between(a.getWeekend().getDate(), b.getWeekend().getDate());
                return weeks > 7 ? (int) (weeks - 7) * 15 : 0;
                })
                .asConstraint("max weeks between u15 challenge tournaments");
        }

    /**
     * HARD: U15-Challenge muss im Winter mindestens einmal stattfinden,
     * damit keine zu grosse Luecke von Herbst bis spaeter Saison entsteht.
     */
    private Constraint u15ChallengeWinterAnchor(ConstraintFactory factory) {
        return factory.forEach(Event.class)
            .filter(e -> e.getType().equals("CHALLENGE") && e.getAgeCategory() == AgeCategory.U15)
            .ifNotExists(Event.class,
                Joiners.filtering((ignored, e) ->
                    e.getType().equals("CHALLENGE")
                    && e.getAgeCategory() == AgeCategory.U15
                    && e.getWeekend() != null
                    && (e.getWeekend().getDate().getMonth() == Month.DECEMBER
                        || e.getWeekend().getDate().getMonth() == Month.JANUARY)))
            .penalize(HardSoftScore.ONE_HARD)
            .asConstraint("u15 challenge winter anchor");
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
        // U17 darf bewusst auch ein Q/Challenge nach der DM haben (zählt zur Folgesaison).
        a.getAgeCategory() != AgeCategory.U17 &&
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
 * SOFT (Gewicht ×1): Verteilung über die Saison anhand aufeinanderfolgender Events.
 * Statt alle Paare einer Altersklasse zu vergleichen, werden nur direkte Nachbarn in der Zeitlinie betrachtet.
 * Dadurch entsteht keine übermäßige "Monatsoptimierung" durch weit entfernte Paare.
 */
private Constraint seasonalGapDistribution(ConstraintFactory factory) {
    return factory.forEachUniquePair(Event.class, Joiners.equal(Event::getAgeCategory))
        .filter((a, b) ->
            a.getWeekend() != null &&
            b.getWeekend() != null &&
            a.getWeekend().getDate().isBefore(b.getWeekend().getDate()))
        .ifNotExists(Event.class,
            Joiners.equal((a, b) -> a.getAgeCategory(), Event::getAgeCategory),
            Joiners.filtering((a, b, c) ->
                c.getWeekend() != null &&
                c.getWeekend().getDate().isAfter(a.getWeekend().getDate()) &&
                c.getWeekend().getDate().isBefore(b.getWeekend().getDate())))
        .penalize(HardSoftScore.ONE_SOFT, (a, b) -> {
            long weeksBetween = ChronoUnit.WEEKS.between(a.getWeekend().getDate(), b.getWeekend().getDate());
            long maxGapWeeks = getMaxSeasonGapWeeks(a.getAgeCategory());
            return weeksBetween > maxGapWeeks ? (int) (weeksBetween - maxGapWeeks) : 0;
        })
        .asConstraint("seasonal gap distribution");
}

/**
 * Feinjustierbare obere Lücke (in Wochen) je Altersklasse.
 * Höherer Wert = weitere Verteilung erlaubt, niedriger Wert = dichterer Rhythmus.
 */
private int getMaxSeasonGapWeeks(AgeCategory ageCategory) {
    return switch (ageCategory) {
        case VET -> 12;
        case SEN -> 8;
        case U23 -> 8;
        case U20 -> 12;
        case U17 -> 8;
        case U15 -> 8;
        case U14 -> 8;
        case U13 -> 8;
    };
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
            // U17 darf bewusst ein Turnier nach der DM haben (Folgesaison).
            if (dm.getAgeCategory() == AgeCategory.U17) {
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
     * HARD: U17 benötigt mindestens ein QB/Challenge-Turnier im September.
     */
    private Constraint u17SeptemberQualifier(ConstraintFactory factory) {
        return factory.forEach(Event.class)
        .filter(dm -> dm.getType().equals("DM")
            && dm.getAgeCategory() == AgeCategory.U17
            && dm.getWeekend() != null)
        .ifNotExists(Event.class,
            Joiners.filtering((dm, e) ->
            e.getAgeCategory() == AgeCategory.U17
            && (e.getType().equals("QB") || e.getType().equals("CHALLENGE"))
            && e.getWeekend() != null
            && e.getWeekend().getDate().getMonth() == Month.SEPTEMBER))
        .penalize(HardSoftScore.ONE_HARD)
        .asConstraint("u17 september qualifier");
    }

    /**
     * HARD: Für U17 soll es im September genau ein QB/Challenge geben.
     * (mindestens eins über u17SeptemberQualifier, höchstens eins über diese Regel)
     */
    private Constraint u17AtMostOneSeptemberQualifier(ConstraintFactory factory) {
        return factory.forEachUniquePair(Event.class)
            .filter((a, b) ->
                a.getAgeCategory() == AgeCategory.U17 &&
                b.getAgeCategory() == AgeCategory.U17 &&
                (a.getType().equals("QB") || a.getType().equals("CHALLENGE")) &&
                (b.getType().equals("QB") || b.getType().equals("CHALLENGE")) &&
                a.getWeekend() != null &&
                b.getWeekend() != null &&
                a.getWeekend().getDate().getMonth() == Month.SEPTEMBER &&
                b.getWeekend().getDate().getMonth() == Month.SEPTEMBER)
            .penalize(HardSoftScore.ONE_HARD)
            .asConstraint("u17 at most one september qualifier");
    }

    /**
     * HARD: U17 benötigt mindestens ein QB/Challenge-Turnier nach der U17-DM.
     * Dieses Turnier wird als Auftakt der Folgesaison akzeptiert.
     */
    private Constraint u17PostDmQualifier(ConstraintFactory factory) {
        return factory.forEach(Event.class)
        .filter(dm -> dm.getType().equals("DM")
            && dm.getAgeCategory() == AgeCategory.U17
            && dm.getWeekend() != null)
        .ifNotExists(Event.class,
            Joiners.filtering((dm, e) ->
            e.getAgeCategory() == AgeCategory.U17
            && (e.getType().equals("QB") || e.getType().equals("CHALLENGE"))
            && e.getWeekend() != null
            && e.getWeekend().getDate().isAfter(dm.getWeekend().getDate())))
        .penalize(HardSoftScore.ONE_HARD)
        .asConstraint("u17 post dm qualifier");
    }

    /**
     * SOFT (stark): Das U17-Q/Challenge nach der DM soll zeitnah liegen.
     * Zielwert: innerhalb von 4 Wochen nach der DM.
     */
    private Constraint u17PostDmQualifierTiming(ConstraintFactory factory) {
        return factory.forEachUniquePair(Event.class)
            .filter((a, b) -> {
                Event dm;
                Event q;
                if (a.getType().equals("DM") && a.getAgeCategory() == AgeCategory.U17
                        && (b.getType().equals("QB") || b.getType().equals("CHALLENGE"))
                        && b.getAgeCategory() == AgeCategory.U17) {
                    dm = a;
                    q = b;
                } else if (b.getType().equals("DM") && b.getAgeCategory() == AgeCategory.U17
                        && (a.getType().equals("QB") || a.getType().equals("CHALLENGE"))
                        && a.getAgeCategory() == AgeCategory.U17) {
                    dm = b;
                    q = a;
                } else {
                    return false;
                }
                return dm.getWeekend() != null
                    && q.getWeekend() != null
                    && q.getWeekend().getDate().isAfter(dm.getWeekend().getDate());
            })
            .penalize(HardSoftScore.ONE_SOFT, (a, b) -> {
                Event dm = a.getType().equals("DM") ? a : b;
                Event q = a.getType().equals("DM") ? b : a;
                long weeksAfterDm = ChronoUnit.WEEKS.between(dm.getWeekend().getDate(), q.getWeekend().getDate());
                return weeksAfterDm > 4 ? (int) (weeksAfterDm - 4) * 30 : 0;
            })
            .asConstraint("u17 post dm qualifier timing");
    }

/**
 * HARD: Mindestens ein SEN-Event muss im November liegen.
 */
private Constraint senNovemberTournament(ConstraintFactory factory) {
    return factory.forEach(Event.class)
        .filter(e -> e.getAgeCategory() == AgeCategory.SEN && e.isCountsAsNationalQ())
        .ifNotExists(Event.class,
            Joiners.filtering((ignored, e) ->
                e.getAgeCategory() == AgeCategory.SEN
                && e.isCountsAsNationalQ()
                && e.getWeekend() != null
                && e.getWeekend().getDate().getMonth() == Month.NOVEMBER))
        .penalize(HardSoftScore.ONE_HARD)
        .asConstraint("sen november tournament");
}

/**
 * HARD: Mindestens ein SEN-Event muss im Januar liegen.
 */
private Constraint senJanuaryTournament(ConstraintFactory factory) {
    return factory.forEach(Event.class)
        .filter(e -> e.getAgeCategory() == AgeCategory.SEN && e.isCountsAsNationalQ())
        .ifNotExists(Event.class,
            Joiners.filtering((ignored, e) ->
                e.getAgeCategory() == AgeCategory.SEN
                && e.isCountsAsNationalQ()
                && e.getWeekend() != null
                && e.getWeekend().getDate().getMonth() == Month.JANUARY))
        .penalize(HardSoftScore.ONE_HARD)
        .asConstraint("sen january tournament");
}

/**
 * HARD: Mindestens ein SEN-Event muss im März liegen.
 */
private Constraint senMarchTournament(ConstraintFactory factory) {
    return factory.forEach(Event.class)
        .filter(e -> e.getAgeCategory() == AgeCategory.SEN && e.isCountsAsNationalQ())
        .ifNotExists(Event.class,
            Joiners.filtering((ignored, e) ->
                e.getAgeCategory() == AgeCategory.SEN
                && e.isCountsAsNationalQ()
                && e.getWeekend() != null
                && e.getWeekend().getDate().getMonth() == Month.MARCH))
        .penalize(HardSoftScore.ONE_HARD)
        .asConstraint("sen march tournament");
}

/**
 * SOFT (stark gewichtet): Verschiebbare SEN-Q-Turniere sollen bevorzugt in Nov/Jan/März liegen.
 * Feste Termine werden ausgenommen, damit unvermeidbare externe Vorgaben nicht bestrafen.
 */
private Constraint senNationalQPreferredMonths(ConstraintFactory factory) {
    return factory.forEach(Event.class)
        .filter(e -> e.getAgeCategory() == AgeCategory.SEN
                && e.isCountsAsNationalQ()
                && e.getWeekend() != null
                && e.getFixedWeekend() == null)
        .filter(e -> {
            Month m = e.getWeekend().getDate().getMonth();
            return m != Month.NOVEMBER && m != Month.JANUARY && m != Month.MARCH;
        })
        .penalize(HardSoftScore.ONE_SOFT, e -> 120)
        .asConstraint("sen national q preferred months");
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