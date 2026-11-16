package com.tricityconnect.dto;

import com.tricityconnect.model.DisruptionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DisruptionForm {

    @NotNull(message = "Choose the disruption type")
    private DisruptionType type = DisruptionType.SHORT_TERMINATED;

    @NotEmpty(message = "Choose at least one affected route")
    private List<Long> routeIds = new ArrayList<>();

    private String borderImpact = "Chandigarh–Mohali";

    /** Where buses now stop (short-termination / diversion) */
    private Long newLastStopId;

    private int delayMinutes = 15;

    @NotNull(message = "Choose a start time")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @NotNull(message = "Choose an expected end time")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    private String title;

    @NotBlank(message = "Write the reason passengers will see")
    private String reason;

    private boolean offerConnector = true;
    private boolean offerRebook = true;
    private boolean offerRefund = true;
    private boolean notifyPassengers = true;

    public DisruptionType getType() { return type; }
    public void setType(DisruptionType type) { this.type = type; }
    public List<Long> getRouteIds() { return routeIds; }
    public void setRouteIds(List<Long> routeIds) { this.routeIds = routeIds; }
    public String getBorderImpact() { return borderImpact; }
    public void setBorderImpact(String borderImpact) { this.borderImpact = borderImpact; }
    public Long getNewLastStopId() { return newLastStopId; }
    public void setNewLastStopId(Long newLastStopId) { this.newLastStopId = newLastStopId; }
    public int getDelayMinutes() { return delayMinutes; }
    public void setDelayMinutes(int delayMinutes) { this.delayMinutes = delayMinutes; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public boolean isOfferConnector() { return offerConnector; }
    public void setOfferConnector(boolean offerConnector) { this.offerConnector = offerConnector; }
    public boolean isOfferRebook() { return offerRebook; }
    public void setOfferRebook(boolean offerRebook) { this.offerRebook = offerRebook; }
    public boolean isOfferRefund() { return offerRefund; }
    public void setOfferRefund(boolean offerRefund) { this.offerRefund = offerRefund; }
    public boolean isNotifyPassengers() { return notifyPassengers; }
    public void setNotifyPassengers(boolean notifyPassengers) { this.notifyPassengers = notifyPassengers; }
}
