package com.tricityconnect.web;

import com.tricityconnect.repository.StopRepository;
import com.tricityconnect.service.SearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.Map;

@Controller
public class HomeController {

    private final StopRepository stopRepository;
    private final SearchService searchService;

    public HomeController(StopRepository stopRepository, SearchService searchService) {
        this.stopRepository = stopRepository;
        this.searchService = searchService;
    }

    @GetMapping("/")
    public String home(Model model) {
        var stops = stopRepository.findAllByOrderByCityAscNameAsc();
        model.addAttribute("stops", stops);
        model.addAttribute("stopOptions", stops.stream()
                .map(s -> Map.of("id", s.getId(), "name", s.getName(), "city", s.getCity())).toList());
        model.addAttribute("defaultFrom", stopRepository.findByName("Kharar Bus Stand").map(s -> s.getId()).orElse(null));
        model.addAttribute("defaultTo", stopRepository.findByName("Sector 17 ISBT").map(s -> s.getId()).orElse(null));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("cards", searchService.corridorCards());
        return "home";
    }
}
