package com.tricityconnect.web;

import com.tricityconnect.model.Alert;
import com.tricityconnect.model.AlertSubscription;
import com.tricityconnect.model.AlertType;
import com.tricityconnect.model.AppUser;
import com.tricityconnect.repository.AlertRepository;
import com.tricityconnect.repository.AlertSubscriptionRepository;
import com.tricityconnect.repository.RouteRepository;
import com.tricityconnect.service.NotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/alerts")
public class AlertController {

    private final AlertRepository alertRepository;
    private final AlertSubscriptionRepository subscriptionRepository;
    private final RouteRepository routeRepository;
    private final CurrentUser currentUser;
    private final com.tricityconnect.service.DisruptionService disruptionService;

    public AlertController(AlertRepository alertRepository, AlertSubscriptionRepository subscriptionRepository,
                           RouteRepository routeRepository, CurrentUser currentUser,
                           com.tricityconnect.service.DisruptionService disruptionService) {
        this.disruptionService = disruptionService;
        this.alertRepository = alertRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.routeRepository = routeRepository;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String alerts(@RequestParam(defaultValue = "all") String filter, Model model) {
        List<Alert> all = alertRepository.findByActiveTrueOrderByPriorityAscCreatedAtDesc();
        Set<AlertType> delays = Set.of(AlertType.DETOUR, AlertType.SUSPENSION, AlertType.DELAY);
        Set<AlertType> service = Set.of(AlertType.FESTIVAL, AlertType.NEW_ROUTE);
        Set<AlertType> system = Set.of(AlertType.PASS_OFFER, AlertType.SYSTEM);
        List<Alert> shown = switch (filter) {
            case "delays" -> all.stream().filter(a -> delays.contains(a.getType())).toList();
            case "service" -> all.stream().filter(a -> service.contains(a.getType())).toList();
            case "system" -> all.stream().filter(a -> system.contains(a.getType())).toList();
            default -> all;
        };
        model.addAttribute("filter", filter);
        model.addAttribute("alerts", shown);
        model.addAttribute("countAll", all.size());
        model.addAttribute("countDelays", all.stream().filter(a -> delays.contains(a.getType())).count());
        model.addAttribute("countService", all.stream().filter(a -> service.contains(a.getType())).count());
        model.addAttribute("countSystem", all.stream().filter(a -> system.contains(a.getType())).count());
        model.addAttribute("routes", routeRepository.findAllByOrderByCodeAsc());
        currentUser.get().ifPresent(u -> {
            model.addAttribute("subscriptions", subscriptionRepository.findByUser(u));
            var affected = disruptionService.affectedBookings(u);
            model.addAttribute("affected", affected);
            java.util.Map<String, java.util.List<com.tricityconnect.dto.RebookOption>> options = new java.util.HashMap<>();
            affected.forEach(b -> options.put(b.getPnr(), disruptionService.rebookOptions(b, 2)));
            model.addAttribute("rebookOptions", options);
        });
        model.addAttribute("resolvedDisruptions", disruptionService.resolved().stream().limit(5).toList());
        return "alerts";
    }

    /** Passenger's answer to a disruption: connector, rebook, refund or keep. */
    @PostMapping("/disruption/{pnr}/{choice}")
    public String disruptionChoice(@PathVariable String pnr, @PathVariable String choice,
                                   @RequestParam(required = false) Long tripId, RedirectAttributes flash) {
        AppUser user = currentUser.require();
        try {
            switch (choice) {
                case "connector" -> {
                    disruptionService.chooseConnector(user, pnr);
                    flash.addFlashAttribute("success", "Done! Show ticket " + pnr
                            + " to the conductor of the free CTU connector bus at the stop where your bus ends.");
                }
                case "rebook" -> {
                    var b = disruptionService.rebook(user, pnr, tripId);
                    flash.addFlashAttribute("success", "Rebooked at no cost: " + b.getTrip().getRoute().getCode() + " at "
                            + b.getBoardingTime().toLocalTime() + ", seat " + b.getSeatNo() + ". Same PNR " + pnr + ".");
                }
                case "refund" -> {
                    var b = disruptionService.refundInFull(user, pnr);
                    flash.addFlashAttribute("success", "₹" + b.getRefundAmount() + " (100%) refunded to your TriCity Wallet.");
                }
                case "keep" -> {
                    disruptionService.keep(user, pnr);
                    flash.addFlashAttribute("success", "OK, booking " + pnr + " is kept as it is.");
                }
                default -> flash.addFlashAttribute("error", "Unknown option.");
            }
        } catch (com.tricityconnect.service.BookingException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/alerts";
    }

    @PostMapping("/subscribe")
    public String subscribe(@RequestParam Long routeId, @RequestParam String whatsapp,
                            @RequestParam(defaultValue = "false") boolean notifyBefore,
                            @RequestParam(defaultValue = "false") boolean critical,
                            RedirectAttributes flash) {
        AppUser user = currentUser.require();
        String number = whatsapp.replace("+91", "").replace(" ", "");
        if (!number.matches("[6-9]\\d{9}")) {
            flash.addFlashAttribute("error", "Enter a valid 10-digit WhatsApp number.");
            return "redirect:/alerts";
        }
        AlertSubscription s = new AlertSubscription();
        s.setUser(user);
        s.setRoute(routeRepository.findById(routeId).orElseThrow(() -> new NotFoundException("Route not found")));
        s.setWhatsappNumber(number);
        s.setNotifyBeforeDeparture(notifyBefore);
        s.setCriticalAlerts(critical);
        subscriptionRepository.save(s);
        flash.addFlashAttribute("success", "Subscribed! Alerts for " + s.getRoute().getCode()
                + " will be sent to +91 " + number + ".");
        return "redirect:/alerts";
    }
}
