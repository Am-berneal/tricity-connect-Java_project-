package com.tricityconnect.service;

import com.tricityconnect.core.*;
import com.tricityconnect.dto.PassForm;
import com.tricityconnect.model.*;
import com.tricityconnect.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/** Buying and listing monthly commuter passes. */
@Service
public class PassService {

    private final CommuterPassRepository passRepository;
    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final FareService fareService;
    private final WalletService walletService;

    public PassService(CommuterPassRepository passRepository, RouteRepository routeRepository,
                       TripRepository tripRepository, BookingRepository bookingRepository,
                       UserRepository userRepository, FareService fareService, WalletService walletService) {
        this.fareService = fareService;
        this.walletService = walletService;
        this.passRepository = passRepository;
        this.routeRepository = routeRepository;
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CommuterPass> passesOf(AppUser user) {
        return passRepository.findByUserOrderByValidFromDesc(user);
    }

    /** Single-trip fare for the whole corridor at the morning departure time. */
    public BigDecimal singleFare(Route route, LocalTime morning) {
        return fareService.quote(route.getDistanceKm(), morning).total();
    }

    public BigDecimal price(Route route, LocalTime morning, int months, PassType type) {
        return PassPricing.price(singleFare(route, morning), months, type == PassType.STUDENT_SENIOR, fareService.rules());
    }

    public BigDecimal fullPrice(Route route, LocalTime morning, int months) {
        return PassPricing.fullPrice(singleFare(route, morning), months);
    }

    public static LocalDate endDate(LocalDate start, int months) {
        return start.plusMonths(months).minusDays(1);
    }

    /** Seats already fixed by other passes on the same buses during the same dates. */
    @Transactional(readOnly = true)
    public Set<String> seatsTakenByPasses(Route route, LocalTime morning, LocalTime evening,
                                          LocalDate start, int months) {
        LocalDate end = endDate(start, months);
        Set<String> taken = new HashSet<>();
        for (CommuterPass p : passRepository.findAll()) {
            if (p.getStatus() != PassStatus.ACTIVE) {
                continue;
            }
            boolean datesOverlap = !p.getValidFrom().isAfter(end) && !p.getValidTo().isBefore(start);
            boolean sameMorningBus = p.getRoute().getId().equals(route.getId()) && p.getMorningDeparture().equals(morning);
            boolean sameEveningBus = p.getRoute().getId().equals(route.getId()) && p.getEveningDeparture().equals(evening);
            if (datesOverlap && (sameMorningBus || sameEveningBus)) {
                taken.add(p.getSeatNo());
            }
        }
        return taken;
    }

    @Transactional
    public CommuterPass buy(AppUser user, PassForm form) {
        Route route = routeRepository.findById(form.getRouteId())
                .orElseThrow(() -> new NotFoundException("Corridor not found"));
        LocalTime morning = LocalTime.parse(form.getMorningTime());
        LocalTime evening = LocalTime.parse(form.getEveningTime());
        if (!Timetable.MORNING_PASS_TIMES.contains(morning) || !Timetable.EVENING_PASS_TIMES.contains(evening)) {
            throw new BookingException("Please choose a morning and evening bus from the timetable.");
        }
        int months = form.getMonths();
        fareService.rules().passDiscountFor(months); // validates 1, 3 or 6
        LocalDate start = form.getStartDate();
        if (start.isBefore(LocalDate.now())) {
            throw new BookingException("The pass cannot start in the past.");
        }
        AppUser buyer = userRepository.findById(user.getId()).orElseThrow();
        String seat = form.getSeatNo();
        if (!SeatLayout.isValid(seat, 36)) {
            throw new BookingException("Please choose a valid seat.");
        }
        if (SeatLayout.isLadiesSeat(seat) && buyer.getGender() != Gender.FEMALE) {
            throw new BookingException("Seat " + seat + " is reserved for women passengers.");
        }
        if (seatsTakenByPasses(route, morning, evening, start, months).contains(seat)) {
            throw new BookingException("Seat " + seat + " is already fixed for another pass holder on these buses.");
        }
        LocalDate end = endDate(start, months);
        ensureNoExistingBookings(route, morning, seat, start, end);
        if (route.getReturnRoute() != null) {
            ensureNoExistingBookings(route.getReturnRoute(), evening, seat, start, end);
        }

        CommuterPass pass = new CommuterPass();
        String passNo;
        do {
            passNo = CodeGenerator.passNumber();
        } while (passRepository.existsByPassNo(passNo));
        pass.setPassNo(passNo);
        pass.setUser(buyer);
        pass.setPassType(form.getPassType() == null ? PassType.CORRIDOR : form.getPassType());
        pass.setRoute(route);
        pass.setSeatNo(seat);
        pass.setMorningDeparture(morning);
        pass.setEveningDeparture(evening);
        pass.setValidFrom(start);
        pass.setValidTo(end);
        pass.setMonths(months);
        pass.setPrice(price(route, morning, months, pass.getPassType()));
        pass.setFullPrice(fullPrice(route, morning, months));
        pass.setStatus(PassStatus.ACTIVE);
        CommuterPass saved = passRepository.save(pass);
        walletService.record(buyer, WalletTransaction.Kind.PASS,
                "Monthly commuter pass (" + months + (months == 1 ? " month)" : " months)") + " seat " + seat,
                saved.getPassNo(), route.getCode(), "UPI", saved.getPrice().negate(), false);
        return saved;
    }

    /** A pass cannot take a seat that a passenger has already booked on one of its buses. */
    private void ensureNoExistingBookings(Route route, LocalTime time, String seat, LocalDate start, LocalDate end) {
        List<Trip> trips = tripRepository.findByRouteAndDepartureTimeBetweenOrderByDepartureTime(
                route, start.atStartOfDay(), end.plusDays(1).atStartOfDay()).stream()
                .filter(t -> t.getDepartureTime().toLocalTime().equals(time)).toList();
        if (trips.isEmpty()) {
            return;
        }
        boolean clash = bookingRepository.findByTripInAndStatus(trips, BookingStatus.CONFIRMED).stream()
                .anyMatch(b -> b.getSeatNo().equals(seat));
        if (clash) {
            throw new BookingException("Seat " + seat + " is already booked on one of these buses. Please choose another seat.");
        }
    }
}
