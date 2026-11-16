package com.tricityconnect.dto;

import java.util.List;

/** Simulated live position of a bus, worked out from the timetable and the current time. */
public record TrackingView(List<TrackStop> stops, int progressPercent, long minutesToBoarding,
                          boolean departed, boolean finished, String statusText, int speedKmh,
                          String nextStopName, String mapPoints, int busX, int busY) {
}
