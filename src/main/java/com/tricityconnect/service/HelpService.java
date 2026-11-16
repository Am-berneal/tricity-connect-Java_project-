package com.tricityconnect.service;

import com.tricityconnect.core.FareRules;
import com.tricityconnect.dto.TripOption;
import com.tricityconnect.model.*;
import com.tricityconnect.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Help Centre: FAQs, the rule-based TriCity Transit Assistant (answers from the live timetable and
 * fare rules, no external AI service), and grievances.
 */
@Service
public class HelpService {

    public record Faq(String category, String question, String answer) {
    }

    public static final List<String> CATEGORIES = List.of("Booking & Seats", "Passes", "Refunds & Delays",
            "Women's Safety", "Payments & Wallet", "Stops & Bays");

    private final StopRepository stopRepository;
    private final RouteStopRepository routeStopRepository;
    private final SearchService searchService;
    private final FareService fareService;
    private final GrievanceRepository grievanceRepository;

    public HelpService(StopRepository stopRepository, RouteStopRepository routeStopRepository,
                       SearchService searchService, FareService fareService, GrievanceRepository grievanceRepository) {
        this.stopRepository = stopRepository;
        this.routeStopRepository = routeStopRepository;
        this.searchService = searchService;
        this.fareService = fareService;
        this.grievanceRepository = grievanceRepository;
    }

    public List<Faq> faqs() {
        FareRules r = fareService.rules();
        return List.of(
                new Faq("Stops & Bays", "Where does the express bus stop in Mohali Phase 7 and Sector 17?",
                        "In Mohali Phase 7, TC-104 buses halt at Bay 3 near the main market. In Chandigarh, express buses use "
                                + "the Sector 17 ISBT main concourse. Your ticket shows the exact bay, and live tracking shows the bus position."),
                new Faq("Booking & Seats", "What is stop-wise seat booking?",
                        "Your seat is reserved only between your boarding and drop stops. After you get off, the same seat can be "
                                + "booked by someone boarding later. This lets the same buses carry more people."),
                new Faq("Refunds & Delays", "How do instant disruption refunds work if a bus terminates early?",
                        "If a published disruption affects your trip (for example buses not entering Chandigarh), the Alerts page "
                                + "offers a free connector bus, rebooking on a later bus, or a 100% refund to your TriCity Wallet."),
                new Faq("Refunds & Delays", "What refund do I get if I cancel?",
                        "2 hours or more before departure: 90%. 30 minutes to 2 hours: 50%. Less than 30 minutes: no refund. "
                                + "Refunds go to your TriCity Wallet immediately."),
                new Faq("Women's Safety", "Who can book the pink ladies seats?",
                        "Rows 1 and 2 can only be booked by women passengers. The conductor's screen shows who booked each seat, "
                                + "so the seats really reach women. You can also share your live trip with family and use SOS (112)."),
                new Faq("Passes", "Can I pause my monthly commuter pass?",
                        "Your pass is valid on weekdays only, so weekends don't count. For leave, contact the ISBT desk; "
                                + "a self-service pause is planned."),
                new Faq("Passes", "What happens if I miss my reserved morning bus?",
                        "Your pass seat is held only on your fixed bus. You can still book a normal ticket on any other bus."),
                new Faq("Booking & Seats", "Do students and senior citizens get a discount?",
                        "Yes: students " + r.studentDiscountPct() + "% off and senior citizens " + r.seniorDiscountPct()
                                + "% off. Choose the category at checkout and carry a valid ID."),
                new Faq("Payments & Wallet", "How do I add money to the TriCity Wallet?",
                        "Open Profile → TriCity Wallet → Add Money. Refunds are also credited to the wallet instantly."),
                new Faq("Booking & Seats", "How is the fare calculated?",
                        "₹" + r.ratePerKm() + " per km for your part of the route, ×" + r.peakMultiplier()
                                + " in peak hours (7–10 AM, 5–8 PM), rounded to the nearest ₹5 and including " + r.gstPercent() + "% GST."));
    }

    /** Answers a passenger question in English (simple keyword rules + live timetable). */
    @Transactional(readOnly = true)
    public String answer(String question) {
        String q = question == null ? "" : question.toLowerCase(Locale.ROOT).trim();
        if (q.isEmpty()) {
            return "Please type your question, for example \"next bus from Kharar to Sector 17\".";
        }
        List<Stop> mentioned = new ArrayList<>();
        for (Stop s : stopRepository.findAll()) {
            String name = s.getName().toLowerCase(Locale.ROOT);
            String shortName = name.replace(" bus stand", "").replace(" isbt", "").replace("(ranai majra)", "").trim();
            int at = q.indexOf(shortName);
            if (at >= 0) {
                mentioned.add(s);
            }
        }
        mentioned.sort(Comparator.comparingInt(s -> q.indexOf(s.getName().toLowerCase(Locale.ROOT)
                .replace(" bus stand", "").replace(" isbt", "").replace("(ranai majra)", "").trim())));

        if ((q.contains("next") || q.contains("bus") || q.contains("from")) && mentioned.size() >= 2) {
            Stop from = mentioned.get(0);
            Stop to = mentioned.get(1);
            List<TripOption> options = searchService.search(from.getId(), to.getId(), LocalDate.now(), "all",
                    null, false, null, "earliest");
            if (options.isEmpty()) {
                options = searchService.search(from.getId(), to.getId(), LocalDate.now().plusDays(1), "all",
                        null, false, null, "earliest");
            }
            if (options.isEmpty()) {
                return "No direct express bus runs from " + from.getName() + " to " + to.getName()
                        + ". Try a nearby stop, or change at Sector 17 ISBT / Sector 43 ISBT.";
            }
            TripOption o = options.get(0);
            DateTimeFormatter f = DateTimeFormatter.ofPattern("EEE hh:mm a");
            return "Next bus: " + o.trip().getRoute().getCode() + " from " + from.getName() + " at "
                    + f.format(o.departAt()) + ", reaching " + to.getName() + " at " + f.format(o.arriveAt())
                    + ". " + o.seatsLeft() + " seats left, fare ₹" + o.fare().total().intValue() + ".";
        }
        if (mentioned.size() == 1 && (q.contains("where") || q.contains("stop") || q.contains("bay") || q.contains("route"))) {
            Stop s = mentioned.get(0);
            List<String> routes = routeStopRepository.findByStopId(s.getId()).stream()
                    .map(rs -> rs.getRoute().getCode()).filter(c -> !c.endsWith("R")).distinct().sorted().toList();
            return s.getName() + " (" + s.getCity() + ", " + s.getState() + ") is served by "
                    + (routes.isEmpty() ? "return routes only" : String.join(", ", routes))
                    + ". Express buses use the marked TriCity Connect bay; your ticket shows the bay number.";
        }
        if (q.contains("refund") || q.contains("cancel")) {
            return faqs().get(3).answer();
        }
        if (q.contains("ladies") || q.contains("women") || q.contains("pink") || q.contains("safety") || q.contains("sos")) {
            return faqs().get(4).answer();
        }
        if (q.contains("pass")) {
            return "A commuter pass fixes your seat on one morning and one evening bus every weekday. "
                    + "1 month saves " + fareService.rules().pass1MonthPct() + "%, 3 months "
                    + fareService.rules().pass3MonthPct() + "%, 6 months " + fareService.rules().pass6MonthPct()
                    + "%. Buy it under My Passes → Get New Pass.";
        }
        if (q.contains("fare") || q.contains("price") || q.contains("cost") || q.contains("ticket")) {
            return faqs().get(9).answer();
        }
        if (q.contains("student") || q.contains("senior") || q.contains("discount")) {
            return faqs().get(7).answer();
        }
        if (q.contains("wallet") || q.contains("money") || q.contains("upi") || q.contains("pay")) {
            return faqs().get(8).answer();
        }
        if (q.contains("disrupt") || q.contains("delay") || q.contains("late") || q.contains("strike")) {
            return faqs().get(2).answer();
        }
        if (q.contains("luggage") || q.contains("bag")) {
            return "Cabin luggage up to 15 kg is allowed. Large items (cycles, furniture) are not allowed on express buses.";
        }
        return "I can help with next buses (\"next bus from Kharar to Sector 17\"), stops, fares, refunds, passes, "
                + "ladies seats and the wallet. For anything else, file a grievance below or call 1800-TRICITY.";
    }

    @Transactional
    public Grievance file(AppUser user, String category, String pnr, String message) {
        if (message == null || message.trim().length() < 10) {
            throw new BookingException("Please describe the problem in at least 10 characters.");
        }
        Grievance g = new Grievance();
        String ticket;
        SecureRandom random = new SecureRandom();
        do {
            ticket = "GRV-" + LocalDate.now().getYear() + "-" + (10000 + random.nextInt(90000));
        } while (grievanceRepository.existsByTicketNo(ticket));
        g.setTicketNo(ticket);
        g.setUser(user);
        g.setCategory(category == null || category.isBlank() ? "Other" : category);
        g.setPnr(pnr == null || pnr.isBlank() ? null : pnr.trim().toUpperCase());
        g.setMessage(message.trim());
        return grievanceRepository.save(g);
    }
}
