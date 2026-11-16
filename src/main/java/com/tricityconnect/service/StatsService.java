package com.tricityconnect.service;

import com.tricityconnect.dto.TripLoad;
import com.tricityconnect.model.*;
import com.tricityconnect.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/** Numbers for the admin dashboard and the reports page. */
@Service
@Transactional(readOnly = true)
public class StatsService {

    public static final int FIRST_HOUR = 6;
    public static final int LAST_HOUR = 22;
    public static final int TREND_DAYS = 8;

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final CommuterPassRepository passRepository;
    private final RouteRepository routeRepository;

    public StatsService(TripRepository tripRepository, BookingRepository bookingRepository,
                        CommuterPassRepository passRepository, RouteRepository routeRepository) {
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
        this.passRepository = passRepository;
        this.routeRepository = routeRepository;
    }

    /** Load of each trip, computed in one pass over the bookings. */
    public List<TripLoad> loads(List<Trip> trips) {
        if (trips.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Booking>> byTrip = bookingRepository.findByTripInAndStatus(trips, BookingStatus.CONFIRMED)
                .stream().collect(Collectors.groupingBy(b -> b.getTrip().getId()));
        List<CommuterPass> passes = passRepository.findAll().stream()
                .filter(p -> p.getStatus() == PassStatus.ACTIVE).toList();
        List<TripLoad> out = new ArrayList<>();
        for (Trip t : trips) {
            List<Booking> bs = byTrip.getOrDefault(t.getId(), List.of());
            int last = t.getRoute().getLastStop().getSeq();
            int pass = (int) passes.stream().filter(p -> p.coversTrip(t)).count();
            int peak = 0;
            for (int seg = 1; seg < last; seg++) {
                final int s = seg;
                int onBoard = (int) bs.stream().filter(b -> b.getFromSeq() <= s && s < b.getToSeq()).count();
                peak = Math.max(peak, onBoard);
            }
            peak = Math.min(t.getBus().getCapacity(), peak + pass);
            int boarded = (int) bs.stream().filter(Booking::isBoarded).count();
            out.add(new TripLoad(t, peak, bs.size(), pass, boarded));
        }
        return out;
    }

    public List<Trip> tripsBetween(LocalDateTime from, LocalDateTime to) {
        return tripRepository.findByDepartureTimeBetweenOrderByDepartureTime(from, to);
    }

    /** Main corridor code for a route (TC-104R -> TC-104). */
    public static String corridor(Route r) {
        return r.isReturnDirection() ? r.getCode().substring(0, r.getCode().length() - 1) : r.getCode();
    }

    // ------------------------------------------------------------ dashboard

    public record HeatRow(Route route, Integer[] cells) {
    }

    public record Recommendation(String level, String title, String detail) {
    }

    public record TrendPoint(LocalDate day, long bookings, int x, int y) {
    }

    public Map<String, Object> dashboard() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> m = new HashMap<>();

        List<Trip> todayTrips = tripsBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        List<TripLoad> todayLoads = loads(todayTrips);
        m.put("tripsToday", todayTrips.size());
        m.put("seatsBookedToday", todayLoads.stream().mapToInt(TripLoad::bookings).sum());
        m.put("activePasses", passRepository.countActive(PassStatus.ACTIVE, today));
        m.put("avgOccupancy", (int) Math.round(todayLoads.stream().mapToInt(TripLoad::percent).average().orElse(0)));
        m.put("activeFleet", todayTrips.stream().map(t -> t.getBus().getId()).distinct().count());

        List<TripLoad> yesterday = loads(tripsBetween(today.minusDays(1).atStartOfDay(), today.atStartOfDay()));
        int yBookings = yesterday.stream().mapToInt(TripLoad::bookings).sum();
        int tBookings = todayLoads.stream().mapToInt(TripLoad::bookings).sum();
        m.put("bookingsChange", yBookings == 0 ? 0 : (int) Math.round(100.0 * (tBookings - yBookings) / yBookings));

        // heatmap: last 7 days, main corridors (both directions merged), average load by departure hour
        List<TripLoad> week = loads(tripsBetween(today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay()));
        Map<String, List<TripLoad>> byCorridor = week.stream()
                .collect(Collectors.groupingBy(l -> corridor(l.trip().getRoute())));
        List<HeatRow> heat = new ArrayList<>();
        List<Recommendation> recs = new ArrayList<>();
        for (Route r : routeRepository.findMainCorridors()) {
            Integer[] cells = new Integer[LAST_HOUR - FIRST_HOUR + 1];
            List<TripLoad> rl = byCorridor.getOrDefault(r.getCode(), List.of());
            for (int h = FIRST_HOUR; h <= LAST_HOUR; h++) {
                final int hour = h;
                OptionalDouble avg = rl.stream().filter(l -> l.trip().getDepartureTime().getHour() == hour)
                        .mapToInt(TripLoad::percent).average();
                cells[h - FIRST_HOUR] = avg.isPresent() ? (int) Math.round(avg.getAsDouble()) : null;
            }
            heat.add(new HeatRow(r, cells));
        }
        // recommendations: the two busiest slots need extra buses, the two emptiest can lose one
        record Cell(HeatRow row, int hour, int load) { }
        List<Cell> cellList = new ArrayList<>();
        for (HeatRow row : heat) {
            for (int i = 0; i < row.cells().length; i++) {
                if (row.cells()[i] != null) cellList.add(new Cell(row, FIRST_HOUR + i, row.cells()[i]));
            }
        }
        cellList.sort(Comparator.comparingInt(Cell::load).reversed());
        cellList.stream().filter(c -> c.load() >= 50).limit(2).forEach(c -> recs.add(new Recommendation(
                c.load() >= 80 ? "CAPACITY ALERT • URGENT" : "CAPACITY WATCH",
                "Add a bus on " + c.row().route().getCode() + " (" + c.row().route().getEndpoints() + "), " + hourLabel(c.hour()),
                "Average peak load " + c.load() + "% over the last 7 days at this hour. Deploy a standby coach.")));
        List<Cell> low = new ArrayList<>(cellList);
        java.util.Collections.reverse(low);
        low.stream().filter(c -> c.load() <= 30).limit(2).forEach(c -> recs.add(new Recommendation("EFFICIENCY OPTIMISER",
                "Reduce frequency on " + c.row().route().getCode() + ", " + hourLabel(c.hour()),
                "Only " + c.load() + "% of seats used. Send one bus to depot fast-charging.")));
        m.put("heat", heat);
        m.put("hours", java.util.stream.IntStream.rangeClosed(FIRST_HOUR, LAST_HOUR).boxed().toList());
        m.put("recommendations", recs.stream().limit(4).toList());

        // trend of seats sold per travel date (last TREND_DAYS days)
        List<Trip> twoWeeks = tripsBetween(today.minusDays(TREND_DAYS - 1).atStartOfDay(), today.plusDays(1).atStartOfDay());
        Map<LocalDate, Long> perDay = bookingRepository.findByTripInAndStatus(twoWeeks, BookingStatus.CONFIRMED).stream()
                .collect(Collectors.groupingBy(b -> b.getTrip().getDepartureTime().toLocalDate(), Collectors.counting()));
        long max = Math.max(1, perDay.values().stream().mapToLong(Long::longValue).max().orElse(1));
        List<TrendPoint> trend = new ArrayList<>();
        for (int i = 0; i < TREND_DAYS; i++) {
            LocalDate d = today.minusDays(TREND_DAYS - 1 - i);
            long v = perDay.getOrDefault(d, 0L);
            trend.add(new TrendPoint(d, v, 10 + i * (400 / (TREND_DAYS - 1)), (int) (150 - 130.0 * v / max)));
        }
        m.put("trend", trend);
        m.put("trendPoints", String.join(" ", trend.stream().map(p -> p.x() + "," + p.y()).toList()));
        m.put("trendTotal", trend.stream().mapToLong(TrendPoint::bookings).sum());
        m.put("trendAvg", Math.round(trend.stream().mapToLong(TrendPoint::bookings).average().orElse(0)));
        m.put("trendPeak", trend.stream().mapToLong(TrendPoint::bookings).max().orElse(0));

        // next 10 departures
        m.put("nextTrips", todayLoads.stream()
                .filter(l -> l.trip().getDepartureTime().plusMinutes(l.trip().getDelayMinutes()).isAfter(now.minusMinutes(5)))
                .limit(10).toList());
        return m;
    }

    private static String hourLabel(int h) {
        return LocalTime.of(h, 0).format(java.time.format.DateTimeFormatter.ofPattern("h a")) + "–"
                + LocalTime.of((h + 1) % 24, 0).format(java.time.format.DateTimeFormatter.ofPattern("h a"));
    }

    // ------------------------------------------------------------ reports

    public record LedgerRow(Route route, int trips, int completed, int avgOccupancy, long passengers,
                            BigDecimal gross, BigDecimal refunds, BigDecimal net) {
    }

    public record Bar(String label, BigDecimal value, int percent) {
    }

    public Map<String, Object> report(LocalDate from, LocalDate to, String corridorCode) {
        Map<String, Object> m = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        List<Trip> trips = tripsBetween(from.atStartOfDay(), to.plusDays(1).atStartOfDay()).stream()
                .filter(t -> corridorCode == null || corridorCode.isBlank() || corridor(t.getRoute()).equals(corridorCode))
                .toList();
        List<TripLoad> loads = loads(trips);
        List<Booking> all = trips.isEmpty() ? List.of() : bookingRepository.findByTripDepartureBetween(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay()).stream()
                .filter(b -> corridorCode == null || corridorCode.isBlank() || corridor(b.getTrip().getRoute()).equals(corridorCode))
                .toList();
        List<Booking> confirmed = all.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).toList();
        List<Booking> cancelled = all.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).toList();

        BigDecimal gross = sum(confirmed.stream().map(Booking::getTotalFare).toList())
                .add(sum(cancelled.stream().map(Booking::getTotalFare).toList()));
        BigDecimal refunds = sum(cancelled.stream().map(b -> b.getRefundAmount() == null ? BigDecimal.ZERO : b.getRefundAmount()).toList());
        m.put("revenue", gross.subtract(refunds));
        m.put("gross", gross);
        m.put("refunds", refunds);
        m.put("passengers", confirmed.size());
        m.put("cancellations", cancelled.size());
        m.put("trips", trips.size());
        m.put("avgOccupancy", (int) Math.round(loads.stream().mapToInt(TripLoad::percent).average().orElse(0)));
        m.put("avgFare", confirmed.isEmpty() ? BigDecimal.ZERO
                : sum(confirmed.stream().map(Booking::getTotalFare).toList())
                .divide(BigDecimal.valueOf(confirmed.size()), 2, java.math.RoundingMode.HALF_UP));

        // revenue by corridor
        Map<String, BigDecimal> rev = new TreeMap<>();
        for (Booking b : all) {
            BigDecimal kept = b.getStatus() == BookingStatus.CONFIRMED ? b.getTotalFare()
                    : b.getTotalFare().subtract(b.getRefundAmount() == null ? BigDecimal.ZERO : b.getRefundAmount());
            rev.merge(corridor(b.getTrip().getRoute()), kept, BigDecimal::add);
        }
        BigDecimal maxRev = rev.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
        List<Bar> revBars = new ArrayList<>();
        Map<String, Route> corridors = new LinkedHashMap<>();
        routeRepository.findMainCorridors().forEach(r -> corridors.put(r.getCode(), r));
        rev.entrySet().stream().sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed()).forEach(e -> {
            Route r = corridors.get(e.getKey());
            revBars.add(new Bar(e.getKey() + (r != null ? " · " + r.getEndpoints() : ""), e.getValue(),
                    maxRev.signum() == 0 ? 0 : e.getValue().multiply(BigDecimal.valueOf(100)).divide(maxRev, 0, java.math.RoundingMode.HALF_UP).intValue()));
        });
        m.put("revenueBars", revBars);

        // occupancy by hour (line chart points)
        List<Integer> hours = new ArrayList<>();
        List<String> pts = new ArrayList<>();
        Map<Integer, Integer> occByHour = new TreeMap<>();
        for (int h = FIRST_HOUR; h <= LAST_HOUR; h++) {
            final int hour = h;
            OptionalDouble avg = loads.stream().filter(l -> l.trip().getDepartureTime().getHour() == hour)
                    .mapToInt(TripLoad::percent).average();
            if (avg.isPresent()) {
                int v = (int) Math.round(avg.getAsDouble());
                occByHour.put(h, v);
                hours.add(h);
                pts.add((20 + (h - FIRST_HOUR) * 40) + "," + (200 - v * 18 / 10));
            }
        }
        m.put("occByHour", occByHour);
        m.put("occPoints", String.join(" ", pts));
        Map.Entry<Integer, Integer> peakHour = occByHour.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
        m.put("peakHour", peakHour);

        // boarding by state (UT Chandigarh / Punjab side / Haryana side)
        Map<String, Long> byState = confirmed.stream().collect(Collectors.groupingBy(
                b -> b.getFromStop().getStop().getState(), TreeMap::new, Collectors.counting()));
        m.put("byState", byState);
        long total = Math.max(1, confirmed.size());
        long ut = byState.getOrDefault("UT Chandigarh", 0L);
        long pb = byState.getOrDefault("Punjab", 0L);
        m.put("utPct", Math.round(100.0 * ut / total));
        m.put("pbPct", Math.round(100.0 * pb / total));
        m.put("hrPct", 100 - Math.round(100.0 * ut / total) - Math.round(100.0 * pb / total));

        // ladies seat use per corridor: share of ladies seat-trips that were booked
        Map<String, Integer> ladies = new TreeMap<>();
        Map<String, List<Trip>> tripsByCorridor = trips.stream().collect(Collectors.groupingBy(t -> corridor(t.getRoute())));
        Map<Long, List<Booking>> confByTrip = confirmed.stream().collect(Collectors.groupingBy(b -> b.getTrip().getId()));
        for (var e : tripsByCorridor.entrySet()) {
            long seatsOffered = 0;
            long seatsUsed = 0;
            for (Trip t : e.getValue()) {
                seatsOffered += 8;
                seatsUsed += confByTrip.getOrDefault(t.getId(), List.of()).stream()
                        .filter(b -> com.tricityconnect.core.SeatLayout.isLadiesSeat(b.getSeatNo()))
                        .map(Booking::getSeatNo).distinct().count();
            }
            ladies.put(e.getKey(), seatsOffered == 0 ? 0 : (int) Math.round(100.0 * seatsUsed / seatsOffered));
        }
        m.put("ladies", ladies);

        // ledger
        List<LedgerRow> ledger = new ArrayList<>();
        for (Route r : corridors.values()) {
            List<TripLoad> rl = loads.stream().filter(l -> corridor(l.trip().getRoute()).equals(r.getCode())).toList();
            if (rl.isEmpty()) {
                continue;
            }
            List<Booking> rb = all.stream().filter(b -> corridor(b.getTrip().getRoute()).equals(r.getCode())).toList();
            BigDecimal g = sum(rb.stream().map(Booking::getTotalFare).toList());
            BigDecimal rf = sum(rb.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED)
                    .map(b -> b.getRefundAmount() == null ? BigDecimal.ZERO : b.getRefundAmount()).toList());
            ledger.add(new LedgerRow(r, rl.size(),
                    (int) rl.stream().filter(l -> l.trip().getDepartureTime().isBefore(now)).count(),
                    (int) Math.round(rl.stream().mapToInt(TripLoad::percent).average().orElse(0)),
                    rb.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count(), g, rf, g.subtract(rf)));
        }
        m.put("ledger", ledger);
        return m;
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
