package com.tricityconnect.config;

import com.tricityconnect.core.*;
import com.tricityconnect.model.*;
import com.tricityconnect.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/**
 * Fills an empty database with realistic Tricity data on first start:
 *  - stops in Chandigarh (UT), Mohali/Kharar/Zirakpur/New Chandigarh (Punjab) and Panchkula (Haryana)
 *  - the six busiest corridors from the RITES Comprehensive Mobility Plan, in both directions
 *  - 24 buses, a 7-day timetable, demo users, sample bookings, a commuter pass and alerts
 *
 * Demo logins (password in brackets):
 *   Passenger  9876543210 (password123)   - Harpreet Singh
 *   Passenger  9812345678 (password123)   - Simran Kaur (female, can book ladies seats)
 *   Admin      admin@tricity.in (admin123)
 *   Conductor  conductor@tricity.in (conductor123)
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String UT = "UT Chandigarh";
    private static final String PB = "Punjab";
    private static final String HR = "Haryana";

    private final StopRepository stopRepository;
    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final CommuterPassRepository passRepository;
    private final AlertRepository alertRepository;
    private final PasswordEncoder passwordEncoder;
    private final FareSettingsRepository fareSettingsRepository;
    private final WalletTransactionRepository walletRepository;
    private final com.tricityconnect.service.DisruptionService disruptionService;
    private final boolean enabled;

    /** Days of past bookings created for the dashboard and reports. */
    public static final int HISTORY_DAYS = 7;

    private final Map<String, Stop> stops = new HashMap<>();
    private final Random random = new Random(42); // fixed seed: same demo data every time
    private final Set<String> usedPnrs = new HashSet<>();

    private String uniquePnr() {
        String pnr;
        do {
            pnr = CodeGenerator.pnr();
        } while (!usedPnrs.add(pnr));
        return pnr;
    }

    public DataSeeder(StopRepository stopRepository, RouteRepository routeRepository, BusRepository busRepository,
                      TripRepository tripRepository, BookingRepository bookingRepository,
                      UserRepository userRepository, CommuterPassRepository passRepository,
                      AlertRepository alertRepository, PasswordEncoder passwordEncoder,
                      FareSettingsRepository fareSettingsRepository, WalletTransactionRepository walletRepository,
                      com.tricityconnect.service.DisruptionService disruptionService,
                      @Value("${app.seed-demo-data:true}") boolean enabled) {
        this.fareSettingsRepository = fareSettingsRepository;
        this.walletRepository = walletRepository;
        this.disruptionService = disruptionService;
        this.stopRepository = stopRepository;
        this.routeRepository = routeRepository;
        this.busRepository = busRepository;
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.passRepository = passRepository;
        this.alertRepository = alertRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled || stopRepository.count() > 0) {
            return;
        }
        log.info("Seeding TriCity Connect demo data ...");
        seedStops();
        List<Route> routes = seedRoutes();
        List<Bus> buses = seedBuses();
        Map<String, AppUser> users = seedUsers();
        List<Trip> trips = seedTrips(routes, buses);
        CommuterPass pass = seedPass(users.get("harpreet"), routeRepository.findByCode("TC-104").orElseThrow());
        seedDemoBookings(users, trips);
        seedRandomBookings(users.get("crowd"), trips, pass);
        seedAlerts();
        fareSettingsRepository.save(FareSettings.from(FareRules.DEFAULT));
        seedWallet(users.get("harpreet"), pass);
        seedDisruptions();
        log.info("Demo data ready: {} stops, {} routes, {} buses, {} trips, {} bookings",
                stopRepository.count(), routeRepository.count(), busRepository.count(),
                tripRepository.count(), bookingRepository.count());
    }

    // ---------------------------------------------------------------- stops

    private void seedStops() {
        stop("Sector 17 ISBT", "Chandigarh", UT);
        stop("Sector 43 ISBT", "Chandigarh", UT);
        stop("PGI", "Chandigarh", UT);
        stop("Khuda Lahora", "Chandigarh", UT);
        stop("Madhya Marg Sector 9", "Chandigarh", UT);
        stop("Housing Board Chowk", "Chandigarh", UT);
        stop("Tribune Chowk", "Chandigarh", UT);
        stop("Kharar Bus Stand", "Kharar (Mohali)", PB);
        stop("Mohali Phase 7", "Mohali", PB);
        stop("Mohali Phase 3B2", "Mohali", PB);
        stop("New Chandigarh (Ranai Majra)", "New Chandigarh", PB);
        stop("Zirakpur Bus Stand", "Zirakpur", PB);
        stop("Panchkula Sector 5 Bus Stand", "Panchkula", HR);
        stop("Mansa Devi Complex", "Panchkula", HR);
    }

    private void stop(String name, String city, String state) {
        stops.put(name, stopRepository.save(new Stop(name, city, state)));
    }

    // ---------------------------------------------------------------- routes

    /** Stop name, km from start, minutes from start. */
    private record S(String name, double km, int min) {
    }

    private List<Route> seedRoutes() {
        List<Route> all = new ArrayList<>();
        // The six busiest corridors identified by RITES (Comprehensive Mobility Plan for the Tricity)
        all.addAll(corridor("TC-104", "Kharar – Chandigarh Express", "Highway Eco-Express", 10,
                new S("Kharar Bus Stand", 0, 0), new S("Mohali Phase 7", 7, 13), new S("Mohali Phase 3B2", 10, 19),
                new S("Sector 43 ISBT", 14, 26), new S("Sector 17 ISBT", 19, 35)));
        all.addAll(corridor("TC-101", "ISBT-43 – New Chandigarh", "Non-Stop Express Corridor", 10,
                new S("Sector 43 ISBT", 0, 0), new S("PGI", 8, 18), new S("Khuda Lahora", 13, 28),
                new S("New Chandigarh (Ranai Majra)", 21, 42)));
        all.addAll(corridor("TC-206", "ISBT-43 – Mansa Devi Complex", "Scenic Pilgrim & Civic Fastline", 12,
                new S("Sector 43 ISBT", 0, 0), new S("Sector 17 ISBT", 5, 14), new S("Madhya Marg Sector 9", 8, 22),
                new S("Housing Board Chowk", 12, 30), new S("Mansa Devi Complex", 16, 40)));
        all.addAll(corridor("TC-312", "ISBT-43 – Panchkula Sector 5", "Cross-City AC Shuttle", 8,
                new S("Sector 43 ISBT", 0, 0), new S("Sector 17 ISBT", 5, 14), new S("Housing Board Chowk", 11, 28),
                new S("Panchkula Sector 5 Bus Stand", 16, 38)));
        all.addAll(corridor("TC-405", "PGI – Zirakpur", "Medical & Airport Link", 15,
                new S("PGI", 0, 0), new S("Sector 17 ISBT", 5, 14), new S("Tribune Chowk", 10, 24),
                new S("Zirakpur Bus Stand", 17, 38)));
        all.addAll(corridor("TC-215", "PGI – Panchkula", "Institutional Direct Corridor", 10,
                new S("PGI", 0, 0), new S("Madhya Marg Sector 9", 4, 10), new S("Housing Board Chowk", 8, 20),
                new S("Panchkula Sector 5 Bus Stand", 12, 30)));
        return all;
    }

    /** Creates a route and its return direction (code + "R"), linked to each other. */
    private List<Route> corridor(String code, String name, String tagline, int freq, S... path) {
        double total = path[path.length - 1].km();
        int totalMin = path[path.length - 1].min();

        Route forward = new Route(code, name, tagline, total, freq);
        for (S s : path) {
            forward.addStop(stops.get(s.name()), s.km(), s.min());
        }
        forward = routeRepository.save(forward);

        String[] parts = name.split(" – ");
        String reverseName = parts.length == 2 ? parts[1] + " – " + parts[0] : name + " (Return)";
        Route back = new Route(code + "R", reverseName, tagline, total, freq);
        for (int i = path.length - 1; i >= 0; i--) {
            back.addStop(stops.get(path[i].name()), total - path[i].km(), totalMin - path[i].min());
        }
        back.setReturnRoute(forward);
        back = routeRepository.save(back);
        forward.setReturnRoute(back);
        return List.of(forward, back);
    }

    // ---------------------------------------------------------------- buses

    private List<Bus> seedBuses() {
        List<Bus> buses = new ArrayList<>();
        String[] depots = {"CTU Depot 1, Industrial Area", "CTU Depot 2, Sector 25", "CTU Depot 4, Hallomajra"};
        for (int i = 0; i < 24; i++) {
            BusType type = i % 6 == 5 ? BusType.AC_CNG : BusType.AC_ELECTRIC;
            String reg = (i % 4 == 3 ? "PB65-AX-" : "CH01-GA-") + (2000 + i * 37 % 1000 + i);
            Bus bus = new Bus(reg, type, 36, depots[i % depots.length]);
            bus.setModel(type == BusType.AC_CNG ? "Ashok Leyland Circuit CNG" : (i % 2 == 0 ? "Tata Ultra EV 9/9" : "Olectra Greentech K9"));
            buses.add(busRepository.save(bus));
        }
        // reserve fleet that is not on the timetable
        String[][] reserve = {{"CH01-GA-5014", "MAINTENANCE"}, {"CH01-GA-5112", "CHARGING"},
                {"CH01-GA-5230", "STANDBY"}, {"PB65-AX-5318", "STANDBY"}};
        for (String[] r : reserve) {
            Bus bus = new Bus(r[0], BusType.AC_ELECTRIC, 36, depots[0]);
            bus.setModel("Tata Ultra EV 9/9");
            bus.setStatus(BusStatus.valueOf(r[1]));
            busRepository.save(bus);
        }
        return buses;
    }

    // ---------------------------------------------------------------- users

    private Map<String, AppUser> seedUsers() {
        Map<String, AppUser> m = new HashMap<>();
        AppUser harpreet = new AppUser("Harpreet Singh", "9876543210", "harpreet@tricity.in", Gender.MALE,
                "Kharar", passwordEncoder.encode("password123"), Role.PASSENGER);
        harpreet.setWalletBalance(new BigDecimal("250.00"));
        m.put("harpreet", userRepository.save(harpreet));

        AppUser simran = new AppUser("Simran Kaur", "9812345678", "simran@tricity.in", Gender.FEMALE,
                "Panchkula", passwordEncoder.encode("password123"), Role.PASSENGER);
        simran.setWalletBalance(new BigDecimal("180.00"));
        m.put("simran", userRepository.save(simran));

        m.put("admin", userRepository.save(new AppUser("Transit Admin", "9000000001", "admin@tricity.in",
                Gender.OTHER, "Chandigarh", passwordEncoder.encode("admin123"), Role.ADMIN)));
        m.put("conductor", userRepository.save(new AppUser("Ramesh Kumar", "9000000002", "conductor@tricity.in",
                Gender.MALE, "Mohali", passwordEncoder.encode("conductor123"), Role.CONDUCTOR)));
        // owner of the background bookings that make buses look busy
        m.put("crowd", userRepository.save(new AppUser("Counter Bookings", "9000000003", null,
                Gender.OTHER, "Chandigarh", passwordEncoder.encode(UUID.randomUUID().toString()), Role.PASSENGER)));
        return m;
    }

    // ---------------------------------------------------------------- trips

    private List<Trip> seedTrips(List<Route> routes, List<Bus> buses) {
        String[] drivers = {"Kuldeep Singh", "Gurpreet S.", "Rajinder Kumar", "Manjit Kaur", "Sukhwinder Singh",
                "Amandeep Sharma", "Balwinder Singh", "Harjit Gill"};
        List<Trip> trips = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int day = -HISTORY_DAYS; day <= 6; day++) {
            LocalDate date = today.plusDays(day);
            for (int r = 0; r < routes.size(); r++) {
                Route route = routes.get(r);
                for (int k = 0; k < Timetable.DEPARTURES.size(); k++) {
                    Bus bus = buses.get((r * 2 + k) % buses.size());
                    Trip t = new Trip(route, bus, date.atTime(Timetable.DEPARTURES.get(k)),
                            "Bay " + (1 + k % 4), drivers[(r + k) % drivers.length]);
                    trips.add(t);
                }
            }
        }
        // a couple of live-looking delays today
        trips.stream().filter(t -> t.getDepartureTime().toLocalDate().equals(today)
                        && t.getRoute().getCode().equals("TC-312")
                        && t.getDepartureTime().toLocalTime().equals(LocalTime.of(8, 20)))
                .forEach(t -> t.setDelayMinutes(2));
        return tripRepository.saveAll(trips);
    }

    // ---------------------------------------------------------------- pass

    private CommuterPass seedPass(AppUser owner, Route route) {
        CommuterPass p = new CommuterPass();
        p.setPassNo("TC-PASS-4982");
        p.setUser(owner);
        p.setPassType(PassType.CORRIDOR);
        p.setRoute(route);
        p.setSeatNo("4A");
        p.setMorningDeparture(LocalTime.of(8, 5));
        p.setEveningDeparture(LocalTime.of(18, 0));
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        p.setValidFrom(start);
        p.setValidTo(start.plusMonths(1).minusDays(1));
        p.setMonths(1);
        BigDecimal fare = FareCalculator.calculate(route.getDistanceKm(), LocalTime.of(8, 5)).total();
        p.setPrice(PassPricing.price(fare, 1, false));
        p.setFullPrice(PassPricing.fullPrice(fare, 1));
        p.setTripsUsed(24);
        p.setCreatedAt(start.atStartOfDay().minusDays(2));
        passRepository.save(p);

        // last month's pass, now expired
        CommuterPass old = new CommuterPass();
        old.setPassNo("TC-PASS-3871");
        old.setUser(owner);
        old.setRoute(route);
        old.setSeatNo("6B");
        old.setMorningDeparture(LocalTime.of(8, 5));
        old.setEveningDeparture(LocalTime.of(18, 0));
        old.setValidFrom(start.minusMonths(1));
        old.setValidTo(start.minusDays(1));
        old.setMonths(1);
        old.setPrice(p.getPrice());
        old.setFullPrice(p.getFullPrice());
        old.setStatus(PassStatus.EXPIRED);
        old.setTripsUsed(41);
        old.setCreatedAt(start.minusMonths(1).atStartOfDay().minusDays(2));
        passRepository.save(old);
        return p;
    }

    // ---------------------------------------------------------------- bookings

    private Trip findTrip(List<Trip> trips, String routeCode, LocalDate date, LocalTime time) {
        return trips.stream().filter(t -> t.getRoute().getCode().equals(routeCode)
                        && t.getDepartureTime().toLocalDate().equals(date)
                        && t.getDepartureTime().toLocalTime().equals(time))
                .findFirst().orElseThrow();
    }

    /** Bookings that appear in Harpreet's "My Bookings" (upcoming, completed, cancelled). */
    private void seedDemoBookings(Map<String, AppUser> users, List<Trip> trips) {
        AppUser h = users.get("harpreet");
        LocalDate today = LocalDate.now();

        // upcoming: tomorrow morning Kharar -> Sector 17
        demoBooking(h, findTrip(trips, "TC-104", today.plusDays(1), LocalTime.of(7, 45)), "8B", 1, 5,
                BookingStatus.CONFIRMED, PaymentMethod.UPI);
        // upcoming: tomorrow evening Sector 17 -> Mohali Phase 7 (return route)
        demoBooking(h, findTrip(trips, "TC-104R", today.plusDays(1), LocalTime.of(17, 45)), "9A", 1, 4,
                BookingStatus.CONFIRMED, PaymentMethod.WALLET);
        // completed: yesterday
        demoBooking(h, findTrip(trips, "TC-104", today.minusDays(1), LocalTime.of(7, 45)), "5C", 1, 5,
                BookingStatus.CONFIRMED, PaymentMethod.UPI);
        demoBooking(h, findTrip(trips, "TC-312", today.minusDays(1), LocalTime.of(17, 15)), "7D", 2, 4,
                BookingStatus.CONFIRMED, PaymentMethod.CARD);
        // cancelled
        Booking c = demoBooking(h, findTrip(trips, "TC-405", today.plusDays(2), LocalTime.of(13, 0)), "6C", 1, 4,
                BookingStatus.CANCELLED, PaymentMethod.UPI);
        c.setRefundAmount(RefundPolicy.refundAmount(c.getTotalFare(), 600));
        c.setCancelReason("Change of plans");
        c.setCancelledAt(LocalDateTime.now().minusHours(3));
    }

    private Booking demoBooking(AppUser user, Trip trip, String seat, int from, int to,
                                BookingStatus status, PaymentMethod method) {
        Route route = trip.getRoute();
        RouteStop a = route.stopAtSeq(from);
        RouteStop b = route.stopAtSeq(to);
        FareBreakdown fare = FareCalculator.calculate(b.getKmFromStart() - a.getKmFromStart(),
                trip.timeAt(a).toLocalTime());
        Booking bk = new Booking();
        bk.setPnr(uniquePnr());
        bk.setUser(user);
        bk.setTrip(trip);
        bk.setSeatNo(seat);
        bk.setFromSeq(from);
        bk.setToSeq(to);
        bk.setPassengerName(user.getFullName());
        bk.setPassengerAge(28);
        bk.setPassengerGender(user.getGender());
        bk.setPassengerMobile(user.getMobile());
        bk.setPassengerEmail(user.getEmail());
        bk.setBaseFare(fare.base());
        bk.setCgst(fare.cgst());
        bk.setSgst(fare.sgst());
        bk.setTotalFare(fare.total());
        bk.setPaymentMethod(method);
        bk.setStatus(status);
        bk.setWhatsappTicket(true);
        bk.setCreatedAt(trip.getDepartureTime().minusDays(1));
        return bookingRepository.save(bk);
    }

    /**
     * Random stop-wise bookings on today's and tomorrow's buses so seat maps look real.
     * One morning bus is filled completely to show the "Bus Full" state.
     */
    private void seedRandomBookings(AppUser crowd, List<Trip> trips, CommuterPass pass) {
        LocalDate today = LocalDate.now();
        List<Booking> batch = new ArrayList<>();
        Set<String> existing = new HashSet<>();
        for (Booking b : bookingRepository.findAll()) {
            existing.add(b.getTrip().getId() + ":" + b.getSeatNo());
        }
        for (Trip trip : trips) {
            LocalDate d = trip.getDepartureTime().toLocalDate();
            boolean history = d.isBefore(today);
            if (!history && !d.equals(today) && !d.equals(today.plusDays(1))) {
                continue;
            }
            Route route = trip.getRoute();
            int last = route.getLastStop().getSeq();
            LocalTime t = trip.getDepartureTime().toLocalTime();
            boolean peak = FareCalculator.isPeak(t);
            boolean fillCompletely = d.equals(today) && route.getCode().equals("TC-104") && t.equals(LocalTime.of(8, 35));
            int target = fillCompletely ? 36 : (peak ? 18 + random.nextInt(14) : 4 + random.nextInt(10));
            if (history) {
                // weekends are quieter than weekdays
                boolean weekend = d.getDayOfWeek().getValue() >= 6;
                target = (peak ? 20 + random.nextInt(14) : 3 + random.nextInt(7)) / (weekend ? 2 : 1);
            }

            List<String> labels = new ArrayList<>(SeatLayout.seatLabels(36));
            Collections.shuffle(labels, random);
            int made = 0;
            for (String seat : labels) {
                if (made >= target) {
                    break;
                }
                if (existing.contains(trip.getId() + ":" + seat) || (pass.coversTrip(trip) && seat.equals(pass.getSeatNo()))) {
                    continue;
                }
                int from;
                int to;
                if (fillCompletely) {
                    from = 1;
                    to = last;
                } else {
                    from = 1 + random.nextInt(last - 1);
                    to = from + 1 + random.nextInt(last - from);
                    // most commuters ride most of the corridor
                    if (random.nextInt(3) > 0) {
                        from = 1;
                        to = last;
                    }
                }
                Booking b = new Booking();
                b.setPnr(uniquePnr());
                b.setUser(crowd);
                b.setTrip(trip);
                b.setSeatNo(seat);
                b.setFromSeq(from);
                b.setToSeq(to);
                boolean woman = SeatLayout.isLadiesSeat(seat) || random.nextBoolean();
                b.setPassengerName(woman ? "Counter Passenger (F)" : "Counter Passenger");
                b.setPassengerAge(18 + random.nextInt(50));
                b.setPassengerGender(woman ? Gender.FEMALE : Gender.MALE);
                RouteStop a = route.stopAtSeq(from);
                RouteStop z = route.stopAtSeq(to);
                FareBreakdown fare = FareCalculator.calculate(z.getKmFromStart() - a.getKmFromStart(),
                        trip.timeAt(a).toLocalTime());
                b.setBaseFare(fare.base());
                b.setCgst(fare.cgst());
                b.setSgst(fare.sgst());
                b.setTotalFare(fare.total());
                b.setPaymentMethod(random.nextInt(4) == 0 ? PaymentMethod.WALLET : PaymentMethod.UPI);
                b.setStatus(BookingStatus.CONFIRMED);
                b.setCreatedAt(trip.getDepartureTime().minusHours(2 + random.nextInt(40)));
                if (history) {
                    b.setBoarded(true);
                    b.setBoardedAt(trip.timeAt(a));
                    // a few cancellations in the history for the reports
                    if (random.nextInt(25) == 0) {
                        b.setStatus(BookingStatus.CANCELLED);
                        b.setRefundAmount(RefundPolicy.refundAmount(fare.total(), 180));
                        b.setCancelReason("Change of plans");
                        b.setCancelledAt(b.getCreatedAt().plusHours(1));
                        b.setBoarded(false);
                    }
                }
                batch.add(b);
                made++;
            }
        }
        bookingRepository.saveAll(batch);
    }

    // ---------------------------------------------------------------- wallet history

    private void seedWallet(AppUser h, CommuterPass pass) {
        List<WalletTransaction> list = new ArrayList<>();
        WalletTransaction topup = new WalletTransaction(h, WalletTransaction.Kind.TOPUP, "Wallet auto-top-up via UPI",
                null, null, "GPay (HDFC •••• 8821)", new BigDecimal("500.00"), true);
        topup.setCreatedAt(LocalDateTime.now().minusDays(20).withHour(18).withMinute(40));
        list.add(topup);
        WalletTransaction p = new WalletTransaction(h, WalletTransaction.Kind.PASS,
                "Monthly commuter pass (1 month) seat " + pass.getSeatNo(), pass.getPassNo(), pass.getRoute().getCode(),
                "UPI", pass.getPrice().negate(), false);
        p.setCreatedAt(pass.getCreatedAt());
        list.add(p);
        for (Booking b : bookingRepository.findByUserOrderByCreatedAtDesc(h)) {
            WalletTransaction t = new WalletTransaction(h, WalletTransaction.Kind.BOOKING,
                    "Seat reservation (seat " + b.getSeatNo() + ") " + b.getFromStop().getStop().getName() + " → "
                            + b.getToStop().getStop().getName(), b.getPnr(), b.getTrip().getRoute().getCode(),
                    b.getPaymentMethod().getLabel(), b.getTotalFare().negate(), b.getPaymentMethod() == PaymentMethod.WALLET);
            t.setCreatedAt(b.getCreatedAt());
            list.add(t);
            if (b.getStatus() == BookingStatus.CANCELLED) {
                WalletTransaction r = new WalletTransaction(h, WalletTransaction.Kind.REFUND,
                        "Cancelled seat refund (90% tier)", b.getPnr(), b.getTrip().getRoute().getCode(),
                        "Credit to Wallet", b.getRefundAmount(), true);
                r.setCreatedAt(b.getCancelledAt());
                list.add(r);
            }
        }
        walletRepository.saveAll(list);
    }

    // ---------------------------------------------------------------- disruptions

    /**
     * A live disruption tomorrow morning on TC-104 (Punjab buses stopping at Mohali because of a
     * timetable dispute) — it affects Harpreet's 07:45 booking, so the passenger options can be demonstrated.
     * Plus one resolved disruption from yesterday for the history list.
     */
    private void seedDisruptions() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        com.tricityconnect.dto.DisruptionForm f = new com.tricityconnect.dto.DisruptionForm();
        f.setType(DisruptionType.SHORT_TERMINATED);
        f.setRouteIds(List.of(routeRepository.findByCode("TC-104").orElseThrow().getId()));
        f.setBorderImpact("Chandigarh–Mohali");
        f.setNewLastStopId(stops.get("Mohali Phase 3B2").getId());
        f.setDelayMinutes(0);
        f.setStartTime(tomorrow.atTime(7, 0));
        f.setEndTime(tomorrow.atTime(9, 0));
        f.setTitle("Punjab buses not entering Chandigarh: TC-104 will terminate at Mohali Phase 3B2");
        f.setReason("Due to the inter-state bus timetable dispute, Punjab-registered buses on TC-104 will not enter UT Chandigarh "
                + "tomorrow morning. Buses end at Mohali Phase 3B2.");
        disruptionService.publish(f);

        com.tricityconnect.dto.DisruptionForm old = new com.tricityconnect.dto.DisruptionForm();
        old.setType(DisruptionType.DELAYED);
        old.setRouteIds(List.of(routeRepository.findByCode("TC-312").orElseThrow().getId()));
        old.setBorderImpact("Chandigarh–Panchkula");
        old.setDelayMinutes(35);
        old.setStartTime(LocalDate.now().minusDays(1).atTime(17, 0));
        old.setEndTime(LocalDate.now().minusDays(1).atTime(19, 0));
        old.setTitle("Panchkula Sector 5 ⇄ Sector 43: VVIP movement on Madhya Marg");
        old.setReason("Temporary VVIP motorcade route block on Madhya Marg. Evening trips delayed by about 35 minutes.");
        Disruption d = disruptionService.publish(old);
        disruptionService.resolve(d.getId(), "Normal services restored at 7:15 PM. No refunds needed.");
    }

    // ---------------------------------------------------------------- alerts

    private void seedAlerts() {
        alertRepository.save(new Alert(AlertType.DETOUR,
                "Route TC-405 detour due to Tribune Flyover structural repair",
                "Morning express trips between 08:00 AM and 10:00 AM will bypass Hallomajra Chowk and divert via Industrial Area Phase-II. Expect about 8 minutes extra.",
                "TC-405", "Expected delay: +8 mins|Bypassed stop: Hallomajra Jn|Alt shuttle: TC-215",
                "Valid 8:00 AM – 10:00 AM today", 1));
        alertRepository.save(new Alert(AlertType.FESTIVAL,
                "Special festival express services: Chandigarh – Panchkula corridor",
                "15 additional electric express buses run on the shopping corridors between Sector 17 Plaza, Elante Mall and Panchkula main market during evening peaks (7:00 PM – 11:00 PM). Frequency every 6 minutes.",
                "TC-312", "Operating window 19:00 – 23:00|Every 6 minutes|Flat fare ₹20 AC",
                "Navratri & Diwali commute windows", 3));
        alertRepository.save(new Alert(AlertType.NEW_ROUTE,
                "New route TC-101 Express: Sector 43 ISBT to New Chandigarh",
                "Direct AC corridor connecting Sector 43 ISBT with New Chandigarh (Ranai Majra) via PGI. First trip 7:15 AM, last trip 9:00 PM.",
                "TC-101", "Sector 43 ISBT → New Chandigarh|42 mins",
                "Commercial runs commenced", 4));
        alertRepository.save(new Alert(AlertType.PASS_OFFER,
                "Monthly pass early-bird discount: save ₹100 on your next cycle",
                "Renew your commuter pass by the 25th of the month to get ₹100 TriCity Wallet credit.",
                null, "Regular, student & senior citizen passes",
                "Offer valid till 25th", 5));
        alertRepository.save(new Alert(AlertType.SYSTEM,
                "Scheduled UPI ticket server sync",
                "QR ticket purchases may see up to 45 seconds delay on Sunday 02:00 – 04:00 AM. Downloaded tickets and passes keep working offline.",
                null, "Offline QR validation supported on board",
                "Sunday 02:00 AM – 04:00 AM", 6));
    }
}
