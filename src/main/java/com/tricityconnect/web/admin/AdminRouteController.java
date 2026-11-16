package com.tricityconnect.web.admin;

import com.tricityconnect.model.Route;
import com.tricityconnect.repository.BookingRepository;
import com.tricityconnect.repository.RouteRepository;
import com.tricityconnect.repository.StopRepository;
import com.tricityconnect.service.BookingException;
import com.tricityconnect.service.FareService;
import com.tricityconnect.service.NotFoundException;
import com.tricityconnect.service.RouteAdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalTime;
import java.util.*;

@Controller
@RequestMapping("/admin/routes")
public class AdminRouteController {

    public static final List<String> CITIES = List.of("Chandigarh", "Mohali", "Kharar (Mohali)", "Zirakpur",
            "New Chandigarh", "Panchkula");
    public static final List<String> STATES = List.of("UT Chandigarh", "Punjab", "Haryana");

    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final BookingRepository bookingRepository;
    private final RouteAdminService routeAdminService;
    private final FareService fareService;

    public AdminRouteController(RouteRepository routeRepository, StopRepository stopRepository,
                                BookingRepository bookingRepository, RouteAdminService routeAdminService,
                                FareService fareService) {
        this.routeRepository = routeRepository;
        this.stopRepository = stopRepository;
        this.bookingRepository = bookingRepository;
        this.routeAdminService = routeAdminService;
        this.fareService = fareService;
    }

    @GetMapping
    public String routes(@RequestParam(required = false) Long id, @RequestParam(defaultValue = "") String q, Model model) {
        List<Route> all = routeRepository.findAllByOrderByCodeAsc().stream()
                .filter(r -> q.isBlank() || (r.getCode() + " " + r.getName()).toLowerCase().contains(q.toLowerCase()))
                .toList();
        Route selected = id == null ? (all.isEmpty() ? null : all.get(0))
                : routeRepository.findById(id).orElseThrow(() -> new NotFoundException("Route not found"));
        model.addAttribute("routes", all);
        model.addAttribute("q", q);
        model.addAttribute("route", selected);
        model.addAttribute("stops", stopRepository.findAllByOrderByCityAscNameAsc());
        model.addAttribute("cities", CITIES);
        model.addAttribute("states", STATES);
        if (selected != null) {
            var rules = fareService.rules();
            Map<Integer, java.math.BigDecimal> fares = new HashMap<>();
            Map<Integer, Long> usage = new HashMap<>();
            selected.getStops().forEach(s -> {
                if (s.getSeq() > 1) {
                    fares.put(s.getSeq(), FareService.quote(s.getKmFromStart(), LocalTime.of(8, 0), rules).total());
                }
                usage.put(s.getSeq(), bookingRepository.countUsingStop(selected.getId(), s.getSeq()));
            });
            model.addAttribute("fares", fares);
            model.addAttribute("usage", usage);
        }
        return "admin/routes";
    }

    @PostMapping("/{id}/stops")
    public String addStop(@PathVariable Long id, @RequestParam String name, @RequestParam String city,
                          @RequestParam String state, @RequestParam int afterSeq, @RequestParam double km,
                          @RequestParam int minutes, RedirectAttributes flash) {
        try {
            var rs = routeAdminService.insertStop(id, name, city, state, afterSeq, km, minutes);
            flash.addFlashAttribute("success", "Stop " + rs.getStop().getName() + " added as stop " + rs.getSeq()
                    + ". Existing bookings were renumbered automatically.");
        } catch (BookingException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/routes?id=" + id;
    }

    @PostMapping("/{id}/stops/{seq}/delete")
    public String removeStop(@PathVariable Long id, @PathVariable int seq, RedirectAttributes flash) {
        try {
            routeAdminService.removeStop(id, seq);
            flash.addFlashAttribute("success", "Stop removed and the route renumbered.");
        } catch (BookingException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/routes?id=" + id;
    }

    @PostMapping("/new")
    public String createCorridor(@RequestParam String code, @RequestParam(required = false) String name,
                                 @RequestParam(required = false) String tagline, @RequestParam(defaultValue = "10") int frequency,
                                 @RequestParam Long firstStopId, @RequestParam Long lastStopId,
                                 @RequestParam double km, @RequestParam int minutes, RedirectAttributes flash) {
        try {
            Route r = routeAdminService.createCorridor(code, name, tagline, frequency, firstStopId, lastStopId, km, minutes);
            flash.addFlashAttribute("success", "Corridor " + r.getCode() + " and its return route created. "
                    + "Add middle stops below, then schedule trips on the Trips page.");
            return "redirect:/admin/routes?id=" + r.getId();
        } catch (BookingException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/routes";
        }
    }
}
