package com.fencingplanner;

import com.fencingplanner.model.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class DataLoader {

    private Map<String, Club> clubs = new HashMap<>();


    public Schedule loadSchedule() {

        List<Club> clubList = loadClubs();
        List<Weekend> weekends = loadWeekends();
        List<Event> events = new ArrayList<>();

        events.addAll(loadFixedEvents(weekends));
        events.addAll(loadApplications());

        return new Schedule(weekends, events);
    }

    // ------------------------------------------------
    // CLUBS
    // ------------------------------------------------

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

                Club club = clubs.get(type);

                Event e = new Event(eventIdCounter++, name, age, club, type);

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

                Club club = clubs.get(clubName);

                Event e = new Event(
                        eventIdCounter++,
                        type + "_" + age + "_" + counter++,
                        age,
                        club,
                        type
                );

                events.add(e);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return events;
    }

}