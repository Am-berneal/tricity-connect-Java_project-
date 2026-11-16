package com.tricityconnect.service;

import com.tricityconnect.core.SeatLayout;
import com.tricityconnect.dto.SeatOccupant;
import com.tricityconnect.model.*;
import com.tricityconnect.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Conductor's handheld ETM (electronic ticket machine) screen:
 * who should be in each seat between this stop and the next, who boards here, and ticket/pass validation.
 */
@Service
public class ConductorService {

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final CommuterPassRepository passRepository;

    public ConductorService(TripRepository tripRepository, BookingRepository bookingRepository,
                            CommuterPassRepository passRepository) {
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
        this.passRepository = passRepository;
    }

    @Transactional(readOnly = true)
    public List<Trip> todaysTrips() {
        LocalDateTime now = LocalDateTime.now();
        return tripRepository.findByDepartureTimeBetweenOrderByDepartureTime(now.minusHours(1),
                        LocalDate.now().plusDays(1).atStartOfDay()).stream()
                .filter(t -> t.getStatus() != TripStatus.CANCELLED)
                .toList();
    }

    /** The stop the bus is at (or just left) according to the timetable. */
    public int currentSeq(Trip trip, LocalDateTime now) {
        int seq = 1;
        for (RouteStop s : trip.getRoute().getStops()) {
            if (!trip.expectedAt(s).isAfter(now)) {
                seq = s.getSeq();
            }
        }
        return Math.min(seq, trip.getRoute().getLastStop().getSeq() - 1);
    }

    @Transactional(readOnly = true)
    public List<SeatOccupant> floor(Trip trip, int seq) {
        List<Booking> bookings = bookingRepository.findByTripAndStatus(trip, BookingStatus.CONFIRMED);
        List<CommuterPass> passes = passRepository.findValidForRoute(trip.getRoute().getId(),
                trip.getDepartureTime().toLocalDate(), PassStatus.ACTIVE).stream().filter(p -> p.coversTrip(trip)).toList();
        List<SeatOccupant> out = new ArrayList<>();
        for (String label : SeatLayout.seatLabels(trip.getBus().getCapacity())) {
            Booking here = bookings.stream()
                    .filter(b -> b.getSeatNo().equals(label) && b.getFromSeq() <= seq && seq < b.getToSeq())
                    .findFirst().orElse(null);
            CommuterPass pass = here != null ? null
                    : passes.stream().filter(p -> p.getSeatNo().equals(label)).findFirst().orElse(null);
            boolean freeing = here != null && here.getToSeq() == seq + 1;
            boolean boarding = here != null && here.getFromSeq() == seq;
            out.add(new SeatOccupant(label, SeatLayout.isLadiesSeat(label), here, pass, freeing, boarding));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<Booking> boardingAt(Trip trip, int seq) {
        return bookingRepository.findByTripAndStatus(trip, BookingStatus.CONFIRMED).stream()
                .filter(b -> b.getFromSeq() == seq)
                .sorted(Comparator.comparing(Booking::getSeatNo))
                .toList();
    }

    /**
     * Validates a scanned QR or typed code. Accepts the ticket QR (TRICITY|PNR|trip|seat|a-b),
     * a plain PNR, or a pass QR (TRICITY-PASS|passNo|seat). Returns a message for the conductor.
     */
    @Transactional
    public String validate(Long tripId, String code) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new NotFoundException("Trip not found"));
        String c = code == null ? "" : code.trim();
        if (c.isEmpty()) {
            throw new BookingException("Scan a QR code or type the PNR.");
        }
        boolean passCode = c.startsWith("TRICITY-PASS|") || passRepository.findByPassNo(c.toUpperCase()).isPresent();
        if (passCode) {
            String passNo = c.startsWith("TRICITY-PASS|") ? c.split("\\|")[1] : c.toUpperCase();
            CommuterPass p = passRepository.findByPassNo(passNo)
                    .orElseThrow(() -> new BookingException("Pass " + passNo + " not found."));
            if (!p.coversTrip(trip)) {
                throw new BookingException("Pass " + passNo + " is not valid on this bus (valid on "
                        + p.getRoute().getCode() + " " + p.getMorningDeparture() + " / " + p.getEveningDeparture() + ").");
            }
            p.setTripsUsed(p.getTripsUsed() + 1);
            return "✓ Pass valid: " + p.getUser().getFullName() + ", seat " + p.getSeatNo();
        }
        String pnr = c.startsWith("TRICITY|") ? c.split("\\|")[1] : c.toUpperCase();
        Booking b = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new BookingException("PNR " + pnr + " not found."));
        if (!b.getTrip().getId().equals(tripId)) {
            throw new BookingException("Ticket " + pnr + " is for another bus (" + b.getTrip().getRoute().getCode()
                    + " at " + b.getTrip().getDepartureTime().toLocalTime() + ").");
        }
        if (b.getStatus() != BookingStatus.CONFIRMED) {
            throw new BookingException("Ticket " + pnr + " was cancelled.");
        }
        if (b.isBoarded()) {
            return "Already checked in: " + b.getPassengerName() + ", seat " + b.getSeatNo();
        }
        b.setBoarded(true);
        b.setBoardedAt(LocalDateTime.now());
        String concession = b.getConcession() != null && b.getConcession() != com.tricityconnect.core.Concession.GENERAL
                ? " — check " + b.getConcession().getLabel() + " ID" : "";
        return "✓ Valid: " + b.getPassengerName() + ", seat " + b.getSeatNo() + ", drop at "
                + b.getToStop().getStop().getName() + concession;
    }
}
