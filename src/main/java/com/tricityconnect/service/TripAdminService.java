package com.tricityconnect.service;

import com.tricityconnect.model.*;
import com.tricityconnect.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Admin trip scheduling: create, bulk weekday schedule, change bus, cancel with automatic refunds. */
@Service
public class TripAdminService {

    private static final String[] DRIVERS = {"Kuldeep Singh", "Gurpreet S.", "Rajinder Kumar", "Manjit Kaur",
            "Sukhwinder Singh", "Amandeep Sharma", "Balwinder Singh", "Harjit Gill"};

    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final BookingRepository bookingRepository;
    private final WalletService walletService;

    public TripAdminService(TripRepository tripRepository, RouteRepository routeRepository,
                            BusRepository busRepository, BookingRepository bookingRepository,
                            WalletService walletService) {
        this.tripRepository = tripRepository;
        this.routeRepository = routeRepository;
        this.busRepository = busRepository;
        this.bookingRepository = bookingRepository;
        this.walletService = walletService;
    }

    private Bus usableBus(Long busId) {
        Bus bus = busRepository.findById(busId).orElseThrow(() -> new NotFoundException("Bus not found"));
        if (bus.getStatus() == BusStatus.MAINTENANCE) {
            throw new BookingException("Bus " + bus.getRegNo() + " is in the workshop. Choose another bus.");
        }
        return bus;
    }

    @Transactional
    public Trip create(Long routeId, Long busId, LocalDateTime departure) {
        if (departure == null || departure.isBefore(LocalDateTime.now())) {
            throw new BookingException("Departure must be in the future.");
        }
        Route route = routeRepository.findById(routeId).orElseThrow(() -> new NotFoundException("Route not found"));
        Trip t = new Trip(route, usableBus(busId), departure, "Bay 1",
                DRIVERS[(int) (Math.abs(departure.hashCode()) % DRIVERS.length)]);
        return tripRepository.save(t);
    }

    /** Creates a trip at the same time every weekday between two dates (skips days that already have one). */
    @Transactional
    public int bulkSchedule(Long routeId, Long busId, LocalTime time, LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from) || from.isBefore(LocalDate.now())) {
            throw new BookingException("Choose a valid future date range.");
        }
        if (to.isAfter(from.plusDays(62))) {
            throw new BookingException("Bulk scheduling is limited to about 2 months at a time.");
        }
        Route route = routeRepository.findById(routeId).orElseThrow(() -> new NotFoundException("Route not found"));
        Bus bus = usableBus(busId);
        int created = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }
            LocalDateTime dep = d.atTime(time);
            boolean exists = !tripRepository.findByRouteAndDepartureTimeBetweenOrderByDepartureTime(route, dep, dep).isEmpty();
            if (!exists && dep.isAfter(LocalDateTime.now())) {
                tripRepository.save(new Trip(route, bus, dep, "Bay 1", DRIVERS[created % DRIVERS.length]));
                created++;
            }
        }
        return created;
    }

    @Transactional
    public Trip changeBus(Long tripId, Long busId) {
        Trip t = tripRepository.findById(tripId).orElseThrow(() -> new NotFoundException("Trip not found"));
        Bus bus = usableBus(busId);
        if (bus.getCapacity() < t.getBus().getCapacity()) {
            long booked = bookingRepository.findByTripAndStatus(t, BookingStatus.CONFIRMED).stream()
                    .filter(b -> com.tricityconnect.core.SeatLayout.rowOf(b.getSeatNo()) * 4 > bus.getCapacity()).count();
            if (booked > 0) {
                throw new BookingException("The new bus is smaller and " + booked + " booked seats would not exist.");
            }
        }
        t.setBus(bus);
        return t;
    }

    /** Cancels a trip; every confirmed passenger gets a 100% refund to their wallet. Returns how many. */
    @Transactional
    public int cancel(Long tripId, String reason) {
        Trip t = tripRepository.findById(tripId).orElseThrow(() -> new NotFoundException("Trip not found"));
        if (t.getStatus() == TripStatus.CANCELLED) {
            throw new BookingException("This trip is already cancelled.");
        }
        t.setStatus(TripStatus.CANCELLED);
        List<Booking> bookings = bookingRepository.findByTripAndStatus(t, BookingStatus.CONFIRMED);
        for (Booking b : bookings) {
            b.setStatus(BookingStatus.CANCELLED);
            b.setRefundAmount(b.getTotalFare());
            b.setCancelReason("Trip cancelled by operator" + (reason == null || reason.isBlank() ? "" : ": " + reason.trim()));
            b.setCancelledAt(LocalDateTime.now());
            walletService.record(b.getUser(), WalletTransaction.Kind.REFUND, "Trip cancelled by CTU – full refund",
                    b.getPnr(), t.getRoute().getCode(), "Credit to Wallet", b.getTotalFare(), true);
        }
        return bookings.size();
    }

    @Transactional(readOnly = true)
    public List<Booking> manifest(Trip trip) {
        return bookingRepository.findByTripAndStatus(trip, BookingStatus.CONFIRMED).stream()
                .sorted(java.util.Comparator.comparing(Booking::getFromSeq).thenComparing(Booking::getSeatNo))
                .toList();
    }
}
