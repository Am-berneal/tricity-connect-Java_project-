package com.tricityconnect.web;

import com.tricityconnect.service.FareService;
import com.tricityconnect.dto.SeatView;
import com.tricityconnect.model.*;
import com.tricityconnect.repository.TripRepository;
import com.tricityconnect.service.NotFoundException;
import com.tricityconnect.service.SeatService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Seat selection page: the bus layout for the passenger's own boarding and drop stops. */
@Controller
public class SeatController {

    private final TripRepository tripRepository;
    private final SeatService seatService;
    private final CurrentUser currentUser;
    private final FareService fareService;

    public SeatController(TripRepository tripRepository, SeatService seatService, CurrentUser currentUser,
                          FareService fareService) {
        this.fareService = fareService;
        this.tripRepository = tripRepository;
        this.seatService = seatService;
        this.currentUser = currentUser;
    }

    @GetMapping("/trips/{id}/seats")
    public String seats(@PathVariable Long id,
                        @RequestParam(defaultValue = "1") int from,
                        @RequestParam(defaultValue = "0") int to,
                        @RequestParam(required = false) String seat,
                        Model model) {
        Trip trip = tripRepository.findById(id).orElseThrow(() -> new NotFoundException("Bus not found"));
        Route route = trip.getRoute();
        int last = route.getLastStop().getSeq();
        if (to <= 0 || to > last) {
            to = last;
        }
        if (trip.getTerminatesAtSeq() != null && to > trip.getTerminatesAtSeq()) {
            to = trip.getTerminatesAtSeq();
        }
        if (from < 1 || from >= to) {
            from = 1;
        }
        AppUser user = currentUser.require();
        RouteStop fromStop = route.stopAtSeq(from);
        RouteStop toStop = route.stopAtSeq(to);
        List<SeatView> seats = seatService.seatMap(trip, from, to, user.getGender());

        // group seats by row for the bus layout
        Map<Integer, List<SeatView>> rows = new TreeMap<>();
        for (SeatView s : seats) {
            rows.computeIfAbsent(s.row(), r -> new java.util.ArrayList<>()).add(s);
        }
        SeatView selected = seats.stream().filter(s -> s.label().equals(seat) && s.available()).findFirst().orElse(null);

        model.addAttribute("trip", trip);
        model.addAttribute("route", route);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("fromStop", fromStop);
        model.addAttribute("toStop", toStop);
        model.addAttribute("rows", rows);
        model.addAttribute("selected", selected);
        model.addAttribute("seatsLeft", seats.stream().filter(s -> s.state() == SeatView.SeatState.AVAILABLE
                || s.state() == SeatView.SeatState.LADIES_LOCKED).count());
        model.addAttribute("fare", fareService.quote(toStop.getKmFromStart() - fromStop.getKmFromStart(),
                trip.timeAt(fromStop).toLocalTime()));
        model.addAttribute("isWoman", user.getGender() == Gender.FEMALE);
        return "seats";
    }
}
