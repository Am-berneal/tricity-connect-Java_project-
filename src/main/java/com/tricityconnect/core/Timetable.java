package com.tricityconnect.core;

import java.time.LocalTime;
import java.util.List;

/** Departure times (from the first stop) used on every corridor. */
public final class Timetable {

    public static final List<LocalTime> DEPARTURES = List.of(
            LocalTime.of(7, 15), LocalTime.of(7, 45), LocalTime.of(8, 5), LocalTime.of(8, 20),
            LocalTime.of(8, 35), LocalTime.of(8, 50), LocalTime.of(9, 30), LocalTime.of(11, 0),
            LocalTime.of(13, 0), LocalTime.of(15, 0), LocalTime.of(17, 15), LocalTime.of(17, 45),
            LocalTime.of(18, 0), LocalTime.of(18, 30), LocalTime.of(19, 15), LocalTime.of(21, 0));

    public static final List<LocalTime> MORNING_PASS_TIMES = DEPARTURES.stream()
            .filter(t -> t.isBefore(LocalTime.of(10, 0))).toList();

    public static final List<LocalTime> EVENING_PASS_TIMES = DEPARTURES.stream()
            .filter(t -> !t.isBefore(LocalTime.of(17, 0)) && t.isBefore(LocalTime.of(20, 0))).toList();

    private Timetable() {
    }

    /** Time-slot filter used on the search form. */
    public static boolean inSlot(LocalTime t, String slot) {
        if (slot == null || slot.isBlank() || slot.equals("all")) {
            return true;
        }
        return switch (slot) {
            case "morning" -> !t.isBefore(LocalTime.of(7, 0)) && t.isBefore(LocalTime.of(10, 0));
            case "day" -> !t.isBefore(LocalTime.of(10, 0)) && t.isBefore(LocalTime.of(17, 0));
            case "evening" -> !t.isBefore(LocalTime.of(17, 0));
            default -> true;
        };
    }
}
