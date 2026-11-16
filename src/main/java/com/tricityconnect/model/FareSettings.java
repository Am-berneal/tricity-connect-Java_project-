package com.tricityconnect.model;

import com.tricityconnect.core.FareRules;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** The single row (id = 1) of fare rules that admins edit on the Fare Rules page. */
@Entity
@Table(name = "fare_settings")
public class FareSettings {

    @Id
    private Long id = 1L;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal ratePerKm;
    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal peakMultiplier;
    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal offPeakMultiplier;
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal minFare;
    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal gstPercent;
    private int studentDiscountPct;
    private int seniorDiscountPct;
    private int pass1MonthPct;
    private int pass3MonthPct;
    private int pass6MonthPct;

    @Column(length = 100)
    private String updatedBy;
    private LocalDateTime updatedAt;

    public static FareSettings from(FareRules r) {
        FareSettings s = new FareSettings();
        s.ratePerKm = r.ratePerKm();
        s.peakMultiplier = r.peakMultiplier();
        s.offPeakMultiplier = r.offPeakMultiplier();
        s.minFare = r.minFare();
        s.gstPercent = r.gstPercent();
        s.studentDiscountPct = r.studentDiscountPct();
        s.seniorDiscountPct = r.seniorDiscountPct();
        s.pass1MonthPct = r.pass1MonthPct();
        s.pass3MonthPct = r.pass3MonthPct();
        s.pass6MonthPct = r.pass6MonthPct();
        s.updatedAt = LocalDateTime.now();
        s.updatedBy = "System default";
        return s;
    }

    public FareRules toRules() {
        return new FareRules(ratePerKm, peakMultiplier, offPeakMultiplier, minFare, gstPercent,
                studentDiscountPct, seniorDiscountPct, pass1MonthPct, pass3MonthPct, pass6MonthPct);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BigDecimal getRatePerKm() { return ratePerKm; }
    public void setRatePerKm(BigDecimal ratePerKm) { this.ratePerKm = ratePerKm; }
    public BigDecimal getPeakMultiplier() { return peakMultiplier; }
    public void setPeakMultiplier(BigDecimal peakMultiplier) { this.peakMultiplier = peakMultiplier; }
    public BigDecimal getOffPeakMultiplier() { return offPeakMultiplier; }
    public void setOffPeakMultiplier(BigDecimal offPeakMultiplier) { this.offPeakMultiplier = offPeakMultiplier; }
    public BigDecimal getMinFare() { return minFare; }
    public void setMinFare(BigDecimal minFare) { this.minFare = minFare; }
    public BigDecimal getGstPercent() { return gstPercent; }
    public void setGstPercent(BigDecimal gstPercent) { this.gstPercent = gstPercent; }
    public int getStudentDiscountPct() { return studentDiscountPct; }
    public void setStudentDiscountPct(int studentDiscountPct) { this.studentDiscountPct = studentDiscountPct; }
    public int getSeniorDiscountPct() { return seniorDiscountPct; }
    public void setSeniorDiscountPct(int seniorDiscountPct) { this.seniorDiscountPct = seniorDiscountPct; }
    public int getPass1MonthPct() { return pass1MonthPct; }
    public void setPass1MonthPct(int pass1MonthPct) { this.pass1MonthPct = pass1MonthPct; }
    public int getPass3MonthPct() { return pass3MonthPct; }
    public void setPass3MonthPct(int pass3MonthPct) { this.pass3MonthPct = pass3MonthPct; }
    public int getPass6MonthPct() { return pass6MonthPct; }
    public void setPass6MonthPct(int pass6MonthPct) { this.pass6MonthPct = pass6MonthPct; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
