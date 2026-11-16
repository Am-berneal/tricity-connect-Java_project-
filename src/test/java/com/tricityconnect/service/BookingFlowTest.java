package com.tricityconnect.service;

import com.tricityconnect.dto.CheckoutForm;
import com.tricityconnect.dto.RefundPreview;
import com.tricityconnect.dto.TripOption;
import com.tricityconnect.model.*;
import com.tricityconnect.repository.StopRepository;
import com.tricityconnect.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests: real Spring context, H2 database filled by DataSeeder.
 * Each test is rolled back afterwards (@Transactional).
 */
@SpringBootTest
@Transactional
class BookingFlowTest {

    @Autowired SearchService searchService;
    @Autowired BookingService bookingService;
    @Autowired SeatService seatService;
    @Autowired StopRepository stopRepository;
    @Autowired UserRepository userRepository;
    @Autowired com.tricityconnect.repository.RouteRepository routeRepository;
    @Autowired com.tricityconnect.repository.TripRepository tripRepository;

    private AppUser harpreet() {
        return userRepository.findByMobile("9876543210").orElseThrow();
    }

    private AppUser simran() {
        return userRepository.findByMobile("9812345678").orElseThrow();
    }

    private TripOption tomorrowKhararToSector17() {
        Long kharar = stopRepository.findByName("Kharar Bus Stand").orElseThrow().getId();
        Long sec17 = stopRepository.findByName("Sector 17 ISBT").orElseThrow().getId();
        List<TripOption> options = searchService.search(kharar, sec17, LocalDate.now().plusDays(1), "all",
                null, false, null, "seats");
        assertFalse(options.isEmpty(), "Seeded timetable should have buses tomorrow");
        return options.get(0); // the bus with most free seats
    }

    private String freeSeat(Trip trip) {
        return seatService.seatMap(trip, 1, trip.getRoute().getLastStop().getSeq(), Gender.MALE).stream()
                .filter(s -> s.available()).findFirst().orElseThrow().label();
    }

    private CheckoutForm form(Trip trip, String seat, int from, int to, Gender gender, PaymentMethod pay) {
        CheckoutForm f = new CheckoutForm();
        f.setTripId(trip.getId());
        f.setSeatNo(seat);
        f.setFromSeq(from);
        f.setToSeq(to);
        f.setPassengerName("Test Passenger");
        f.setPassengerAge(30);
        f.setPassengerGender(gender);
        f.setPassengerMobile("9876543210");
        f.setPaymentMethod(pay);
        return f;
    }

    @Test
    void searchFindsKhararToSector17Buses() {
        TripOption o = tomorrowKhararToSector17();
        assertEquals("TC-104", o.trip().getRoute().getCode());
        assertEquals(35, o.minutes());
        assertEquals(19.0, o.km(), 0.01);
    }

    @Test
    void seatCanBeResoldAfterFirstPassengerGetsOff() {
        Trip trip = tomorrowKhararToSector17().trip();
        String seat = freeSeat(trip);

        Booking first = bookingService.book(harpreet(), form(trip, seat, 1, 2, Gender.MALE, PaymentMethod.UPI));
        Booking second = bookingService.book(simran(), form(trip, seat, 2, 5, Gender.FEMALE, PaymentMethod.UPI));

        assertEquals(seat, first.getSeatNo());
        assertEquals(seat, second.getSeatNo());
        assertNotEquals(first.getPnr(), second.getPnr());
    }

    @Test
    void overlappingBookingIsRejected() {
        Trip trip = tomorrowKhararToSector17().trip();
        String seat = freeSeat(trip);
        bookingService.book(harpreet(), form(trip, seat, 1, 3, Gender.MALE, PaymentMethod.UPI));

        BookingException e = assertThrows(BookingException.class,
                () -> bookingService.book(simran(), form(trip, seat, 2, 5, Gender.FEMALE, PaymentMethod.UPI)));
        assertTrue(e.getMessage().contains(seat));
    }

    @Test
    void menCannotBookLadiesSeats() {
        Trip trip = tomorrowKhararToSector17().trip();
        assertThrows(BookingException.class,
                () -> bookingService.book(harpreet(), form(trip, "1A", 1, 2, Gender.MALE, PaymentMethod.UPI)));
    }

    @Test
    void walletIsChargedAndRefundedOnCancel() {
        AppUser user = harpreet();
        BigDecimal start = user.getWalletBalance();
        Trip trip = tomorrowKhararToSector17().trip();

        Booking b = bookingService.book(user, form(trip, freeSeat(trip), 1, 5, Gender.MALE, PaymentMethod.WALLET));
        assertEquals(0, start.subtract(b.getTotalFare()).compareTo(harpreet().getWalletBalance()));

        RefundPreview preview = bookingService.refundPreview(b);
        assertEquals(90, preview.percent(), "Trip is tomorrow, so 90% refund");

        Booking cancelled = bookingService.cancel(user, b.getPnr(), "Change of plans");
        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        assertEquals(0, start.subtract(b.getTotalFare()).add(preview.amount()).compareTo(harpreet().getWalletBalance()));
    }

    @Test
    void passHolderSeatIsBlockedOnTheirBus() {
        // Harpreet's seeded pass fixes seat 4A on the 08:05 TC-104 every weekday this month
        LocalDate day = LocalDate.now().plusDays(1);
        while (day.getDayOfWeek().getValue() > 5) {
            day = day.plusDays(1);
        }
        if (day.getMonthValue() != LocalDate.now().getMonthValue()) {
            return; // pass only covers the current month
        }
        // read the trip directly: tomorrow's 07:00–09:00 TC-104 buses are short-terminated by the demo
        // disruption, so search hides them
        Route tc104 = routeRepository.findByCode("TC-104").orElseThrow();
        Trip trip = tripRepository.findByRouteAndDepartureTimeBetweenOrderByDepartureTime(tc104,
                day.atTime(8, 5), day.atTime(8, 5)).get(0);
        assertTrue(seatService.occupiedSeats(trip, 1, 5).contains("4A"));
    }
}
