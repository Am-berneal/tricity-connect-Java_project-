package com.tricityconnect.service;

import com.tricityconnect.core.*;
import com.tricityconnect.dto.CheckoutForm;
import com.tricityconnect.dto.RefundPreview;
import com.tricityconnect.model.*;
import com.tricityconnect.repository.BookingRepository;
import com.tricityconnect.repository.TripRepository;
import com.tricityconnect.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Booking a seat, cancelling with refund, and reading a passenger's bookings. */
@Service
public class BookingService {

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final SeatService seatService;
    private final FareService fareService;
    private final WalletService walletService;

    public BookingService(TripRepository tripRepository, BookingRepository bookingRepository,
                          UserRepository userRepository, SeatService seatService,
                          FareService fareService, WalletService walletService) {
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.seatService = seatService;
        this.fareService = fareService;
        this.walletService = walletService;
    }

    /**
     * Books one seat. The whole method runs in one database transaction:
     * the trip row is locked first, so if two people try to book the same seat at the
     * same moment the second one waits, then sees the seat is taken. If anything fails,
     * nothing is saved (wallet is not charged).
     */
    @Transactional
    public Booking book(AppUser user, CheckoutForm form) {
        Trip trip = tripRepository.lockById(form.getTripId())
                .orElseThrow(() -> new NotFoundException("Bus not found"));
        Route route = trip.getRoute();

        if (trip.getStatus() == TripStatus.CANCELLED) {
            throw new BookingException("This bus has been cancelled. Please choose another bus.");
        }
        int fromSeq = form.getFromSeq();
        int toSeq = form.getToSeq();
        if (fromSeq < 1 || toSeq > route.getLastStop().getSeq() || fromSeq >= toSeq) {
            throw new BookingException("Please choose a drop stop after your boarding stop.");
        }
        RouteStop from = route.stopAtSeq(fromSeq);
        RouteStop to = route.stopAtSeq(toSeq);
        if (trip.getTerminatesAtSeq() != null && toSeq > trip.getTerminatesAtSeq()) {
            throw new BookingException("Because of a disruption this bus now ends at "
                    + route.stopAtSeq(trip.getTerminatesAtSeq()).getStop().getName() + ". Please choose another bus.");
        }
        if (!trip.expectedAt(from).isAfter(LocalDateTime.now())) {
            throw new BookingException("This bus has already left your boarding stop.");
        }

        String seat = form.getSeatNo();
        int capacity = trip.getBus().getCapacity();
        if (!SeatLayout.isValid(seat, capacity)) {
            throw new BookingException("Seat " + seat + " does not exist on this bus.");
        }
        if (SeatLayout.isLadiesSeat(seat) && form.getPassengerGender() != Gender.FEMALE) {
            throw new BookingException("Seat " + seat + " is reserved for women passengers.");
        }
        Set<String> taken = seatService.occupiedSeats(trip, fromSeq, toSeq);
        if (taken.contains(seat)) {
            throw new BookingException("Sorry, seat " + seat + " was just booked by someone else. Please pick another seat.");
        }

        Concession concession = form.getConcession() == null ? Concession.GENERAL : form.getConcession();
        FareBreakdown fare = fareService.quote(to.getKmFromStart() - from.getKmFromStart(),
                trip.timeAt(from).toLocalTime(), concession);

        AppUser payer = userRepository.findById(user.getId()).orElseThrow();
        if (form.getPaymentMethod() == PaymentMethod.WALLET
                && payer.getWalletBalance().compareTo(fare.total()) < 0) {
            throw new BookingException("Your TriCity Wallet balance (₹" + payer.getWalletBalance()
                    + ") is not enough. Please choose UPI or card.");
        }

        Booking b = new Booking();
        b.setPnr(newPnr());
        b.setUser(payer);
        b.setTrip(trip);
        b.setSeatNo(seat);
        b.setFromSeq(fromSeq);
        b.setToSeq(toSeq);
        b.setPassengerName(form.getPassengerName().trim());
        b.setPassengerAge(form.getPassengerAge());
        b.setPassengerGender(form.getPassengerGender());
        b.setPassengerMobile(form.getPassengerMobile());
        b.setPassengerEmail(form.getPassengerEmail());
        b.setWhatsappTicket(form.isWhatsappTicket());
        if (form.isWomenSafety() && form.getEmergencyContact() != null && !form.getEmergencyContact().isBlank()) {
            b.setEmergencyContact(form.getEmergencyContact());
        }
        b.setBaseFare(fare.base());
        b.setCgst(fare.cgst());
        b.setSgst(fare.sgst());
        b.setTotalFare(fare.total());
        b.setPaymentMethod(form.getPaymentMethod());
        b.setConcession(concession);
        b.setStatus(BookingStatus.CONFIRMED);
        Booking saved = bookingRepository.save(b);
        walletService.record(payer, WalletTransaction.Kind.BOOKING,
                "Seat reservation (seat " + seat + ") " + from.getStop().getName() + " → " + to.getStop().getName(),
                saved.getPnr(), route.getCode(), form.getPaymentMethod().getLabel(), fare.total().negate(),
                form.getPaymentMethod() == PaymentMethod.WALLET);
        return saved;
    }

    private String newPnr() {
        String pnr;
        do {
            pnr = CodeGenerator.pnr();
        } while (bookingRepository.existsByPnr(pnr));
        return pnr;
    }

    public RefundPreview refundPreview(Booking booking) {
        long minutes = Duration.between(LocalDateTime.now(), booking.getBoardingTime()).toMinutes();
        boolean cancellable = booking.getStatus() == BookingStatus.CONFIRMED && minutes > 0;
        BigDecimal amount = cancellable ? RefundPolicy.refundAmount(booking.getTotalFare(), minutes) : BigDecimal.ZERO;
        return new RefundPreview(cancellable ? RefundPolicy.refundPercent(minutes) : 0, amount, minutes, cancellable);
    }

    /** Cancels a booking and credits the refund to the passenger's TriCity Wallet. */
    @Transactional
    public Booking cancel(AppUser user, String pnr, String reason) {
        Booking b = findForUser(user, pnr);
        RefundPreview preview = refundPreview(b);
        if (!preview.cancellable()) {
            throw new BookingException("This booking can no longer be cancelled.");
        }
        b.setStatus(BookingStatus.CANCELLED);
        b.setRefundAmount(preview.amount());
        b.setCancelReason(reason);
        b.setCancelledAt(LocalDateTime.now());
        walletService.record(b.getUser(), WalletTransaction.Kind.REFUND,
                "Cancelled seat refund (" + preview.percent() + "% tier)", b.getPnr(),
                b.getTrip().getRoute().getCode(), "Credit to Wallet", preview.amount(), true);
        return b;
    }

    @Transactional(readOnly = true)
    public Booking findForUser(AppUser user, String pnr) {
        Booking b = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new NotFoundException("Booking " + pnr + " not found"));
        if (!b.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Booking " + pnr + " not found");
        }
        return b;
    }

    @Transactional(readOnly = true)
    public List<Booking> bookingsOf(AppUser user) {
        return bookingRepository.findByUserOrderByCreatedAtDesc(user);
    }
}
