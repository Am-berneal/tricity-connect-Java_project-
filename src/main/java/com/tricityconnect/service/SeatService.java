package com.tricityconnect.service;

import com.tricityconnect.core.SeatLayout;
import com.tricityconnect.core.SeatSegments;
import com.tricityconnect.dto.SeatView;
import com.tricityconnect.dto.SeatView.SeatState;
import com.tricityconnect.model.*;
import com.tricityconnect.repository.BookingRepository;
import com.tricityconnect.repository.CommuterPassRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Works out which seats are free on a trip for a given part of the route.
 * A seat is taken if a confirmed booking overlaps the passenger's stops,
 * or if a commuter pass holds that seat on this trip.
 */
@Service
@Transactional(readOnly = true)
public class SeatService {

    private final BookingRepository bookingRepository;
    private final CommuterPassRepository passRepository;

    public SeatService(BookingRepository bookingRepository, CommuterPassRepository passRepository) {
        this.bookingRepository = bookingRepository;
        this.passRepository = passRepository;
    }

    /** Seats that cannot be booked between fromSeq and toSeq. */
    public Set<String> occupiedSeats(Trip trip, int fromSeq, int toSeq) {
        List<Booking> bookings = bookingRepository.findByTripAndStatus(trip, BookingStatus.CONFIRMED);
        List<CommuterPass> passes = passesFor(trip);
        return occupiedSeats(trip, fromSeq, toSeq, bookings, passes);
    }

    /** Same as above, using bookings and passes already loaded (avoids one query per trip in search). */
    public Set<String> occupiedSeats(Trip trip, int fromSeq, int toSeq,
                                     List<Booking> tripBookings, List<CommuterPass> routePasses) {
        SeatSegments.validate(fromSeq, toSeq);
        Set<String> taken = new HashSet<>();
        for (Booking b : tripBookings) {
            if (SeatSegments.overlaps(fromSeq, toSeq, b.getFromSeq(), b.getToSeq())) {
                taken.add(b.getSeatNo());
            }
        }
        taken.addAll(passSeats(trip, routePasses));
        return taken;
    }

    public Set<String> passSeats(Trip trip, List<CommuterPass> routePasses) {
        Set<String> seats = new HashSet<>();
        for (CommuterPass p : routePasses) {
            if (p.coversTrip(trip)) {
                seats.add(p.getSeatNo());
            }
        }
        return seats;
    }

    public List<CommuterPass> passesFor(Trip trip) {
        LocalDate date = trip.getDepartureTime().toLocalDate();
        return passRepository.findValidForRoute(trip.getRoute().getId(), date, PassStatus.ACTIVE);
    }

    public int seatsLeft(Trip trip, int fromSeq, int toSeq, List<Booking> tripBookings,
                         List<CommuterPass> routePasses) {
        return trip.getBus().getCapacity()
                - occupiedSeats(trip, fromSeq, toSeq, tripBookings, routePasses).size();
    }

    /** Builds the seat map shown on the seat-selection page. */
    public List<SeatView> seatMap(Trip trip, int fromSeq, int toSeq, Gender passengerGender) {
        List<Booking> bookings = bookingRepository.findByTripAndStatus(trip, BookingStatus.CONFIRMED);
        List<CommuterPass> passes = passesFor(trip);
        Set<String> booked = new HashSet<>();
        for (Booking b : bookings) {
            if (SeatSegments.overlaps(fromSeq, toSeq, b.getFromSeq(), b.getToSeq())) {
                booked.add(b.getSeatNo());
            }
        }
        Set<String> passHeld = passSeats(trip, passes);

        List<SeatView> seats = new ArrayList<>();
        for (String label : SeatLayout.seatLabels(trip.getBus().getCapacity())) {
            boolean ladies = SeatLayout.isLadiesSeat(label);
            SeatState state;
            if (booked.contains(label)) {
                state = SeatState.BOOKED;
            } else if (passHeld.contains(label)) {
                state = SeatState.PASS_RESERVED;
            } else if (ladies && passengerGender != Gender.FEMALE) {
                state = SeatState.LADIES_LOCKED;
            } else {
                state = SeatState.AVAILABLE;
            }
            seats.add(new SeatView(label, SeatLayout.rowOf(label), SeatLayout.columnOf(label), ladies,
                    SeatLayout.isWindow(label), state));
        }
        return seats;
    }
}
