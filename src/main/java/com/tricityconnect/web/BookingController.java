package com.tricityconnect.web;

import com.tricityconnect.dto.RefundPreview;
import com.tricityconnect.model.AppUser;
import com.tricityconnect.model.Booking;
import com.tricityconnect.model.BookingStatus;
import com.tricityconnect.service.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.*;

/** My Bookings, confirmation / e-ticket, PDF download, cancellation and live tracking. */
@Controller
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final TrackingService trackingService;
    private final TicketPdfService ticketPdfService;
    private final QrService qrService;
    private final CurrentUser currentUser;

    public BookingController(BookingService bookingService, TrackingService trackingService,
                             TicketPdfService ticketPdfService, QrService qrService, CurrentUser currentUser) {
        this.bookingService = bookingService;
        this.trackingService = trackingService;
        this.ticketPdfService = ticketPdfService;
        this.qrService = qrService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String myBookings(@RequestParam(defaultValue = "upcoming") String tab, Model model) {
        AppUser user = currentUser.require();
        List<Booking> all = bookingService.bookingsOf(user);
        List<Booking> upcoming = all.stream().filter(Booking::isUpcoming)
                .sorted(Comparator.comparing(Booking::getBoardingTime)).toList();
        List<Booking> completed = all.stream().filter(Booking::isCompleted)
                .sorted(Comparator.comparing(Booking::getBoardingTime).reversed()).toList();
        List<Booking> cancelled = all.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).toList();

        Map<String, RefundPreview> refunds = new HashMap<>();
        upcoming.forEach(b -> refunds.put(b.getPnr(), bookingService.refundPreview(b)));

        Booking featured = upcoming.isEmpty() ? null : upcoming.get(0);
        model.addAttribute("tab", tab);
        model.addAttribute("upcoming", upcoming);
        model.addAttribute("completed", completed);
        model.addAttribute("cancelled", cancelled);
        model.addAttribute("refunds", refunds);
        model.addAttribute("featured", featured);
        if (featured != null) {
            model.addAttribute("featuredQr", qrService.dataUri(TicketPdfService.qrText(featured)));
            model.addAttribute("featuredTrack", trackingService.track(featured, LocalDateTime.now()));
        }
        long tripsDone = completed.size();
        model.addAttribute("carbonSaved", String.format("%.1f", completed.stream().mapToDouble(Booking::getDistanceKm).sum() * 0.12));
        model.addAttribute("tripsDone", tripsDone);
        return "bookings";
    }

    @GetMapping("/{pnr}/confirmation")
    public String confirmation(@PathVariable String pnr, Model model) {
        Booking b = bookingService.findForUser(currentUser.require(), pnr);
        model.addAttribute("b", b);
        model.addAttribute("qr", qrService.dataUri(TicketPdfService.qrText(b)));
        model.addAttribute("track", trackingService.track(b, LocalDateTime.now()));
        return "confirmation";
    }

    @GetMapping("/{pnr}/ticket.pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String pnr) {
        Booking b = bookingService.findForUser(currentUser.require(), pnr);
        byte[] pdf = ticketPdfService.ticket(b);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"TriCity-Ticket-" + pnr + ".pdf\"")
                .body(pdf);
    }

    @PostMapping("/{pnr}/cancel")
    public String cancel(@PathVariable String pnr, @RequestParam(defaultValue = "Other") String reason,
                         RedirectAttributes flash) {
        try {
            Booking b = bookingService.cancel(currentUser.require(), pnr, reason);
            flash.addFlashAttribute("success", "Booking " + pnr + " cancelled. ₹" + b.getRefundAmount()
                    + " has been added to your TriCity Wallet.");
            return "redirect:/bookings?tab=cancelled";
        } catch (BookingException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/bookings";
        }
    }

    @GetMapping("/{pnr}/track")
    public String track(@PathVariable String pnr, Model model) {
        Booking b = bookingService.findForUser(currentUser.require(), pnr);
        model.addAttribute("b", b);
        model.addAttribute("track", trackingService.track(b, LocalDateTime.now()));
        return "tracking";
    }
}
