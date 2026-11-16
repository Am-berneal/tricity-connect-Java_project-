package com.tricityconnect.service;

import com.tricityconnect.core.FareCalculator;
import com.tricityconnect.core.Timetable;
import com.tricityconnect.dto.CorridorCard;
import com.tricityconnect.dto.TripOption;
import com.tricityconnect.model.*;
import com.tricityconnect.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/** Finds buses between two stops and the busy-route cards on the home page. */
@Service
@Transactional(readOnly = true)
public class SearchService {

    private final RouteStopRepository routeStopRepository;
    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final CommuterPassRepository passRepository;
    private final SeatService seatService;
    private final FareService fareService;

    public SearchService(RouteStopRepository routeStopRepository, RouteRepository routeRepository,
                         TripRepository tripRepository, BookingRepository bookingRepository,
                         CommuterPassRepository passRepository, SeatService seatService, FareService fareService) {
        this.fareService = fareService;
        this.routeStopRepository = routeStopRepository;
        this.routeRepository = routeRepository;
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
        this.passRepository = passRepository;
        this.seatService = seatService;
    }

    /**
     * All buses on the given date that stop at fromStop and later at toStop.
     *
     * @param slot "morning", "day", "evening" or "all"
     * @param sort "earliest", "fastest", "cheapest" or "seats"
     */
    public List<TripOption> search(Long fromStopId, Long toStopId, LocalDate date, String slot,
                                   String busType, boolean ladiesOnly, Integer maxFare, String sort) {
        if (fromStopId == null || toStopId == null || fromStopId.equals(toStopId)) {
            return List.of();
        }
        LocalDateTime now = LocalDateTime.now();
        List<TripOption> options = new ArrayList<>();
        var rules = fareService.rules();

        for (RouteStop from : routeStopRepository.findBoardingStopsServing(fromStopId, toStopId)) {
            Route route = from.getRoute();
            RouteStop to = route.getStops().stream()
                    .filter(s -> s.getStop().getId().equals(toStopId) && s.getSeq() > from.getSeq())
                    .findFirst().orElse(null);
            if (to == null) {
                continue;
            }
            List<Trip> trips = tripRepository.findByRouteAndDepartureTimeBetweenOrderByDepartureTime(
                    route, date.atStartOfDay().minusHours(2), date.plusDays(1).atStartOfDay());
            trips = trips.stream()
                    .filter(t -> t.getStatus() != TripStatus.CANCELLED)
                    .filter(t -> t.getTerminatesAtSeq() == null || to.getSeq() <= t.getTerminatesAtSeq())
                    .filter(t -> t.timeAt(from).toLocalDate().equals(date))
                    .filter(t -> t.expectedAt(from).isAfter(now))
                    .filter(t -> Timetable.inSlot(t.timeAt(from).toLocalTime(), slot))
                    .filter(t -> busType == null || busType.isBlank()
                            || t.getBus().getType().name().equals(busType))
                    .toList();
            if (trips.isEmpty()) {
                continue;
            }
            Map<Long, List<Booking>> bookingsByTrip = bookingRepository
                    .findByTripInAndStatus(trips, BookingStatus.CONFIRMED).stream()
                    .collect(Collectors.groupingBy(b -> b.getTrip().getId()));
            List<CommuterPass> passes = passRepository.findValidForRoute(route.getId(), date, PassStatus.ACTIVE);

            for (Trip trip : trips) {
                List<Booking> tb = bookingsByTrip.getOrDefault(trip.getId(), List.of());
                Set<String> taken = seatService.occupiedSeats(trip, from.getSeq(), to.getSeq(), tb, passes);
                int capacity = trip.getBus().getCapacity();
                int seatsLeft = capacity - taken.size();
                int ladiesFree = (int) com.tricityconnect.core.SeatLayout.seatLabels(capacity).stream()
                        .filter(com.tricityconnect.core.SeatLayout::isLadiesSeat)
                        .filter(s -> !taken.contains(s)).count();
                double km = to.getKmFromStart() - from.getKmFromStart();
                var fare = FareService.quote(km, trip.timeAt(from).toLocalTime(), rules);
                String note = freeAfterNote(trip, from, to, seatsLeft, tb, passes);
                options.add(new TripOption(trip, from, to, fare, seatsLeft, ladiesFree, note));
            }
        }

        if (ladiesOnly) {
            options.removeIf(o -> o.ladiesFree() == 0);
        }
        if (maxFare != null) {
            options.removeIf(o -> o.fare().total().intValue() > maxFare);
        }
        Comparator<TripOption> order = switch (sort == null ? "earliest" : sort) {
            case "fastest" -> Comparator.comparingInt(TripOption::minutes);
            case "cheapest" -> Comparator.comparing(o -> o.fare().total());
            case "seats" -> Comparator.comparingInt(TripOption::seatsLeft).reversed();
            default -> Comparator.comparing(TripOption::departAt);
        };
        options.sort(order);
        return options;
    }

    /**
     * If more seats free up part-way along the journey, tell the passenger,
     * e.g. "3 seats free after Mohali Phase 7".
     */
    private String freeAfterNote(Trip trip, RouteStop from, RouteStop to, int seatsLeft,
                                 List<Booking> bookings, List<CommuterPass> passes) {
        for (int seq = from.getSeq() + 1; seq < to.getSeq(); seq++) {
            int later = seatService.seatsLeft(trip, seq, to.getSeq(), bookings, passes);
            if (later > seatsLeft) {
                int extra = later - seatsLeft;
                return extra + (extra == 1 ? " seat" : " seats") + " free after " + trip.getRoute().stopAtSeq(seq).getStop().getName();
            }
        }
        return null;
    }

    /** Cards for the six busiest corridors with the next bus and seats left on it. */
    public List<CorridorCard> corridorCards() {
        LocalDateTime now = LocalDateTime.now();
        List<CorridorCard> cards = new ArrayList<>();
        for (Route route : routeRepository.findMainCorridors()) {
            List<Trip> trips = tripRepository.findByRouteAndDepartureTimeBetweenOrderByDepartureTime(
                    route, now, now.plusDays(2));
            Trip next = trips.stream().filter(t -> t.getStatus() != TripStatus.CANCELLED).findFirst().orElse(null);
            int seatsLeft = 0;
            var rules = fareService.rules();
            var fare = FareService.quote(route.getDistanceKm(), LocalTime.NOON, rules);
            if (next != null) {
                int last = route.getLastStop().getSeq();
                seatsLeft = next.getBus().getCapacity() - seatService.occupiedSeats(next, 1, last).size();
                fare = FareService.quote(route.getDistanceKm(), next.getDepartureTime().toLocalTime(), rules);
            }
            cards.add(new CorridorCard(route, next, seatsLeft, fare));
        }
        return cards;
    }
}
