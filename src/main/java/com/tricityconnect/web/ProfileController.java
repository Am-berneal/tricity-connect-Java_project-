package com.tricityconnect.web;

import com.tricityconnect.model.AppUser;
import com.tricityconnect.model.Booking;
import com.tricityconnect.model.BookingStatus;
import com.tricityconnect.model.CommuterPass;
import com.tricityconnect.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/** Profile & Wallet: personal info, wallet + transaction history, saved routes, emergency contact, language. */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    /** A frequently travelled journey, used for 1-click rebooking. */
    public record SavedRoute(Long fromStopId, Long toStopId, String from, String to, String routeCode,
                             long trips, java.time.LocalTime usualTime, BigDecimal usualFare) {
    }

    private final CurrentUser currentUser;
    private final WalletService walletService;
    private final UserService userService;
    private final BookingService bookingService;
    private final PassService passService;

    public ProfileController(CurrentUser currentUser, WalletService walletService, UserService userService,
                             BookingService bookingService, PassService passService) {
        this.currentUser = currentUser;
        this.walletService = walletService;
        this.userService = userService;
        this.bookingService = bookingService;
        this.passService = passService;
    }

    @GetMapping
    public String profile(@RequestParam(defaultValue = "wallet") String tab,
                          @RequestParam(defaultValue = "all") String filter, Model model) {
        AppUser user = currentUser.require();
        var history = walletService.history(user);
        var shown = switch (filter) {
            case "credits" -> history.stream().filter(t -> t.isCredit()).toList();
            case "debits" -> history.stream().filter(t -> !t.isCredit()).toList();
            case "topups" -> history.stream().filter(t -> t.getKind().name().equals("TOPUP")).toList();
            default -> history;
        };
        model.addAttribute("tab", tab);
        model.addAttribute("filter", filter);
        model.addAttribute("filters", List.of("all", "credits", "debits", "topups"));
        model.addAttribute("transactions", shown);
        model.addAttribute("txCount", history.size());
        model.addAttribute("cities", AuthController.CITIES);
        model.addAttribute("savedRoutes", savedRoutes(bookingService.bookingsOf(user)));
        CommuterPass active = passService.passesOf(user).stream().filter(CommuterPass::isCurrentlyActive).findFirst().orElse(null);
        model.addAttribute("activePass", active);
        return "profile";
    }

    /** Groups the passenger's trips by boarding + drop stop and returns the most frequent ones. */
    static List<SavedRoute> savedRoutes(List<Booking> bookings) {
        Map<String, List<Booking>> groups = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .collect(Collectors.groupingBy(b -> b.getFromStop().getStop().getId() + "-" + b.getToStop().getStop().getId()));
        return groups.values().stream()
                .sorted(Comparator.comparingInt((List<Booking> l) -> l.size()).reversed())
                .limit(4)
                .map(l -> {
                    Booking b = l.get(0);
                    return new SavedRoute(b.getFromStop().getStop().getId(), b.getToStop().getStop().getId(),
                            b.getFromStop().getStop().getName(), b.getToStop().getStop().getName(),
                            b.getTrip().getRoute().getCode(), l.size(), b.getBoardingTime().toLocalTime(), b.getTotalFare());
                }).toList();
    }

    @PostMapping("/wallet/topup")
    public String topUp(@RequestParam BigDecimal amount, @RequestParam(defaultValue = "UPI") String method,
                        RedirectAttributes flash) {
        try {
            AppUser u = walletService.topUp(currentUser.require(), amount, method);
            flash.addFlashAttribute("success", "₹" + amount + " added. New balance ₹" + u.getWalletBalance() + ".");
        } catch (BookingException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile?tab=wallet";
    }

    @PostMapping("/info")
    public String info(@RequestParam String fullName, @RequestParam(required = false) String email,
                       @RequestParam(required = false) String homeCity, RedirectAttributes flash) {
        try {
            userService.updateProfile(currentUser.require(), fullName, email, homeCity);
            flash.addFlashAttribute("success", "Profile updated.");
        } catch (BookingException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile?tab=info";
    }

    @PostMapping("/password")
    public String password(@RequestParam String current, @RequestParam String newPassword,
                           @RequestParam String confirm, RedirectAttributes flash) {
        try {
            userService.changePassword(currentUser.require(), current, newPassword, confirm);
            flash.addFlashAttribute("success", "Password changed.");
        } catch (BookingException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile?tab=info";
    }

    @PostMapping("/emergency")
    public String emergency(@RequestParam(required = false) String name, @RequestParam(required = false) String mobile,
                            RedirectAttributes flash) {
        try {
            userService.updateEmergencyContact(currentUser.require(), name, mobile);
            flash.addFlashAttribute("success", "Emergency contact saved. It will be pre-filled for the women's safety guard.");
        } catch (BookingException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile?tab=contacts";
    }

    /** Statement download (CSV opens in Excel). */
    @GetMapping(value = "/statement.csv", produces = "text/csv")
    @ResponseBody
    public String statement(jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Content-Disposition", "attachment; filename=\"TriCity-Wallet-Statement.csv\"");
        StringBuilder sb = new StringBuilder("Date,Description,Reference,Route,Method,Amount,Wallet\n");
        for (var t : walletService.history(currentUser.require())) {
            sb.append(t.getCreatedAt()).append(',').append(csv(t.getDescription())).append(',')
                    .append(csv(t.getReference())).append(',').append(csv(t.getRouteCode())).append(',')
                    .append(csv(t.getMethod())).append(',').append(t.getAmount()).append(',')
                    .append(t.isWalletImpact() ? "yes" : "no").append('\n');
        }
        return sb.toString();
    }

    static String csv(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
