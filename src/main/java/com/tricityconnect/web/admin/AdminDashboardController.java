package com.tricityconnect.web.admin;

import com.tricityconnect.repository.DisruptionRepository;
import com.tricityconnect.service.StatsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Admin home: fleet operations dashboard. */
@Controller
public class AdminDashboardController {

    private final StatsService statsService;
    private final DisruptionRepository disruptionRepository;

    public AdminDashboardController(StatsService statsService, DisruptionRepository disruptionRepository) {
        this.statsService = statsService;
        this.disruptionRepository = disruptionRepository;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAllAttributes(statsService.dashboard());
        model.addAttribute("activeDisruptions", disruptionRepository.findByActiveTrueOrderByCreatedAtDesc());
        model.addAttribute("trendDays", StatsService.TREND_DAYS);
        return "admin/dashboard";
    }
}
