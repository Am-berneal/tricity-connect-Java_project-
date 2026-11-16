package com.tricityconnect.model;

import jakarta.persistence.*;

/** One stop on a route, in order (seq 1, 2, 3 ...), with distance and time from the first stop. */
@Entity
@Table(name = "route_stops",
        uniqueConstraints = @UniqueConstraint(columnNames = {"route_id", "seq"}))
public class RouteStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "route_id")
    private Route route;

    @ManyToOne(optional = false)
    @JoinColumn(name = "stop_id")
    private Stop stop;

    @Column(nullable = false)
    private int seq;

    private double kmFromStart;

    private int minutesFromStart;

    public RouteStop() {
    }

    public RouteStop(Route route, Stop stop, int seq, double kmFromStart, int minutesFromStart) {
        this.route = route;
        this.stop = stop;
        this.seq = seq;
        this.kmFromStart = kmFromStart;
        this.minutesFromStart = minutesFromStart;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }
    public Stop getStop() { return stop; }
    public void setStop(Stop stop) { this.stop = stop; }
    public int getSeq() { return seq; }
    public void setSeq(int seq) { this.seq = seq; }
    public double getKmFromStart() { return kmFromStart; }
    public void setKmFromStart(double kmFromStart) { this.kmFromStart = kmFromStart; }
    public int getMinutesFromStart() { return minutesFromStart; }
    public void setMinutesFromStart(int minutesFromStart) { this.minutesFromStart = minutesFromStart; }
}
