package com.tricityconnect.web;

import com.tricityconnect.dto.TripOption;
import com.tricityconnect.model.BusType;
import com.tricityconnect.model.Stop;
import com.tricityconnect.repository.StopRepository;
import com.tricityconnect.service.SearchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
public class SearchController {

    private final SearchService searchService;
    private final StopRepository stopRepository;

    public SearchController(SearchService searchService, StopRepository stopRepository) {
        this.searchService = searchService;
        this.stopRepository = stopRepository;
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) Long from,
                         @RequestParam(required = false) Long to,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                         @RequestParam(defaultValue = "all") String slot,
                         @RequestParam(required = false) String busType,
                         @RequestParam(defaultValue = "false") boolean ladies,
                         @RequestParam(required = false) Integer maxFare,
                         @RequestParam(defaultValue = "earliest") String sort,
                         Model model) {
        LocalDate day = date == null ? LocalDate.now() : date;
        if (day.isBefore(LocalDate.now())) {
            day = LocalDate.now();
        }
        List<Stop> stops = stopRepository.findAllByOrderByCityAscNameAsc();
        Stop fromStop = from == null ? null : stopRepository.findById(from).orElse(null);
        Stop toStop = to == null ? null : stopRepository.findById(to).orElse(null);

        List<TripOption> options = searchService.search(from, to, day, slot, busType, ladies, maxFare, sort);

        model.addAttribute("stops", stops);
        model.addAttribute("fromStop", fromStop);
        model.addAttribute("toStop", toStop);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("date", day);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("slot", slot);
        model.addAttribute("busType", busType);
        model.addAttribute("ladies", ladies);
        model.addAttribute("maxFare", maxFare);
        model.addAttribute("sort", sort);
        model.addAttribute("sorts", List.of("earliest", "fastest", "cheapest", "seats"));
        model.addAttribute("busTypes", BusType.values());
        model.addAttribute("options", options);
        model.addAttribute("electricCount", options.stream().filter(o -> o.trip().getBus().getType() == BusType.AC_ELECTRIC).count());
        model.addAttribute("sameStop", from != null && from.equals(to));
        return "results";
    }
}
