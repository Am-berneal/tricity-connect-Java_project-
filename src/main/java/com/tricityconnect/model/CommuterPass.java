package com.tricityconnect.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Monthly commuter pass: a fixed seat on a fixed morning bus (route) and a fixed
 * evening bus (the return route), every weekday between validFrom and validTo.
 */
@Entity
@Table(name = "commuter_passes")
public class CommuterPass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String passNo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PassType passType = PassType.CORRIDOR;

    /** Morning direction, e.g. TC-104 Kharar -> Sector 17 */
    @ManyToOne(optional = false)
    @JoinColumn(name = "route_id")
    private Route route;

    /** Fixed seat on both buses */
    @Column(nullable = false, length = 4)
    private String seatNo;

    @Column(nullable = false)
    private LocalTime morningDeparture;

    @Column(nullable = false)
    private LocalTime eveningDeparture;

    @Column(nullable = false)
    private LocalDate validFrom;

    @Column(nullable = false)
    private LocalDate validTo;

    private int months;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 10, scale = 2)
    private BigDecimal fullPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PassStatus status = PassStatus.ACTIVE;

    private int tripsUsed;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Route getReturnRoute() {
        return route.getReturnRoute();
    }

    public boolean isValidOn(LocalDate date) {
        return status == PassStatus.ACTIVE
                && !date.isBefore(validFrom) && !date.isAfter(validTo)
                && date.getDayOfWeek() != DayOfWeek.SATURDAY
                && date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    /** Does this pass hold its seat on the given trip? */
    public boolean coversTrip(Trip trip) {
        if (!isValidOn(trip.getDepartureTime().toLocalDate())) {
            return false;
        }
        LocalTime t = trip.getDepartureTime().toLocalTime();
        Long tripRouteId = trip.getRoute().getId();
        boolean morning = tripRouteId.equals(route.getId()) && t.equals(morningDeparture);
        boolean evening = getReturnRoute() != null && tripRouteId.equals(getReturnRoute().getId())
                && t.equals(eveningDeparture);
        return morning || evening;
    }

    public long getTotalDays() {
        return ChronoUnit.DAYS.between(validFrom, validTo) + 1;
    }

    public long getDaysLeft() {
        LocalDate today = LocalDate.now();
        if (today.isAfter(validTo)) {
            return 0;
        }
        LocalDate start = today.isBefore(validFrom) ? validFrom : today;
        return ChronoUnit.DAYS.between(start, validTo) + 1;
    }

    public int getProgressPercent() {
        return (int) Math.round(100.0 * (getTotalDays() - getDaysLeft()) / getTotalDays());
    }

    public int getTotalTrips() {
        return 2 * 22 * months;
    }

    public BigDecimal getSavings() {
        return fullPrice == null ? BigDecimal.ZERO : fullPrice.subtract(price);
    }

    public boolean isCurrentlyActive() {
        return status == PassStatus.ACTIVE && !LocalDate.now().isAfter(validTo);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPassNo() { return passNo; }
    public void setPassNo(String passNo) { this.passNo = passNo; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public PassType getPassType() { return passType; }
    public void setPassType(PassType passType) { this.passType = passType; }
    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }
    public String getSeatNo() { return seatNo; }
    public void setSeatNo(String seatNo) { this.seatNo = seatNo; }
    public LocalTime getMorningDeparture() { return morningDeparture; }
    public void setMorningDeparture(LocalTime morningDeparture) { this.morningDeparture = morningDeparture; }
    public LocalTime getEveningDeparture() { return eveningDeparture; }
    public void setEveningDeparture(LocalTime eveningDeparture) { this.eveningDeparture = eveningDeparture; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
    public int getMonths() { return months; }
    public void setMonths(int months) { this.months = months; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getFullPrice() { return fullPrice; }
    public void setFullPrice(BigDecimal fullPrice) { this.fullPrice = fullPrice; }
    public PassStatus getStatus() { return status; }
    public void setStatus(PassStatus status) { this.status = status; }
    public int getTripsUsed() { return tripsUsed; }
    public void setTripsUsed(int tripsUsed) { this.tripsUsed = tripsUsed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
