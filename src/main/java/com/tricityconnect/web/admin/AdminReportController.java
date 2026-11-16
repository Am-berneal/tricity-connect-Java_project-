package com.tricityconnect.web.admin;

import com.tricityconnect.repository.RouteRepository;
import com.tricityconnect.service.StatsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/reports")
public class AdminReportController {

    private final StatsService statsService;
    private final RouteRepository routeRepository;

    public AdminReportController(StatsService statsService, RouteRepository routeRepository) {
        this.statsService = statsService;
        this.routeRepository = routeRepository;
    }

    private LocalDate[] range(LocalDate from, LocalDate to) {
        LocalDate t = to == null ? LocalDate.now() : to;
        LocalDate f = from == null ? t.minusDays(6) : from;
        if (f.isAfter(t)) {
            f = t;
        }
        return new LocalDate[]{f, t};
    }

    @GetMapping
    public String report(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                         @RequestParam(defaultValue = "") String corridor, Model model) {
        LocalDate[] r = range(from, to);
        model.addAllAttributes(statsService.report(r[0], r[1], corridor));
        model.addAttribute("from", r[0]);
        model.addAttribute("to", r[1]);
        model.addAttribute("corridor", corridor);
        model.addAttribute("corridors", routeRepository.findMainCorridors());
        return "admin/reports";
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    @ResponseBody
    @SuppressWarnings("unchecked")
    public String export(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                         @RequestParam(defaultValue = "") String corridor, HttpServletResponse response) {
        LocalDate[] r = range(from, to);
        response.setHeader("Content-Disposition", "attachment; filename=\"TriCity-Report-" + r[0] + "-to-" + r[1] + ".csv\"");
        Map<String, Object> m = statsService.report(r[0], r[1], corridor);
        StringBuilder sb = new StringBuilder("Route,Corridor,Trips,Completed,Avg occupancy %,Passengers,Gross revenue,Refunds,Net revenue\n");
        for (StatsService.LedgerRow row : (List<StatsService.LedgerRow>) m.get("ledger")) {
            sb.append(row.route().getCode()).append(",\"").append(row.route().getEndpoints()).append("\",")
                    .append(row.trips()).append(',').append(row.completed()).append(',').append(row.avgOccupancy()).append(',')
                    .append(row.passengers()).append(',').append(row.gross()).append(',').append(row.refunds()).append(',')
                    .append(row.net()).append('\n');
        }
        return sb.toString();
    }
}
