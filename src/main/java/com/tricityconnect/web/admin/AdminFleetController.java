package com.tricityconnect.web.admin;

import com.tricityconnect.model.Bus;
import com.tricityconnect.model.BusStatus;
import com.tricityconnect.model.BusType;
import com.tricityconnect.service.BookingException;
import com.tricityconnect.service.FleetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/admin/buses")
public class AdminFleetController {

    public static final List<String> DEPOTS = List.of("CTU Depot 1, Industrial Area", "CTU Depot 2, Sector 25",
            "CTU Depot 4, Hallomajra");

    private final FleetService fleetService;

    public AdminFleetController(FleetService fleetService) {
        this.fleetService = fleetService;
    }

    @GetMapping
    public String buses(@RequestParam(defaultValue = "") String q, @RequestParam(required = false) BusType type,
                        @RequestParam(required = false) BusStatus status, Model model) {
        List<Bus> all = fleetService.all();
        List<Bus> shown = all.stream()
                .filter(b -> q.isBlank() || b.getRegNo().toLowerCase().contains(q.toLowerCase()))
                .filter(b -> type == null || b.getType() == type)
                .filter(b -> status == null || b.getStatus() == status)
                .toList();
        model.addAttribute("buses", shown);
        model.addAttribute("total", all.size());
        model.addAttribute("electric", all.stream().filter(b -> b.getType() == BusType.AC_ELECTRIC).count());
        model.addAttribute("countActive", all.stream().filter(b -> b.getStatus() == BusStatus.ACTIVE).count());
        model.addAttribute("countCharging", all.stream().filter(b -> b.getStatus() == BusStatus.CHARGING || b.getStatus() == BusStatus.STANDBY).count());
        model.addAttribute("countMaintenance", all.stream().filter(b -> b.getStatus() == BusStatus.MAINTENANCE).count());
        model.addAttribute("nextTrip", fleetService.nextTripByBus());
        model.addAttribute("types", BusType.values());
        model.addAttribute("statuses", BusStatus.values());
        model.addAttribute("depots", DEPOTS);
        model.addAttribute("q", q);
        model.addAttribute("type", type);
        model.addAttribute("status", status);
        model.addAttribute("seatRows", java.util.stream.IntStream.rangeClosed(1, 9).boxed().toList());
        model.addAttribute("cols", Arrays.asList("A", "B", "C", "D"));
        return "admin/buses";
    }

    @PostMapping
    public String add(@RequestParam String regNo, @RequestParam BusType type, @RequestParam int capacity,
                      @RequestParam(required = false) String model, @RequestParam String depot, RedirectAttributes flash) {
        try {
            Bus b = fleetService.add(regNo, type, capacity, model, depot);
            flash.addFlashAttribute("success", "Bus " + b.getRegNo() + " added to the fleet (status: standby).");
        } catch (BookingException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/buses";
    }

    @PostMapping("/{id}/status")
    public String status(@PathVariable Long id, @RequestParam BusStatus status, RedirectAttributes flash) {
        Bus b = fleetService.setStatus(id, status);
        flash.addFlashAttribute("success", b.getRegNo() + " is now: " + status.getLabel());
        return "redirect:/admin/buses";
    }
}
