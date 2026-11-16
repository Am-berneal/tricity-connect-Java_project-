package com.tricityconnect.dto;

import com.tricityconnect.model.PassType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class PassForm {

    @NotNull(message = "Please choose a corridor")
    private Long routeId;

    @NotBlank(message = "Please choose a morning bus")
    private String morningTime;

    @NotBlank(message = "Please choose an evening bus")
    private String eveningTime;

    @NotBlank(message = "Please choose your fixed seat")
    private String seatNo;

    private int months = 1;

    @NotNull(message = "Please choose a start date")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    private PassType passType = PassType.CORRIDOR;

    public Long getRouteId() { return routeId; }
    public void setRouteId(Long routeId) { this.routeId = routeId; }
    public String getMorningTime() { return morningTime; }
    public void setMorningTime(String morningTime) { this.morningTime = morningTime; }
    public String getEveningTime() { return eveningTime; }
    public void setEveningTime(String eveningTime) { this.eveningTime = eveningTime; }
    public String getSeatNo() { return seatNo; }
    public void setSeatNo(String seatNo) { this.seatNo = seatNo; }
    public int getMonths() { return months; }
    public void setMonths(int months) { this.months = months; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public PassType getPassType() { return passType; }
    public void setPassType(PassType passType) { this.passType = passType; }
}
