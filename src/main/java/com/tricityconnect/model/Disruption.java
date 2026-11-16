package com.tricityconnect.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * An incident published by an admin (e.g. "Punjab buses not entering Chandigarh").
 * Trips of the affected routes inside the time window are marked, passengers are notified,
 * and they choose: free connector bus, rebooking, or full refund.
 */
@Entity
@Table(name = "disruptions")
public class Disruption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisruptionType type;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 1000)
    private String reason;

    /** Comma-separated route codes, e.g. "TC-104,TC-104R" */
    @Column(nullable = false, length = 200)
    private String routeCodes;

    @Column(length = 40)
    private String borderImpact;

    /** For short-termination / diversion: where the buses now stop */
    @ManyToOne
    @JoinColumn(name = "new_last_stop_id")
    private Stop newLastStop;

    private int delayMinutes;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    private boolean offerConnector;
    private boolean offerRebook;
    private boolean offerRefund;

    private int affectedTrips;
    private int affectedPassengers;

    private boolean active = true;

    @OneToOne
    @JoinColumn(name = "alert_id")
    private Alert alert;

    @Column(length = 300)
    private String resolutionNote;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime resolvedAt;

    public List<String> getRouteCodeList() {
        return Arrays.stream(routeCodes.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public DisruptionType getType() { return type; }
    public void setType(DisruptionType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getRouteCodes() { return routeCodes; }
    public void setRouteCodes(String routeCodes) { this.routeCodes = routeCodes; }
    public String getBorderImpact() { return borderImpact; }
    public void setBorderImpact(String borderImpact) { this.borderImpact = borderImpact; }
    public Stop getNewLastStop() { return newLastStop; }
    public void setNewLastStop(Stop newLastStop) { this.newLastStop = newLastStop; }
    public int getDelayMinutes() { return delayMinutes; }
    public void setDelayMinutes(int delayMinutes) { this.delayMinutes = delayMinutes; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public boolean isOfferConnector() { return offerConnector; }
    public void setOfferConnector(boolean offerConnector) { this.offerConnector = offerConnector; }
    public boolean isOfferRebook() { return offerRebook; }
    public void setOfferRebook(boolean offerRebook) { this.offerRebook = offerRebook; }
    public boolean isOfferRefund() { return offerRefund; }
    public void setOfferRefund(boolean offerRefund) { this.offerRefund = offerRefund; }
    public int getAffectedTrips() { return affectedTrips; }
    public void setAffectedTrips(int affectedTrips) { this.affectedTrips = affectedTrips; }
    public int getAffectedPassengers() { return affectedPassengers; }
    public void setAffectedPassengers(int affectedPassengers) { this.affectedPassengers = affectedPassengers; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Alert getAlert() { return alert; }
    public void setAlert(Alert alert) { this.alert = alert; }
    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
