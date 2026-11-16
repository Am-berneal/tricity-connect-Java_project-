package com.tricityconnect.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** One scheduled run of a bus on a route, e.g. TC-104 leaving Kharar at 07:45 on 24 Oct. */
@Entity
@Table(name = "trips", indexes = @Index(name = "idx_trip_route_time", columnList = "route_id, departure_time"))
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trip_seq")
    @SequenceGenerator(name = "trip_seq", sequenceName = "trip_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "route_id")
    private Route route;

    @ManyToOne(optional = false)
    @JoinColumn(name = "bus_id")
    private Bus bus;

    /** Departure time from the first stop of the route. */
    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private TripStatus status = TripStatus.SCHEDULED;

    private int delayMinutes;

    @Column(length = 40)
    private String bay;

    @Column(length = 60)
    private String driverName;

    /** If a disruption makes the bus end early: the last stop it now reaches (null = normal). */
    private Integer terminatesAtSeq;

    @ManyToOne
    @JoinColumn(name = "disruption_id")
    private Disruption disruption;

    public Trip() {
    }

    public Trip(Route route, Bus bus, LocalDateTime departureTime, String bay, String driverName) {
        this.route = route;
        this.bus = bus;
        this.departureTime = departureTime;
        this.bay = bay;
        this.driverName = driverName;
    }

    /** Scheduled time the bus reaches a stop (without delay). */
    public LocalDateTime timeAt(RouteStop stop) {
        return departureTime.plusMinutes(stop.getMinutesFromStart());
    }

    /** Expected time including any delay. */
    public LocalDateTime expectedAt(RouteStop stop) {
        return timeAt(stop).plusMinutes(delayMinutes);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }
    public Bus getBus() { return bus; }
    public void setBus(Bus bus) { this.bus = bus; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }
    public TripStatus getStatus() { return status; }
    public void setStatus(TripStatus status) { this.status = status; }
    public int getDelayMinutes() { return delayMinutes; }
    public void setDelayMinutes(int delayMinutes) { this.delayMinutes = delayMinutes; }
    public String getBay() { return bay; }
    public void setBay(String bay) { this.bay = bay; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public Integer getTerminatesAtSeq() { return terminatesAtSeq; }
    public void setTerminatesAtSeq(Integer terminatesAtSeq) { this.terminatesAtSeq = terminatesAtSeq; }
    public Disruption getDisruption() { return disruption; }
    public void setDisruption(Disruption disruption) { this.disruption = disruption; }

    public boolean isDisrupted() {
        return disruption != null && disruption.isActive();
    }
}
