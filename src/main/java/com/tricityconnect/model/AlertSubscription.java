package com.tricityconnect.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** A passenger's request for WhatsApp/SMS alerts on their regular route. */
@Entity
@Table(name = "alert_subscriptions")
public class AlertSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "route_id")
    private Route route;

    @Column(nullable = false, length = 10)
    private String whatsappNumber;

    private boolean notifyBeforeDeparture = true;

    private boolean criticalAlerts = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }
    public String getWhatsappNumber() { return whatsappNumber; }
    public void setWhatsappNumber(String whatsappNumber) { this.whatsappNumber = whatsappNumber; }
    public boolean isNotifyBeforeDeparture() { return notifyBeforeDeparture; }
    public void setNotifyBeforeDeparture(boolean notifyBeforeDeparture) { this.notifyBeforeDeparture = notifyBeforeDeparture; }
    public boolean isCriticalAlerts() { return criticalAlerts; }
    public void setCriticalAlerts(boolean criticalAlerts) { this.criticalAlerts = criticalAlerts; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
