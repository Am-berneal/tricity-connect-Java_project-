package com.tricityconnect.service;

import com.tricityconnect.core.SeatLayout;
import com.tricityconnect.dto.DisruptionForm;
import com.tricityconnect.dto.RebookOption;
import com.tricityconnect.model.*;
import com.tricityconnect.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Disruptions (admin side) and the passenger's three choices:
 * free connector bus, rebooking on a later bus, or a 100% refund to the wallet.
 */
@Service
public class DisruptionService {

    private static final DateTimeFormatter WINDOW = DateTimeFormatter.ofPattern("dd MMM, hh:mm a");

    private final DisruptionRepository disruptionRepository;
    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final BookingRepository bookingRepository;
    private final AlertRepository alertRepository;
    private final SeatService seatService;
    private final WalletService walletService;

    public DisruptionService(DisruptionRepository disruptionRepository, TripRepository tripRepository,
                             RouteRepository routeRepository, StopRepository stopRepository,
                             BookingRepository bookingRepository, AlertRepository alertRepository,
                             SeatService seatService, WalletService walletService) {
        this.disruptionRepository = disruptionRepository;
        this.tripRepository = tripRepository;
        this.routeRepository = routeRepository;
        this.stopRepository = stopRepository;
        this.bookingRepository = bookingRepository;
        this.alertRepository = alertRepository;
        this.seatService = seatService;
        this.walletService = walletService;
    }

    // ------------------------------------------------------------------ admin

    @Transactional
    public Disruption publish(DisruptionForm f) {
        if (f.getRouteIds() == null || f.getRouteIds().isEmpty()) {
            throw new BookingException("Choose at least one affected route.");
        }
        if (!f.getEndTime().isAfter(f.getStartTime())) {
            throw new BookingException("End time must be after the start time.");
        }
        List<Route> routes = routeRepository.findAllById(f.getRouteIds());
        Stop newLast = f.getNewLastStopId() == null ? null : stopRepository.findById(f.getNewLastStopId()).orElse(null);
        if (f.getType() == DisruptionType.SHORT_TERMINATED && newLast == null) {
            throw new BookingException("For a short-termination, choose the stop where buses will now end.");
        }

        Disruption d = new Disruption();
        d.setCode(newCode(routes.get(0)));
        d.setType(f.getType());
        String codes = String.join(", ", routes.stream().map(Route::getCode).toList());
        d.setTitle(f.getTitle() != null && !f.getTitle().isBlank() ? f.getTitle().trim()
                : codes + ": " + f.getType().getLabel() + (newLast != null ? " at " + newLast.getName() : ""));
        d.setReason(f.getReason().trim());
        d.setRouteCodes(String.join(",", routes.stream().map(Route::getCode).toList()));
        d.setBorderImpact(f.getBorderImpact());
        d.setNewLastStop(newLast);
        d.setDelayMinutes(Math.max(0, f.getDelayMinutes()));
        d.setStartTime(f.getStartTime());
        d.setEndTime(f.getEndTime());
        d.setOfferConnector(f.isOfferConnector());
        d.setOfferRebook(f.isOfferRebook());
        d.setOfferRefund(f.isOfferRefund());
        d = disruptionRepository.save(d);

        int trips = 0;
        int passengers = 0;
        for (Route route : routes) {
            for (Trip t : tripRepository.findByRouteAndDepartureTimeBetweenOrderByDepartureTime(
                    route, f.getStartTime(), f.getEndTime())) {
                if (t.getStatus() == TripStatus.CANCELLED || t.isDisrupted()) {
                    continue;
                }
                if (!apply(d, t, route, newLast)) {
                    continue;
                }
                trips++;
                for (Booking b : bookingRepository.findByTripAndStatus(t, BookingStatus.CONFIRMED)) {
                    if (b.isAffectedByDisruption()) {
                        passengers++;
                    }
                }
            }
        }
        d.setAffectedTrips(trips);
        d.setAffectedPassengers(passengers);

        Alert alert = new Alert(alertType(d.getType()), d.getTitle(), d.getReason() + offerText(d),
                routes.get(0).getCode(), tags(d), "Valid " + WINDOW.format(d.getStartTime()) + " – "
                + WINDOW.format(d.getEndTime()), 0);
        d.setAlert(alertRepository.save(alert));
        return d;
    }

    /** Changes one trip; returns false if the trip is not really affected. */
    private boolean apply(Disruption d, Trip t, Route route, Stop newLast) {
        switch (d.getType()) {
            case SUSPENDED -> t.setStatus(TripStatus.CANCELLED);
            case SHORT_TERMINATED -> {
                Optional<RouteStop> rs = route.getStops().stream()
                        .filter(s -> s.getStop().getId().equals(newLast.getId())).findFirst();
                if (rs.isEmpty() || rs.get().getSeq() == route.getLastStop().getSeq() || rs.get().getSeq() == 1) {
                    return false; // this route doesn't pass the new last stop
                }
                t.setTerminatesAtSeq(rs.get().getSeq());
                t.setStatus(TripStatus.DISRUPTED);
            }
            case DELAYED, DIVERTED -> {
                t.setDelayMinutes(t.getDelayMinutes() + d.getDelayMinutes());
                t.setStatus(TripStatus.DISRUPTED);
            }
        }
        t.setDisruption(d);
        return true;
    }

    private String newCode(Route r) {
        String base = "DIS-" + LocalDateTime.now().getYear() + "-" + r.getCode().replace("TC-", "").replace("R", "");
        String code = base + "A";
        char suffix = 'A';
        while (disruptionRepository.existsByCode(code)) {
            suffix++;
            code = base + suffix;
        }
        return code;
    }

    private static AlertType alertType(DisruptionType t) {
        return switch (t) {
            case SUSPENDED, SHORT_TERMINATED -> AlertType.SUSPENSION;
            case DELAYED -> AlertType.DELAY;
            case DIVERTED -> AlertType.DETOUR;
        };
    }

    private static String offerText(Disruption d) {
        List<String> offers = new ArrayList<>();
        if (d.isOfferConnector()) offers.add("a free CTU connector bus");
        if (d.isOfferRebook()) offers.add("free rebooking on a later bus");
        if (d.isOfferRefund()) offers.add("a 100% refund to the TriCity Wallet");
        return offers.isEmpty() ? "" : " Affected passengers can choose " + String.join(", ", offers) + ".";
    }

    private static String tags(Disruption d) {
        List<String> t = new ArrayList<>();
        if (d.getDelayMinutes() > 0 && d.getType() != DisruptionType.SUSPENDED) t.add("Expected delay: +" + d.getDelayMinutes() + " mins");
        if (d.getNewLastStop() != null) t.add("Buses end at " + d.getNewLastStop().getName());
        if (d.isOfferConnector()) t.add("Free connector bus");
        if (d.isOfferRefund()) t.add("100% refund option");
        return String.join("|", t);
    }

    @Transactional
    public Disruption resolve(Long id, String note) {
        Disruption d = disruptionRepository.findById(id).orElseThrow(() -> new NotFoundException("Disruption not found"));
        d.setActive(false);
        d.setResolvedAt(LocalDateTime.now());
        d.setResolutionNote(note == null || note.isBlank() ? "Services restored to normal" : note.trim());
        if (d.getAlert() != null) {
            d.getAlert().setActive(false);
        }
        LocalDateTime now = LocalDateTime.now();
        for (Trip t : tripRepository.findByDisruption(d)) {
            if (t.getDepartureTime().isAfter(now)) {
                t.setStatus(TripStatus.SCHEDULED);
                t.setTerminatesAtSeq(null);
                t.setDelayMinutes(Math.max(0, t.getDelayMinutes() - d.getDelayMinutes()));
            }
        }
        return d;
    }

    @Transactional(readOnly = true)
    public List<Disruption> active() {
        return disruptionRepository.findByActiveTrueOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Disruption> resolved() {
        return disruptionRepository.findByActiveFalseOrderByResolvedAtDesc();
    }

    // ------------------------------------------------------------------ passenger

    @Transactional(readOnly = true)
    public List<Booking> affectedBookings(AppUser user) {
        LocalDateTime now = LocalDateTime.now();
        return bookingRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .filter(Booking::isAffectedByDisruption)
                .filter(b -> b.getDropTime().isAfter(now.minusHours(2)))
                .sorted(Comparator.comparing(Booking::getBoardingTime))
                .toList();
    }

    private Booking affected(AppUser user, String pnr) {
        Booking b = bookingRepository.findByPnr(pnr).orElseThrow(() -> new NotFoundException("Booking not found"));
        if (!b.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Booking not found");
        }
        if (!b.isAffectedByDisruption()) {
            throw new BookingException("This booking is not affected by an active disruption.");
        }
        return b;
    }

    /** Later buses on the same route today that still reach the passenger's drop stop. */
    @Transactional(readOnly = true)
    public List<RebookOption> rebookOptions(Booking b, int max) {
        List<RebookOption> out = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        Trip old = b.getTrip();
        LocalDateTime endOfDay = old.getDepartureTime().toLocalDate().plusDays(1).atStartOfDay();
        for (Trip t : tripRepository.findByRouteAndDepartureTimeBetweenOrderByDepartureTime(
                old.getRoute(), old.getDepartureTime(), endOfDay)) {
            if (t.getId().equals(old.getId()) || t.getStatus() == TripStatus.CANCELLED || t.isDisrupted()) {
                continue;
            }
            RouteStop from = t.getRoute().stopAtSeq(b.getFromSeq());
            if (!t.expectedAt(from).isAfter(now)) {
                continue;
            }
            int free = t.getBus().getCapacity() - seatService.occupiedSeats(t, b.getFromSeq(), b.getToSeq()).size();
            if (free > 0) {
                out.add(new RebookOption(t, t.expectedAt(from), free));
                if (out.size() >= max) {
                    break;
                }
            }
        }
        return out;
    }

    @Transactional
    public Booking chooseConnector(AppUser user, String pnr) {
        Booking b = affected(user, pnr);
        if (!b.getTrip().getDisruption().isOfferConnector()) {
            throw new BookingException("A connector bus is not offered for this disruption.");
        }
        b.setDisruptionChoice("CONNECTOR");
        return b;
    }

    /** Passenger keeps the booking as it is (e.g. a short delay is fine for them). */
    @Transactional
    public Booking keep(AppUser user, String pnr) {
        Booking b = affected(user, pnr);
        b.setDisruptionChoice("KEPT");
        return b;
    }

    @Transactional
    public Booking rebook(AppUser user, String pnr, Long tripId) {
        Booking b = affected(user, pnr);
        if (!b.getTrip().getDisruption().isOfferRebook()) {
            throw new BookingException("Rebooking is not offered for this disruption.");
        }
        List<RebookOption> options = rebookOptions(b, 20);
        RebookOption chosen = options.stream()
                .filter(o -> tripId == null || o.trip().getId().equals(tripId))
                .findFirst()
                .orElseThrow(() -> new BookingException("No seats left on later buses today. Please choose the refund."));
        Trip target = tripRepository.lockById(chosen.trip().getId()).orElseThrow();
        Set<String> taken = seatService.occupiedSeats(target, b.getFromSeq(), b.getToSeq());
        boolean woman = b.getPassengerGender() == Gender.FEMALE;
        String seat = null;
        if (!taken.contains(b.getSeatNo()) && (woman || !SeatLayout.isLadiesSeat(b.getSeatNo()))) {
            seat = b.getSeatNo(); // keep the same seat number if possible
        } else {
            for (String s : SeatLayout.seatLabels(target.getBus().getCapacity())) {
                if (!taken.contains(s) && (woman || !SeatLayout.isLadiesSeat(s))) {
                    seat = s;
                    break;
                }
            }
        }
        if (seat == null) {
            throw new BookingException("That bus just filled up. Please choose another bus or the refund.");
        }
        b.setTrip(target);
        b.setSeatNo(seat);
        b.setDisruptionChoice("REBOOKED");
        return b;
    }

    @Transactional
    public Booking refundInFull(AppUser user, String pnr) {
        Booking b = affected(user, pnr);
        Disruption d = b.getTrip().getDisruption();
        if (!d.isOfferRefund()) {
            throw new BookingException("Refund is not offered for this disruption.");
        }
        b.setStatus(BookingStatus.CANCELLED);
        b.setRefundAmount(b.getTotalFare());
        b.setCancelReason("Disruption refund (" + d.getCode() + ")");
        b.setCancelledAt(LocalDateTime.now());
        b.setDisruptionChoice("REFUNDED");
        walletService.record(b.getUser(), WalletTransaction.Kind.DISRUPTION_REFUND,
                "Instant disruption refund (" + d.getCode() + ")", b.getPnr(), b.getTrip().getRoute().getCode(),
                "Credit to Wallet", b.getTotalFare(), true);
        return b;
    }
}
