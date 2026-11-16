package com.tricityconnect.service;

import com.tricityconnect.model.*;
import com.tricityconnect.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Admin changes to the route network: add/remove stops on a corridor, create a new corridor.
 *
 * Stops on a route are numbered 1, 2, 3 ... and bookings store those numbers. When a stop is
 * inserted at position p, every stop from p onwards moves up by one, so bookings and short-terminated
 * trips are shifted too — they keep pointing at the same physical stops.
 */
@Service
public class RouteAdminService {

    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final StopRepository stopRepository;
    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;

    public RouteAdminService(RouteRepository routeRepository, RouteStopRepository routeStopRepository,
                             StopRepository stopRepository, BookingRepository bookingRepository,
                             TripRepository tripRepository) {
        this.routeRepository = routeRepository;
        this.routeStopRepository = routeStopRepository;
        this.stopRepository = stopRepository;
        this.bookingRepository = bookingRepository;
        this.tripRepository = tripRepository;
    }

    /** Finds an existing stop by name or creates it. */
    @Transactional
    public Stop findOrCreateStop(String name, String city, String state) {
        if (name == null || name.isBlank()) {
            throw new BookingException("Please enter the stop name.");
        }
        return stopRepository.findByName(name.trim())
                .orElseGet(() -> stopRepository.save(new Stop(name.trim(), city.trim(), state.trim())));
    }

    @Transactional
    public RouteStop insertStop(Long routeId, String name, String city, String state, int afterSeq,
                                double kmFromStart, int minutesFromStart) {
        Route route = routeRepository.findById(routeId).orElseThrow(() -> new NotFoundException("Route not found"));
        List<RouteStop> stops = route.getStops();
        int last = route.getLastStop().getSeq();
        if (afterSeq < 1 || afterSeq > last) {
            throw new BookingException("Choose where the new stop goes.");
        }
        RouteStop prev = route.stopAtSeq(afterSeq);
        RouteStop next = afterSeq < last ? route.stopAtSeq(afterSeq + 1) : null;
        if (kmFromStart <= prev.getKmFromStart() || minutesFromStart <= prev.getMinutesFromStart()) {
            throw new BookingException("Distance and time must be more than at " + prev.getStop().getName()
                    + " (" + prev.getKmFromStart() + " km, " + prev.getMinutesFromStart() + " min).");
        }
        if (next != null && (kmFromStart >= next.getKmFromStart() || minutesFromStart >= next.getMinutesFromStart())) {
            throw new BookingException("Distance and time must be less than at " + next.getStop().getName()
                    + " (" + next.getKmFromStart() + " km, " + next.getMinutesFromStart() + " min).");
        }
        Stop stop = findOrCreateStop(name, city, state);
        if (stops.stream().anyMatch(s -> s.getStop().getId().equals(stop.getId()))) {
            throw new BookingException(stop.getName() + " is already on this route.");
        }
        int position = afterSeq + 1;

        // 1) move later stops out of the way in two steps (the (route, seq) pair must stay unique)
        for (RouteStop s : stops) {
            if (s.getSeq() >= position) s.setSeq(s.getSeq() + 1000);
        }
        routeStopRepository.flush();
        for (RouteStop s : stops) {
            if (s.getSeq() >= 1000) s.setSeq(s.getSeq() - 1000 + 1);
        }
        routeStopRepository.flush();

        // 2) add the new stop
        RouteStop added = new RouteStop(route, stop, position, kmFromStart, minutesFromStart);
        stops.add(position - 1, added);
        if (next == null) {
            route.setDistanceKm(kmFromStart);
        }
        routeRepository.saveAndFlush(route);

        // 3) keep bookings on the same physical stops
        bookingRepository.shiftFromSeq(routeId, position, 1);
        bookingRepository.shiftToSeq(routeId, position, 1);
        tripRepository.shiftTerminateSeq(routeId, position, 1);
        return added;
    }

    @Transactional
    public void removeStop(Long routeId, int seq) {
        Route route = routeRepository.findById(routeId).orElseThrow(() -> new NotFoundException("Route not found"));
        int last = route.getLastStop().getSeq();
        if (seq <= 1 || seq >= last) {
            throw new BookingException("The first and last stop of a corridor cannot be removed.");
        }
        long used = bookingRepository.countUsingStop(routeId, seq);
        if (used > 0) {
            throw new BookingException("This stop has " + used + " bookings (boarding or drop). It cannot be removed.");
        }
        RouteStop target = route.stopAtSeq(seq);
        route.getStops().remove(target);
        routeRepository.saveAndFlush(route);
        for (RouteStop s : route.getStops()) {
            if (s.getSeq() > seq) s.setSeq(s.getSeq() + 1000);
        }
        routeStopRepository.flush();
        for (RouteStop s : route.getStops()) {
            if (s.getSeq() >= 1000) s.setSeq(s.getSeq() - 1000 - 1);
        }
        routeStopRepository.flush();
        bookingRepository.shiftFromSeq(routeId, seq + 1, -1);
        bookingRepository.shiftToSeq(routeId, seq + 1, -1);
        tripRepository.shiftTerminateSeq(routeId, seq + 1, -1);
    }

    /** Creates a new corridor with its first and last stop, in both directions. */
    @Transactional
    public Route createCorridor(String code, String name, String tagline, int frequency,
                                Long firstStopId, Long lastStopId, double km, int minutes) {
        String c = code == null ? "" : code.trim().toUpperCase();
        if (!c.matches("TC-\\d{3}")) {
            throw new BookingException("Route code must look like TC-123.");
        }
        if (routeRepository.findByCode(c).isPresent()) {
            throw new BookingException("Route " + c + " already exists.");
        }
        if (firstStopId == null || firstStopId.equals(lastStopId)) {
            throw new BookingException("Choose two different end stops.");
        }
        if (km <= 0 || minutes <= 0) {
            throw new BookingException("Distance and travel time must be more than zero.");
        }
        Stop a = stopRepository.findById(firstStopId).orElseThrow();
        Stop b = stopRepository.findById(lastStopId).orElseThrow();
        String n = name == null || name.isBlank() ? a.getName() + " – " + b.getName() : name.trim();

        Route forward = new Route(c, n, tagline == null || tagline.isBlank() ? "New Express Corridor" : tagline.trim(), km, frequency);
        forward.addStop(a, 0, 0);
        forward.addStop(b, km, minutes);
        forward = routeRepository.save(forward);

        Route back = new Route(c + "R", b.getName() + " – " + a.getName(), forward.getTagline(), km, frequency);
        back.addStop(b, 0, 0);
        back.addStop(a, km, minutes);
        back.setReturnRoute(forward);
        back = routeRepository.save(back);
        forward.setReturnRoute(back);
        return forward;
    }
}
