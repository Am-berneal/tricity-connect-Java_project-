package com.tricityconnect.web;

import com.tricityconnect.core.PassPricing;
import com.tricityconnect.core.SeatLayout;
import com.tricityconnect.core.Timetable;
import com.tricityconnect.dto.PassForm;
import com.tricityconnect.model.*;
import com.tricityconnect.repository.RouteRepository;
import com.tricityconnect.service.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/** My Passes and the "Get your monthly commuter pass" page. */
@Controller
@RequestMapping("/passes")
public class PassController {

    private final PassService passService;
    private final RouteRepository routeRepository;
    private final QrService qrService;
    private final CurrentUser currentUser;
    private final FareService fareService;

    public PassController(PassService passService, RouteRepository routeRepository, QrService qrService,
                          CurrentUser currentUser, FareService fareService) {
        this.fareService = fareService;
        this.passService = passService;
        this.routeRepository = routeRepository;
        this.qrService = qrService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String myPasses(Model model) {
        AppUser user = currentUser.require();
        List<CommuterPass> passes = passService.passesOf(user);
        CommuterPass active = passes.stream().filter(CommuterPass::isCurrentlyActive).findFirst().orElse(null);
        model.addAttribute("active", active);
        model.addAttribute("past", passes.stream().filter(p -> p != active).toList());
        if (active != null) {
            model.addAttribute("qr", qrService.dataUri("TRICITY-PASS|" + active.getPassNo() + "|" + active.getSeatNo()));
        }
        return "passes";
    }

    /** The buy page. Every choice reloads the page so the seat map and price stay correct. */
    @GetMapping("/new")
    public String newPass(@RequestParam(required = false) Long routeId,
                          @RequestParam(required = false) String morning,
                          @RequestParam(required = false) String evening,
                          @RequestParam(defaultValue = "1") int months,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                          @RequestParam(required = false) String seat,
                          @RequestParam(defaultValue = "CORRIDOR") PassType type,
                          Model model) {
        List<Route> corridors = routeRepository.findMainCorridors();
        Route route = routeId == null ? corridors.get(0)
                : routeRepository.findById(routeId).orElse(corridors.get(0));
        LocalTime m = morning == null ? LocalTime.of(8, 5) : LocalTime.parse(morning);
        LocalTime e = evening == null ? LocalTime.of(18, 0) : LocalTime.parse(evening);
        if (months != 1 && months != 3 && months != 6) {
            months = 1;
        }
        LocalDate startDate = start == null || start.isBefore(LocalDate.now()) ? LocalDate.now().plusDays(1) : start;
        AppUser user = currentUser.require();

        Set<String> taken = passService.seatsTakenByPasses(route, m, e, startDate, months);
        Map<Integer, List<String>> rows = new TreeMap<>();
        for (String label : SeatLayout.seatLabels(36)) {
            rows.computeIfAbsent(SeatLayout.rowOf(label), r -> new ArrayList<>()).add(label);
        }

        Map<Integer, BigDecimal> priceByMonths = new LinkedHashMap<>();
        for (int mo : new int[]{1, 3, 6}) {
            priceByMonths.put(mo, passService.price(route, m, mo, type));
        }

        PassForm form = new PassForm();
        form.setRouteId(route.getId());
        form.setMorningTime(m.toString());
        form.setEveningTime(e.toString());
        form.setMonths(months);
        form.setStartDate(startDate);
        form.setSeatNo(seat);
        form.setPassType(type);

        model.addAttribute("form", form);
        model.addAttribute("corridors", corridors);
        model.addAttribute("route", route);
        model.addAttribute("morningTimes", Timetable.MORNING_PASS_TIMES);
        model.addAttribute("eveningTimes", Timetable.EVENING_PASS_TIMES);
        model.addAttribute("rows", rows);
        model.addAttribute("taken", taken);
        model.addAttribute("ladiesSeats", SeatLayout.seatLabels(36).stream().filter(SeatLayout::isLadiesSeat).toList());
        model.addAttribute("morningTime", m);
        model.addAttribute("eveningTime", e);
        model.addAttribute("isWoman", user.getGender() == Gender.FEMALE);
        model.addAttribute("singleFare", passService.singleFare(route, m));
        model.addAttribute("price", passService.price(route, m, months, type));
        model.addAttribute("fullPrice", passService.fullPrice(route, m, months));
        model.addAttribute("discount", fareService.rules().passDiscountFor(months));
        model.addAttribute("discounts", Map.of(1, fareService.rules().pass1MonthPct(), 3, fareService.rules().pass3MonthPct(),
                6, fareService.rules().pass6MonthPct()));
        model.addAttribute("priceByMonths", priceByMonths);
        model.addAttribute("endDate", PassService.endDate(startDate, months));
        model.addAttribute("today", LocalDate.now());
        return "pass-new";
    }

    @PostMapping
    public String buy(@Valid @ModelAttribute("form") PassForm form, BindingResult result, RedirectAttributes flash) {
        StringBuilder back = new StringBuilder("redirect:/passes/new?months=" + form.getMonths());
        if (form.getRouteId() != null) back.append("&routeId=").append(form.getRouteId());
        if (form.getMorningTime() != null) back.append("&morning=").append(form.getMorningTime());
        if (form.getEveningTime() != null) back.append("&evening=").append(form.getEveningTime());
        if (form.getStartDate() != null) back.append("&start=").append(form.getStartDate());
        if (form.getPassType() != null) back.append("&type=").append(form.getPassType());
        if (result.hasErrors()) {
            flash.addFlashAttribute("error", result.getAllErrors().get(0).getDefaultMessage());
            return back.toString();
        }
        try {
            CommuterPass pass = passService.buy(currentUser.require(), form);
            flash.addFlashAttribute("success", "Pass " + pass.getPassNo() + " is ready. Seat " + pass.getSeatNo()
                    + " is yours every weekday.");
            return "redirect:/passes";
        } catch (BookingException | IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return back.toString();
        }
    }
}
