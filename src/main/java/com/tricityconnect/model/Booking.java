package com.tricityconnect.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A seat booking for part of a trip. The seat is held only from fromSeq to toSeq
 * (stop-wise booking), so the same seat can be sold again after this passenger gets off.
 */
@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_booking_trip_seat", columnList = "trip_id, seat_no"),
        @Index(name = "idx_booking_user", columnList = "user_id")
})
public class Booking {

    // SEQUENCE ids let Hibernate insert bookings in batches (much faster seeding on MySQL)
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "booking_seq")
    @SequenceGenerator(name = "booking_seq", sequenceName = "booking_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String pnr;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Column(name = "seat_no", nullable = false, length = 4)
    private String seatNo;

    /** Boarding stop number on the route */
    private int fromSeq;

    /** Drop stop number on the route */
    private int toSeq;

    @Column(nullable = false, length = 100)
    private String passengerName;

    private int passengerAge;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender passengerGender;

    @Column(length = 10)
    private String passengerMobile;

    @Column(length = 120)
    private String passengerEmail;

    @Column(precision = 10, scale = 2)
    private BigDecimal baseFare;
    @Column(precision = 10, scale = 2)
    private BigDecimal cgst;
    @Column(precision = 10, scale = 2)
    private BigDecimal sgst;
    @Column(precision = 10, scale = 2)
    private BigDecimal totalFare;

    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private BookingStatus status = BookingStatus.CONFIRMED;

    private boolean whatsappTicket;

    /** Women's safety mode: share live trip with this number */
    @Column(length = 10)
    private String emergencyContact;

    @Column(precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(length = 120)
    private String cancelReason;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private com.tricityconnect.core.Concession concession = com.tricityconnect.core.Concession.GENERAL;

    /** Set by the conductor when the ticket QR is scanned */
    private boolean boarded;
    private LocalDateTime boardedAt;

    /** Passenger's answer to a disruption: CONNECTOR, REBOOKED or REFUNDED */
    @Column(length = 12)
    private String disruptionChoice;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime cancelledAt;

    public RouteStop getFromStop() {
        return trip.getRoute().stopAtSeq(fromSeq);
    }

    public RouteStop getToStop() {
        return trip.getRoute().stopAtSeq(toSeq);
    }

    /** When the bus is scheduled to reach this passenger's boarding stop. */
    public LocalDateTime getBoardingTime() {
        return trip.timeAt(getFromStop());
    }

    public LocalDateTime getDropTime() {
        return trip.timeAt(getToStop());
    }

    public double getDistanceKm() {
        return getToStop().getKmFromStart() - getFromStop().getKmFromStart();
    }

    public long getDurationMinutes() {
        return getToStop().getMinutesFromStart() - getFromStop().getMinutesFromStart();
    }

    /** Is this booking hit by an active disruption on its trip? */
    public boolean isAffectedByDisruption() {
        if (status != BookingStatus.CONFIRMED || !trip.isDisrupted() || disruptionChoice != null) {
            return false;
        }
        if (trip.getStatus() == TripStatus.CANCELLED) {
            return true;
        }
        if (trip.getTerminatesAtSeq() != null) {
            return toSeq > trip.getTerminatesAtSeq();
        }
        return true;
    }

    public boolean isUpcoming() {
        return status == BookingStatus.CONFIRMED && getDropTime().isAfter(LocalDateTime.now());
    }

    public boolean isCompleted() {
        return status == BookingStatus.CONFIRMED && !getDropTime().isAfter(LocalDateTime.now());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }
    public String getSeatNo() { return seatNo; }
    public void setSeatNo(String seatNo) { this.seatNo = seatNo; }
    public int getFromSeq() { return fromSeq; }
    public void setFromSeq(int fromSeq) { this.fromSeq = fromSeq; }
    public int getToSeq() { return toSeq; }
    public void setToSeq(int toSeq) { this.toSeq = toSeq; }
    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }
    public int getPassengerAge() { return passengerAge; }
    public void setPassengerAge(int passengerAge) { this.passengerAge = passengerAge; }
    public Gender getPassengerGender() { return passengerGender; }
    public void setPassengerGender(Gender passengerGender) { this.passengerGender = passengerGender; }
    public String getPassengerMobile() { return passengerMobile; }
    public void setPassengerMobile(String passengerMobile) { this.passengerMobile = passengerMobile; }
    public String getPassengerEmail() { return passengerEmail; }
    public void setPassengerEmail(String passengerEmail) { this.passengerEmail = passengerEmail; }
    public BigDecimal getBaseFare() { return baseFare; }
    public void setBaseFare(BigDecimal baseFare) { this.baseFare = baseFare; }
    public BigDecimal getCgst() { return cgst; }
    public void setCgst(BigDecimal cgst) { this.cgst = cgst; }
    public BigDecimal getSgst() { return sgst; }
    public void setSgst(BigDecimal sgst) { this.sgst = sgst; }
    public BigDecimal getTotalFare() { return totalFare; }
    public void setTotalFare(BigDecimal totalFare) { this.totalFare = totalFare; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public boolean isWhatsappTicket() { return whatsappTicket; }
    public void setWhatsappTicket(boolean whatsappTicket) { this.whatsappTicket = whatsappTicket; }
    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public com.tricityconnect.core.Concession getConcession() { return concession; }
    public void setConcession(com.tricityconnect.core.Concession concession) { this.concession = concession; }
    public boolean isBoarded() { return boarded; }
    public void setBoarded(boolean boarded) { this.boarded = boarded; }
    public LocalDateTime getBoardedAt() { return boardedAt; }
    public void setBoardedAt(LocalDateTime boardedAt) { this.boardedAt = boardedAt; }
    public String getDisruptionChoice() { return disruptionChoice; }
    public void setDisruptionChoice(String disruptionChoice) { this.disruptionChoice = disruptionChoice; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
}
