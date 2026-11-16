package com.tricityconnect.service;

import com.tricityconnect.dto.DisruptionForm;
import com.tricityconnect.model.*;
import com.tricityconnect.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Integration tests for the admin, disruption, wallet and conductor features (H2 + seeded demo data). */
@SpringBootTest
@Transactional
class OperationsFlowTest {

    @Autowired DisruptionService disruptionService;
    @Autowired ConductorService conductorService;
    @Autowired TripAdminService tripAdminService;
    @Autowired StatsService statsService;
    @Autowired FareService fareService;
    @Autowired UserRepository userRepository;
    @Autowired RouteRepository routeRepository;
    @Autowired TripRepository tripRepository;
    @Autowired BookingRepository bookingRepository;

    private AppUser harpreet() {
        return userRepository.findByMobile("9876543210").orElseThrow();
    }

    @Test
    void seededDisruptionAffectsHarpreetAndRebookKeepsHerSeat() {
        List<Booking> affected = disruptionService.affectedBookings(harpreet());
        assertFalse(affected.isEmpty(), "Demo disruption should affect Harpreet's 07:45 booking");
        Booking b = affected.get(0);
        String seat = b.getSeatNo();
        assertFalse(disruptionService.rebookOptions(b, 3).isEmpty());

        Booking moved = disruptionService.rebook(harpreet(), b.getPnr(), null);
        assertFalse(moved.isAffectedByDisruption());
        assertTrue(moved.getTrip().getDepartureTime().isAfter(b.getTrip().getDepartureTime()));
        assertEquals(seat, moved.getSeatNo(), "Same seat is kept when it is free on the later bus");
    }

    @Test
    void suspensionGivesFullRefundToWallet() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        DisruptionForm f = new DisruptionForm();
        f.setType(DisruptionType.SUSPENDED);
        f.setRouteIds(List.of(routeRepository.findByCode("TC-104").orElseThrow().getId()));
        f.setStartTime(tomorrow.atTime(17, 0));
        f.setEndTime(tomorrow.atTime(20, 0));
        f.setReason("Test strike");
        Disruption d = disruptionService.publish(f);
        assertTrue(d.getAffectedTrips() > 0);

        Booking hit = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED && b.getUser() != null && b.getTrip().getDisruption() != null
                        && b.getTrip().getDisruption().getId().equals(d.getId()))
                .findFirst().orElseThrow();
        AppUser owner = hit.getUser();
        BigDecimal before = owner.getWalletBalance();
        Booking refunded = disruptionService.refundInFull(owner, hit.getPnr());
        assertEquals(0, refunded.getRefundAmount().compareTo(hit.getTotalFare()), "Disruption refunds are 100%");
        assertEquals(0, before.add(hit.getTotalFare()).compareTo(owner.getWalletBalance()));

        disruptionService.resolve(d.getId(), "done");
        assertFalse(disruptionService.active().stream().anyMatch(x -> x.getId().equals(d.getId())));
    }

    @Test
    void conductorValidatesTicketOnceAndRejectsUnknown() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Booking b = bookingRepository.findAll().stream()
                .filter(x -> x.getStatus() == BookingStatus.CONFIRMED && !x.isBoarded()
                        && x.getTrip().getDepartureTime().toLocalDate().equals(tomorrow))
                .findFirst().orElseThrow();
        Long tripId = b.getTrip().getId();
        assertTrue(conductorService.validate(tripId, "TRICITY|" + b.getPnr() + "|x|y|z").startsWith("✓"));
        assertTrue(conductorService.validate(tripId, b.getPnr()).startsWith("Already checked in"));
        assertThrows(BookingException.class, () -> conductorService.validate(tripId, "TCNOPE00"));
    }

    @Test
    void cancellingATripRefundsEveryone() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Trip t = tripRepository.findByRouteAndDepartureTimeBetweenOrderByDepartureTime(
                routeRepository.findByCode("TC-215").orElseThrow(), tomorrow.atTime(8, 5), tomorrow.atTime(8, 5)).get(0);
        int refunded = tripAdminService.cancel(t.getId(), "Breakdown");
        assertTrue(refunded > 0);
        assertEquals(TripStatus.CANCELLED, tripRepository.findById(t.getId()).orElseThrow().getStatus());
    }

    @Test
    void reportAndDashboardHaveNumbers() {
        Map<String, Object> d = statsService.dashboard();
        assertNotNull(d.get("tripsToday"));
        Map<String, Object> r = statsService.report(LocalDate.now().minusDays(6), LocalDate.now(), "");
        assertTrue(((Integer) r.get("passengers")) > 0);
        assertFalse(((List<?>) r.get("ledger")).isEmpty());
    }

    @Test
    void savedFareRulesChangeNewQuotes() {
        FareSettings s = fareService.settings();
        BigDecimal before = fareService.quote(19, java.time.LocalTime.of(8, 0)).total();
        s.setRatePerKm(new BigDecimal("2.50"));
        fareService.save(s, "Test Admin");
        BigDecimal after = fareService.quote(19, java.time.LocalTime.of(8, 0)).total();
        assertTrue(after.compareTo(before) > 0);
    }
}
