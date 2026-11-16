package com.tricityconnect.service;

import com.tricityconnect.dto.TrackStop;
import com.tricityconnect.dto.TrackingView;
import com.tricityconnect.model.Booking;
import com.tricityconnect.model.RouteStop;
import com.tricityconnect.model.Trip;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Live tracking. Real buses would send GPS positions; in this project the position is
 * simulated from the timetable (plus any delay) and the current time.
 */
@Service
public class TrackingService {

    public TrackingView track(Booking booking, LocalDateTime now) {
        Trip trip = booking.getTrip();
        List<RouteStop> stops = trip.getRoute().getStops();
        RouteStop first = stops.get(0);
        RouteStop last = stops.get(stops.size() - 1);
        double totalKm = last.getKmFromStart();
        int totalMinutes = last.getMinutesFromStart();

        LocalDateTime actualStart = trip.getDepartureTime().plusMinutes(trip.getDelayMinutes());
        double minutesRun = Duration.between(actualStart, now).toSeconds() / 60.0;
        boolean departed = minutesRun >= 0;
        boolean finished = minutesRun >= totalMinutes;
        int progress = (int) Math.round(100.0 * Math.max(0, Math.min(minutesRun, totalMinutes)) / totalMinutes);

        List<TrackStop> view = new ArrayList<>();
        String nextStop = null;
        for (RouteStop s : stops) {
            LocalDateTime eta = trip.expectedAt(s);
            String state;
            if (!eta.isAfter(now)) {
                state = "passed";
            } else if (nextStop == null) {
                state = "next";
                nextStop = s.getStop().getName();
            } else {
                state = "upcoming";
            }
            int pos = totalKm == 0 ? 0 : (int) Math.round(100.0 * s.getKmFromStart() / totalKm);
            view.add(new TrackStop(s.getStop().getName(), s.getStop().getCity(), eta, state,
                    s.getSeq() == booking.getFromSeq(), s.getSeq() == booking.getToSeq(), pos,
                    mapX(s.getKmFromStart(), totalKm), mapY(s.getKmFromStart(), totalKm)));
        }

        long toBoarding = Duration.between(now, trip.expectedAt(booking.getFromStop())).toMinutes();
        String status;
        if (finished) {
            status = "Trip completed at " + last.getStop().getName();
        } else if (!departed) {
            status = "Waiting at " + first.getStop().getName() + (trip.getBay() == null ? "" : " (" + trip.getBay() + ")");
        } else {
            status = "On the way to " + nextStop;
        }
        int speed = departed && !finished ? 36 + (int) (trip.getId() % 9) : 0;

        // bus position on the map: find how many km it has covered by interpolating between stops
        double busKm = 0;
        double run = Math.max(0, Math.min(minutesRun, totalMinutes));
        for (int i = 0; i < stops.size() - 1; i++) {
            RouteStop a = stops.get(i);
            RouteStop b = stops.get(i + 1);
            if (run >= a.getMinutesFromStart() && run <= b.getMinutesFromStart()) {
                double f = (run - a.getMinutesFromStart()) / Math.max(1, b.getMinutesFromStart() - a.getMinutesFromStart());
                busKm = a.getKmFromStart() + f * (b.getKmFromStart() - a.getKmFromStart());
                break;
            }
        }
        StringBuilder points = new StringBuilder();
        for (int step = 0; step <= 40; step++) {
            double km = totalKm * step / 40.0;
            points.append(mapX(km, totalKm)).append(',').append(mapY(km, totalKm)).append(' ');
        }
        return new TrackingView(view, progress, toBoarding, departed, finished, status, speed, nextStop,
                points.toString().trim(), mapX(busKm, totalKm), mapY(busKm, totalKm));
    }

    /** Schematic map: route runs left to right across an 800 x 420 picture with a gentle curve. */
    static int mapX(double km, double totalKm) {
        return (int) Math.round(70 + 660 * (totalKm == 0 ? 0 : km / totalKm));
    }

    static int mapY(double km, double totalKm) {
        double f = totalKm == 0 ? 0 : km / totalKm;
        return (int) Math.round(220 - 70 * Math.sin(f * Math.PI * 1.6));
    }
}
