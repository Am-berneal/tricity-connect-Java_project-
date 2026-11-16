package com.tricityconnect.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A bus corridor, e.g. TC-104 Kharar Bus Stand -> Sector 17 ISBT.
 * Each direction is its own route; returnRoute links the two directions.
 */
@Entity
@Table(name = "routes")
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 60)
    private String tagline;

    private double distanceKm;

    private int frequencyMinutes;

    @ManyToOne
    @JoinColumn(name = "return_route_id")
    private Route returnRoute;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seq ASC")
    private List<RouteStop> stops = new ArrayList<>();

    public Route() {
    }

    public Route(String code, String name, String tagline, double distanceKm, int frequencyMinutes) {
        this.code = code;
        this.name = name;
        this.tagline = tagline;
        this.distanceKm = distanceKm;
        this.frequencyMinutes = frequencyMinutes;
    }

    public RouteStop addStop(Stop stop, double kmFromStart, int minutesFromStart) {
        RouteStop rs = new RouteStop(this, stop, stops.size() + 1, kmFromStart, minutesFromStart);
        stops.add(rs);
        return rs;
    }

    public RouteStop getFirstStop() {
        return stops.get(0);
    }

    public RouteStop getLastStop() {
        return stops.get(stops.size() - 1);
    }

    public RouteStop stopAtSeq(int seq) {
        return stops.stream().filter(s -> s.getSeq() == seq).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No stop " + seq + " on route " + code));
    }

    /** "Kharar Bus Stand → Sector 17 ISBT" */
    public String getEndpoints() {
        return getFirstStop().getStop().getName() + " → " + getLastStop().getStop().getName();
    }

    public boolean isReturnDirection() {
        return code.endsWith("R");
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }
    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
    public int getFrequencyMinutes() { return frequencyMinutes; }
    public void setFrequencyMinutes(int frequencyMinutes) { this.frequencyMinutes = frequencyMinutes; }
    public Route getReturnRoute() { return returnRoute; }
    public void setReturnRoute(Route returnRoute) { this.returnRoute = returnRoute; }
    public List<RouteStop> getStops() { return stops; }
    public void setStops(List<RouteStop> stops) { this.stops = stops; }
}
