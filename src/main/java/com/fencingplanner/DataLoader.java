package com.fencingplanner;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fencingplanner.model.AgeCategory;
import com.fencingplanner.model.Club;
import com.fencingplanner.model.Event;
import com.fencingplanner.model.Schedule;
import com.fencingplanner.model.Weekend;

public class DataLoader {

    private Map<String, Club> clubs = new HashMap<>();


    /**
     * Loads the complete schedule including clubs, weekends, and events.
     * @return the loaded schedule
     */
    public Schedule loadSchedule() {

        List<Club> clubList = loadClubs();
        List<Weekend> weekends = loadWeekends();

        // Konfigurierbarer Mindestabstand zwischen Turnieren pro Altersklasse
        loadAgeCategoryWeekGaps();

        List<Event> events = new ArrayList<>();

        events.addAll(loadFixedEvents(weekends));
        events.addAll(loadApplications());

        return new Schedule(weekends, events);
    }

    // ------------------------------------------------
    // CLUBS
    // ------------------------------------------------

    /**
     * Loads the list of clubs from the clubs.csv file.
     * Anpassbar in: src/main/resources/clubs.csv
     * Format: name
     * @return the list of clubs
     */
    private List<Club> loadClubs() {

        List<Club> clubList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        getClass().getResourceAsStream("/clubs.csv")))) {

            String line = br.readLine(); // header

            while ((line = br.readLine()) != null) {

                String name = line.trim();

                Club club = new Club(name);

                clubList.add(club);
                clubs.put(name, club);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return clubList;
    }

    // ------------------------------------------------
    // WEEKENDS
    // ------------------------------------------------

    /**
     * Loads the list of weekends from the weekends.csv file.
     * Anpassbar in: src/main/resources/weekends.csv
     * Format: date,blocked (z.B. 2026-09-05,false)
     * @return the list of weekends
     */
    private List<Weekend> loadWeekends() {

        List<Weekend> weekends = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        getClass().getResourceAsStream("/weekends.csv")))) {

            String line = br.readLine(); // header

            while ((line = br.readLine()) != null) {

                String[] parts = line.split(",");

                LocalDate date = LocalDate.parse(parts[0]);
                boolean blocked = Boolean.parseBoolean(parts[1]);

                Weekend w = new Weekend(date);
                w.setBlocked(blocked);

                weekends.add(w);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return weekends;
    }

    // ------------------------------------------------
    // FIXED EVENTS (FIE / EFC)
    // ------------------------------------------------

    /**
     * Loads fixed events (FIE/EFC) from the events.csv file and assigns them to their fixed weekends.
     * Anpassbar in: src/main/resources/events.csv
     * Format: name,type,ageCategory,fixedDate,qbEquivalent (z.B. FIE_Basel,FIE,U20,2027-01-02,false)
     * @param weekends the list of available weekends
     * @return the list of fixed events
     */
    private List<Event> loadFixedEvents(List<Weekend> weekends) {

        List<Event> events = new ArrayList<>();
        long eventIdCounter = 1;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        getClass().getResourceAsStream("/events.csv")))) {

            String line = br.readLine(); // header

            while ((line = br.readLine()) != null) {

                String[] p = line.split(",");

                String name = p[0];
                String type = p[1];
                AgeCategory age = AgeCategory.valueOf(p[2]);
                LocalDate date = LocalDate.parse(p[3]);
                boolean qbEquivalent = Boolean.parseBoolean(p[4]);

                Club club = clubs.get(type);

                Event e = new Event(eventIdCounter++, name, age, club, type);
                e.setCountsAsNationalQ(qbEquivalent);

                Optional<Weekend> weekend = weekends.stream()
                        .filter(w -> w.getDate().equals(date))
                        .findFirst();

                weekend.ifPresent(w -> {
                    e.setFixedWeekend(w);
                    e.setWeekend(w);
                });

                events.add(e);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return events;
    }

    // ------------------------------------------------
    // APPLICATION EVENTS
    // ------------------------------------------------

    /**
     * Loads application events from the applications.csv file.
     * Anpassbar in: src/main/resources/applications.csv
     * Format: club,type,ageCategory[,venueAvailability] (z.B. Heidenheim,QB,U17,all)
     * venueAvailability ist optional (Standard: "all"), einzelne Daten mit ; getrennt.
     * Bevorzugte Termine (Wunschtermine) werden mit * markiert (z.B. 2026-09-05*;2026-09-12*;2026-09-19)
     * @return the list of application events
     */
    private List<Event> loadApplications() {

        List<Event> events = new ArrayList<>();
        long eventIdCounter = 1000;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        getClass().getResourceAsStream("/applications.csv")))) {

            String line = br.readLine(); // header

            int counter = 1;

            while ((line = br.readLine()) != null) {

                String[] p = line.split(",");

                String clubName = p[0];
                String type = p[1];
                AgeCategory age = AgeCategory.valueOf(p[2]);
                String rawAvailability = p.length > 3 ? p[3] : "all";

                // Bevorzugte Termine mit * markiert extrahieren
                String venueAvailability;
                String preferredDates = null;
                if (!rawAvailability.equals("all")) {
                    List<String> allDates = new ArrayList<>();
                    List<String> preferred = new ArrayList<>();
                    for (String d : rawAvailability.split(";")) {
                        String trimmed = d.trim();
                        if (trimmed.endsWith("*")) {
                            String date = trimmed.substring(0, trimmed.length() - 1);
                            allDates.add(date);
                            preferred.add(date);
                        } else {
                            allDates.add(trimmed);
                        }
                    }
                    venueAvailability = String.join(";", allDates);
                    if (!preferred.isEmpty()) {
                        preferredDates = String.join(";", preferred);
                    }
                } else {
                    venueAvailability = rawAvailability;
                }

                Club club = clubs.get(clubName);

                Event e = new Event(
                        eventIdCounter++,
                        type + "_" + age + "_" + counter++,
                        age,
                        club,
                        type
                );
                e.setCountsAsNationalQ(type.equals("QB"));
                e.setVenueAvailability(venueAvailability);
                e.setPreferredDates(preferredDates);

                events.add(e);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return events;
    }

    // ------------------------------------------------
    // AGE CATEGORY CONFIGURATION
    // ------------------------------------------------

    /**
     * Loads the minimum weeks gap configuration for age categories from ageCategoryWeekGap.csv.
     * Anpassbar in: src/main/resources/ageCategoryWeekGap.csv
     * Format: ageCategory,minWeeksBetweenTournaments (z.B. U17,2)
     */
    private void loadAgeCategoryWeekGaps() {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        getClass().getResourceAsStream("/ageCategoryWeekGap.csv")))) {

            String line = br.readLine(); // header

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 2) {
                    continue;
                }

                try {
                    AgeCategory ageCategory = AgeCategory.valueOf(parts[0].trim());
                    int minWeeks = Integer.parseInt(parts[1].trim());
                    ageCategory.setMinWeeksBetweenTournaments(minWeeks);
                } catch (IllegalArgumentException e) {
                    // ignore invalid lines
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}