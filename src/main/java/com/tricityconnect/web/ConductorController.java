package com.tricityconnect.web;

import com.tricityconnect.model.Trip;
import com.tricityconnect.repository.TripRepository;
import com.tricityconnect.service.BookingException;
import com.tricityconnect.service.ConductorService;
import com.tricityconnect.service.NotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

/** Conductor's mobile ETM screen (open on a phone). */
@Controller
@RequestMapping("/conductor")
public class ConductorController {

    private final ConductorService conductorService;
    private final TripRepository tripRepository;

    public ConductorController(ConductorService conductorService, TripRepository tripRepository) {
        this.conductorService = conductorService;
        this.tripRepository = tripRepository;
    }

    @GetMapping
    public String etm(@RequestParam(required = false) Long tripId, @RequestParam(required = false) Integer seq, Model model) {
        if (tripId == null) {
            model.addAttribute("trips", conductorService.todaysTrips());
            return "conductor/select";
        }
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new NotFoundException("Trip not found"));
        int last = trip.getRoute().getLastStop().getSeq();
        int s = seq == null ? conductorService.currentSeq(trip, LocalDateTime.now()) : Math.max(1, Math.min(seq, last - 1));
        var floor = conductorService.floor(trip, s);
        var rows = new java.util.TreeMap<Integer, java.util.List<com.tricityconnect.dto.SeatOccupant>>();
        floor.forEach(o -> rows.computeIfAbsent(com.tricityconnect.core.SeatLayout.rowOf(o.label()), r -> new java.util.ArrayList<>()).add(o));
        model.addAttribute("trip", trip);
        model.addAttribute("seq", s);
        model.addAttribute("current", trip.getRoute().stopAtSeq(s));
        model.addAttribute("next", trip.getRoute().stopAtSeq(s + 1));
        model.addAttribute("rows", rows);
        model.addAttribute("onBoard", floor.stream().filter(o -> !o.empty()).count());
        model.addAttribute("validated", floor.stream().filter(o -> o.booking() != null && o.booking().isBoarded()).count());
        model.addAttribute("freeingNext", floor.stream().filter(com.tricityconnect.dto.SeatOccupant::freeingNext).count());
        model.addAttribute("boarding", conductorService.boardingAt(trip, s));
        model.addAttribute("last", last);
        return "conductor/etm";
    }

    @PostMapping("/validate")
    public String validate(@RequestParam Long tripId, @RequestParam int seq, @RequestParam String code,
                           RedirectAttributes flash) {
        try {
            flash.addFlashAttribute("success", conductorService.validate(tripId, code));
        } catch (BookingException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/conductor?tripId=" + tripId + "&seq=" + seq;
    }
}
