package com.tricityconnect.core;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Map;

/**
 * Simple demand forecast used on the Fare Rules calendar:
 * weekday pattern x festival boost, scaled 0-100.
 * Weekdays carry office/college commuters; weekends are quieter except festival days.
 */
public final class DemandForecast {

    /** Festivals and events with heavy Tricity travel (fixed-date ones; movable festivals are added by year). */
    private static final Map<MonthDay, String> FIXED_EVENTS = Map.of(
            MonthDay.of(1, 26), "Republic Day",
            MonthDay.of(8, 15), "Independence Day",
            MonthDay.of(10, 2), "Gandhi Jayanti",
            MonthDay.of(11, 1), "Haryana & Punjab Day",
            MonthDay.of(12, 25), "Christmas",
            MonthDay.of(12, 31), "New Year's Eve");

    private static final Map<LocalDate, String> MOVABLE_EVENTS = Map.of(
            LocalDate.of(2026, 3, 4), "Holi",
            LocalDate.of(2026, 10, 20), "Dussehra",
            LocalDate.of(2026, 11, 8), "Diwali",
            LocalDate.of(2026, 11, 24), "Guru Nanak Jayanti",
            LocalDate.of(2027, 3, 22), "Holi",
            LocalDate.of(2027, 10, 9), "Dussehra",
            LocalDate.of(2027, 10, 29), "Diwali");

    private DemandForecast() {
    }

    public static String eventOn(LocalDate d) {
        String e = MOVABLE_EVENTS.get(d);
        return e != null ? e : FIXED_EVENTS.get(MonthDay.from(d));
    }

    /** 0-100 demand index. */
    public static int index(LocalDate d) {
        double base = switch (d.getDayOfWeek()) {
            case MONDAY -> 82;
            case TUESDAY, WEDNESDAY -> 74;
            case THURSDAY -> 76;
            case FRIDAY -> 88;
            case SATURDAY -> 52;
            default -> 40;
        };
        if (eventOn(d) != null) {
            base = Math.max(base, 70) + 25;
        }
        // the days just before a festival are busy too
        if (eventOn(d.plusDays(1)) != null && d.getDayOfWeek() != DayOfWeek.SUNDAY) {
            base += 10;
        }
        return (int) Math.min(100, Math.round(base));
    }

    public static String level(int index) {
        if (index >= 90) return "Super Peak";
        if (index >= 78) return "High Peak";
        if (index >= 60) return "Moderate";
        return "Low";
    }
}
