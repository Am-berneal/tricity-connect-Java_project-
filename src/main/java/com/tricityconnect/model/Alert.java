package com.tricityconnect.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** A service bulletin: detour, delay, suspension, festival service, new route, offer, maintenance. */
@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private AlertType type;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    /** Affected route code, or null if network-wide */
    @Column(length = 12)
    private String routeCode;

    /** Short chips shown under the alert, separated by "|" */
    @Column(length = 300)
    private String tags;

    @Column(length = 60)
    private String validWindow;

    private boolean active = true;

    /** Lower number = shown first */
    private int priority;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Alert() {
    }

    public Alert(AlertType type, String title, String message, String routeCode, String tags,
                 String validWindow, int priority) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.routeCode = routeCode;
        this.tags = tags;
        this.validWindow = validWindow;
        this.priority = priority;
    }

    public String[] getTagList() {
        return tags == null || tags.isBlank() ? new String[0] : tags.split("\\|");
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AlertType getType() { return type; }
    public void setType(AlertType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getRouteCode() { return routeCode; }
    public void setRouteCode(String routeCode) { this.routeCode = routeCode; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getValidWindow() { return validWindow; }
    public void setValidWindow(String validWindow) { this.validWindow = validWindow; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
