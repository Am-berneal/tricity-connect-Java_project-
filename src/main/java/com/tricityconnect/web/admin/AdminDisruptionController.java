package com.tricityconnect.web.admin;

import com.tricityconnect.dto.DisruptionForm;
import com.tricityconnect.model.Disruption;
import com.tricityconnect.model.DisruptionType;
import com.tricityconnect.repository.RouteRepository;
import com.tricityconnect.repository.StopRepository;
import com.tricityconnect.service.BookingException;
import com.tricityconnect.service.DisruptionService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Controller
@RequestMapping("/admin/disruptions")
public class AdminDisruptionController {

    public static final List<String> BORDERS = List.of("Chandigarh–Mohali", "Chandigarh–Panchkula",
            "Intra-Chandigarh", "All inter-state");

    private final DisruptionService disruptionService;
    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;

    public AdminDisruptionController(DisruptionService disruptionService, RouteRepository routeRepository,
                                     StopRepository stopRepository) {
        this.disruptionService = disruptionService;
        this.routeRepository = routeRepository;
        this.stopRepository = stopRepository;
    }

    @ModelAttribute
    public void common(Model model) {
        model.addAttribute("routes", routeRepository.findAllByOrderByCodeAsc());
        model.addAttribute("stops", stopRepository.findAllByOrderByCityAscNameAsc());
        model.addAttribute("types", DisruptionType.values());
        model.addAttribute("borders", BORDERS);
        List<Disruption> active = disruptionService.active();
        model.addAttribute("active", active);
        model.addAttribute("resolved", disruptionService.resolved().stream().limit(10).toList());
        model.addAttribute("affectedTotal", active.stream().mapToInt(Disruption::getAffectedPassengers).sum());
        model.addAttribute("tripsTotal", active.stream().mapToInt(Disruption::getAffectedTrips).sum());
    }

    @GetMapping
    public String page(Model model) {
        DisruptionForm f = new DisruptionForm();
        LocalDateTime start = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(1);
        f.setStartTime(start);
        f.setEndTime(start.plusHours(3));
        model.addAttribute("form", f);
        return "admin/disruptions";
    }

    @PostMapping
    public String publish(@Valid @ModelAttribute("form") DisruptionForm form, BindingResult result,
                          Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            model.addAttribute("error", result.getAllErrors().get(0).getDefaultMessage());
            return "admin/disruptions";
        }
        try {
            Disruption d = disruptionService.publish(form);
            flash.addFlashAttribute("success", d.getCode() + " published: " + d.getAffectedTrips() + " trips and "
                    + d.getAffectedPassengers() + " passengers affected"
                    + (form.isNotifyPassengers() ? "; passengers notified by app alert." : "."));
            return "redirect:/admin/disruptions";
        } catch (BookingException e) {
            model.addAttribute("error", e.getMessage());
            return "admin/disruptions";
        }
    }

    @PostMapping("/{id}/resolve")
    public String resolve(@PathVariable Long id, @RequestParam(required = false) String note, RedirectAttributes flash) {
        Disruption d = disruptionService.resolve(id, note);
        flash.addFlashAttribute("success", d.getCode() + " resolved. Future trips are back to normal.");
        return "redirect:/admin/disruptions";
    }
}
