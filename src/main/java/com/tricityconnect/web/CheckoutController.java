package com.tricityconnect.web;

import com.tricityconnect.core.Concession;
import com.tricityconnect.service.FareService;
import com.tricityconnect.dto.CheckoutForm;
import com.tricityconnect.model.*;
import com.tricityconnect.repository.TripRepository;
import com.tricityconnect.service.BookingException;
import com.tricityconnect.service.BookingService;
import com.tricityconnect.service.NotFoundException;
import com.tricityconnect.service.SeatService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Passenger details + payment, then the booking is created. */
@Controller
public class CheckoutController {

    private final TripRepository tripRepository;
    private final SeatService seatService;
    private final BookingService bookingService;
    private final CurrentUser currentUser;
    private final FareService fareService;

    public CheckoutController(TripRepository tripRepository, SeatService seatService,
                              BookingService bookingService, CurrentUser currentUser, FareService fareService) {
        this.fareService = fareService;
        this.tripRepository = tripRepository;
        this.seatService = seatService;
        this.bookingService = bookingService;
        this.currentUser = currentUser;
    }

    @GetMapping("/checkout")
    public String checkout(@RequestParam Long tripId, @RequestParam int from, @RequestParam int to,
                           @RequestParam(required = false) String seat, Model model, RedirectAttributes flash) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new NotFoundException("Bus not found"));
        String back = "redirect:/trips/" + tripId + "/seats?from=" + from + "&to=" + to;
        if (seat == null || seat.isBlank()) {
            flash.addFlashAttribute("error", "Please select a seat first.");
            return back;
        }
        if (from >= to || seatService.occupiedSeats(trip, from, to).contains(seat)) {
            flash.addFlashAttribute("error", "Seat " + seat + " is no longer available. Please choose another seat.");
            return back;
        }
        AppUser user = currentUser.require();
        CheckoutForm form = new CheckoutForm();
        form.setTripId(tripId);
        form.setSeatNo(seat);
        form.setFromSeq(from);
        form.setToSeq(to);
        form.setPassengerName(user.getFullName());
        form.setPassengerGender(user.getGender());
        form.setPassengerMobile(user.getMobile());
        form.setPassengerEmail(user.getEmail());
        form.setPassengerAge(25);
        if (user.getEmergencyMobile() != null) {
            form.setEmergencyContact(user.getEmergencyMobile());
            form.setWomenSafety(user.getGender() == Gender.FEMALE);
        }
        addPageData(model, trip, form, user);
        model.addAttribute("form", form);
        return "checkout";
    }

    @PostMapping("/checkout")
    public String pay(@Valid @ModelAttribute("form") CheckoutForm form, BindingResult result, Model model) {
        Trip trip = tripRepository.findById(form.getTripId()).orElseThrow(() -> new NotFoundException("Bus not found"));
        AppUser user = currentUser.require();
        if (form.isWomenSafety() && (form.getEmergencyContact() == null || form.getEmergencyContact().isBlank())) {
            result.rejectValue("emergencyContact", "required", "Enter a family member's number to share your trip");
        }
        if (result.hasErrors()) {
            addPageData(model, trip, form, user);
            return "checkout";
        }
        try {
            Booking booking = bookingService.book(user, form);
            return "redirect:/bookings/" + booking.getPnr() + "/confirmation";
        } catch (BookingException e) {
            model.addAttribute("error", e.getMessage());
            addPageData(model, trip, form, currentUser.require());
            return "checkout";
        }
    }

    private void addPageData(Model model, Trip trip, CheckoutForm form, AppUser user) {
        Route route = trip.getRoute();
        RouteStop fromStop = route.stopAtSeq(form.getFromSeq());
        RouteStop toStop = route.stopAtSeq(form.getToSeq());
        model.addAttribute("trip", trip);
        model.addAttribute("fromStop", fromStop);
        model.addAttribute("toStop", toStop);
        double km = toStop.getKmFromStart() - fromStop.getKmFromStart();
        var time = trip.timeAt(fromStop).toLocalTime();
        model.addAttribute("fare", fareService.quote(km, time, Concession.GENERAL));
        model.addAttribute("fareStudent", fareService.quote(km, time, Concession.STUDENT));
        model.addAttribute("fareSenior", fareService.quote(km, time, Concession.SENIOR));
        model.addAttribute("concessions", Concession.values());
        java.util.Map<String, java.util.Map<String, Object>> table = new java.util.HashMap<>();
        for (Concession c : Concession.values()) {
            var f = fareService.quote(km, time, c);
            table.put(c.name(), java.util.Map.of("base", f.base(), "cgst", f.cgst(), "sgst", f.sgst(), "total", f.total()));
        }
        model.addAttribute("fareTable", table);
        model.addAttribute("genders", Gender.values());
        model.addAttribute("wallet", user.getWalletBalance());
    }
}
