package com.tricityconnect.web.admin;

import com.tricityconnect.dto.TripLoad;
import com.tricityconnect.model.*;
import com.tricityconnect.repository.*;
import com.tricityconnect.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/admin/trips")
public class AdminTripController {

    private final StatsService statsService;
    private final TripAdminService tripAdminService;
    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final TripRepository tripRepository;
    private final CommuterPassRepository passRepository;
    private final ConductorService conductorService;

    public AdminTripController(StatsService statsService, TripAdminService tripAdminService,
                               RouteRepository routeRepository, BusRepository busRepository,
                               TripRepository tripRepository, CommuterPassRepository passRepository,
                               ConductorService conductorService) {
        this.statsService = statsService;
        this.tripAdminService = tripAdminService;
        this.routeRepository = routeRepository;
        this.busRepository = busRepository;
        this.tripRepository = tripRepository;
        this.passRepository = passRepository;
        this.conductorService = conductorService;
    }

    @GetMapping
    public String trips(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(required = false) Long routeId,
                        @RequestParam(required = false) TripStatus status,
                        @RequestParam(defaultValue = "") String q, Model model) {
        LocalDate day = date == null ? LocalDate.now() : date;
        List<Trip> trips = statsService.tripsBetween(day.atStartOfDay(), day.plusDays(1).atStartOfDay()).stream()
                .filter(t -> routeId == null || t.getRoute().getId().equals(routeId))
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> q.isBlank() || (t.getRoute().getCode() + " " + t.getBus().getRegNo() + " " + t.getDriverName()
                        + " TR-" + t.getId()).toLowerCase().contains(q.toLowerCase()))
                .toList();
        List<TripLoad> loads = statsService.loads(trips);
        model.addAttribute("loads", loads);
        model.addAttribute("date", day);
        model.addAttribute("routeId", routeId);
        model.addAttribute("status", status);
        model.addAttribute("q", q);
        model.addAttribute("routes", routeRepository.findAllByOrderByCodeAsc());
        model.addAttribute("buses", busRepository.findAllByOrderByRegNoAsc());
        model.addAttribute("statuses", TripStatus.values());
        model.addAttribute("totalSeatsSold", loads.stream().mapToInt(TripLoad::bookings).sum());
        model.addAttribute("totalCapacity", loads.stream().mapToInt(TripLoad::capacity).sum());
        model.addAttribute("passHolders", loads.stream().mapToInt(TripLoad::passHolders).sum());
        model.addAttribute("disrupted", loads.stream().filter(l -> l.trip().getStatus() == TripStatus.DISRUPTED
                || l.trip().getStatus() == TripStatus.CANCELLED).count());
        model.addAttribute("avgLoad", (int) Math.round(loads.stream().mapToInt(TripLoad::percent).average().orElse(0)));
        model.addAttribute("now", LocalDateTime.now());
        return "admin/trips";
    }

    @GetMapping("/{id}")
    public String manifest(@PathVariable Long id, Model model) {
        Trip trip = tripRepository.findById(id).orElseThrow(() -> new NotFoundException("Trip not found"));
        model.addAttribute("trip", trip);
        model.addAttribute("load", statsService.loads(List.of(trip)).get(0));
        model.addAttribute("bookings", tripAdminService.manifest(trip));
        model.addAttribute("passes", passRepository.findValidForRoute(trip.getRoute().getId(),
                trip.getDepartureTime().toLocalDate(), PassStatus.ACTIVE).stream().filter(p -> p.coversTrip(trip)).toList());
        model.addAttribute("buses", busRepository.findAllByOrderByRegNoAsc());
        model.addAttribute("currentSeq", conductorService.currentSeq(trip, LocalDateTime.now()));
        return "admin/manifest";
    }

    @PostMapping
    public String create(@RequestParam Long routeId, @RequestParam Long busId,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime departure,
                         RedirectAttributes flash) {
        try {
            Trip t = tripAdminService.create(routeId, busId, departure);
            flash.addFlashAttribute("success", "Trip TR-" + t.getId() + " created.");
            return "redirect:/admin/trips?date=" + departure.toLocalDate();
        } catch (BookingException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/trips";
        }
    }

    @PostMapping("/bulk")
    public String bulk(@RequestParam Long routeId, @RequestParam Long busId,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       RedirectAttributes flash) {
        try {
            int n = tripAdminService.bulkSchedule(routeId, busId, time, from, to);
            flash.addFlashAttribute("success", n + " weekday trips scheduled.");
        } catch (BookingException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/trips?date=" + from;
    }

    @PostMapping("/{id}/bus")
    public String changeBus(@PathVariable Long id, @RequestParam Long busId, RedirectAttributes flash) {
        try {
            Trip t = tripAdminService.changeBus(id, busId);
            flash.addFlashAttribute("success", "Trip TR-" + id + " now runs with " + t.getBus().getRegNo() + ".");
        } catch (BookingException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/trips/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, @RequestParam(required = false) String reason,
                         @RequestParam(required = false) String back, RedirectAttributes flash) {
        try {
            int n = tripAdminService.cancel(id, reason);
            flash.addFlashAttribute("success", "Trip TR-" + id + " cancelled. " + n + " passengers refunded 100% to their wallets.");
        } catch (BookingException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + (back != null && back.startsWith("/admin/") ? back : "/admin/trips");
    }
}
