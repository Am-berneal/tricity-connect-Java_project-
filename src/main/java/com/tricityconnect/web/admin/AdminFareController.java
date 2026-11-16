package com.tricityconnect.web.admin;

import com.tricityconnect.core.Concession;
import com.tricityconnect.core.DemandForecast;
import com.tricityconnect.core.FareCalculator;
import com.tricityconnect.model.FareSettings;
import com.tricityconnect.model.Route;
import com.tricityconnect.repository.RouteRepository;
import com.tricityconnect.service.BookingException;
import com.tricityconnect.service.FareService;
import com.tricityconnect.web.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/fares")
public class AdminFareController {

    public record CalendarDay(LocalDate date, int demand, String level, String event, boolean inMonth) {
    }

    private final FareService fareService;
    private final RouteRepository routeRepository;
    private final CurrentUser currentUser;

    public AdminFareController(FareService fareService, RouteRepository routeRepository, CurrentUser currentUser) {
        this.fareService = fareService;
        this.routeRepository = routeRepository;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String fares(@RequestParam(required = false) String month,
                        @RequestParam(required = false) Long routeId,
                        @RequestParam(defaultValue = "1") int from,
                        @RequestParam(defaultValue = "0") int to,
                        @RequestParam(defaultValue = "08:30") String time,
                        @RequestParam(defaultValue = "GENERAL") Concession concession,
                        Model model) {
        YearMonth ym = month == null ? YearMonth.now() : YearMonth.parse(month);
        // calendar grid starting on Monday
        List<CalendarDay> days = new ArrayList<>();
        LocalDate first = ym.atDay(1);
        LocalDate start = first.minusDays(first.getDayOfWeek().getValue() - 1L);
        for (int i = 0; i < 42; i++) {
            LocalDate d = start.plusDays(i);
            if (i >= 35 && d.getMonth() != ym.getMonth()) {
                break;
            }
            int idx = DemandForecast.index(d);
            days.add(new CalendarDay(d, idx, DemandForecast.level(idx), DemandForecast.eventOn(d), d.getMonth() == ym.getMonth()));
        }
        model.addAttribute("days", days);
        model.addAttribute("month", ym);
        model.addAttribute("settings", fareService.settings());

        // fare preview calculator
        List<Route> routes = routeRepository.findAllByOrderByCodeAsc();
        Route route = routeId == null ? routeRepository.findByCode("TC-104").orElse(routes.get(0))
                : routeRepository.findById(routeId).orElse(routes.get(0));
        int last = route.getLastStop().getSeq();
        int t = to <= 0 || to > last ? last : to;
        int f = from < 1 || from >= t ? 1 : from;
        LocalTime lt = LocalTime.parse(time);
        double km = route.stopAtSeq(t).getKmFromStart() - route.stopAtSeq(f).getKmFromStart();
        var rules = fareService.rules();
        model.addAttribute("routes", routes);
        model.addAttribute("pRoute", route);
        model.addAttribute("pFrom", f);
        model.addAttribute("pTo", t);
        model.addAttribute("pTime", lt);
        model.addAttribute("pConcession", concession);
        model.addAttribute("concessions", Concession.values());
        model.addAttribute("pKm", km);
        model.addAttribute("pPeak", FareCalculator.isPeak(lt));
        model.addAttribute("pFare", FareCalculator.calculate(km, lt, rules, rules.discountFor(concession)));
        model.addAttribute("pRaw", rules.ratePerKm().multiply(java.math.BigDecimal.valueOf(km)));
        model.addAttribute("pDiscount", rules.discountFor(concession));
        return "admin/fares";
    }

    @PostMapping
    public String save(@ModelAttribute FareSettings form, RedirectAttributes flash) {
        try {
            fareService.save(form, currentUser.require().getFullName());
            flash.addFlashAttribute("success", "Fare rules saved. New bookings and passes use them immediately; "
                    + "existing tickets keep the fare they were sold at.");
        } catch (BookingException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/fares";
    }
}
