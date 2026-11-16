package com.tricityconnect.service;

import com.tricityconnect.core.*;
import com.tricityconnect.model.FareSettings;
import com.tricityconnect.repository.FareSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;

/** Reads the current fare rules (set by admins on the Fare Rules page) and quotes fares. */
@Service
public class FareService {

    private final FareSettingsRepository repository;

    public FareService(FareSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public FareRules rules() {
        return repository.findById(1L).map(FareSettings::toRules).orElse(FareRules.DEFAULT);
    }

    @Transactional(readOnly = true)
    public FareSettings settings() {
        return repository.findById(1L).orElseGet(() -> FareSettings.from(FareRules.DEFAULT));
    }

    public FareBreakdown quote(double km, LocalTime time) {
        return FareCalculator.calculate(km, time, rules(), 0);
    }

    public FareBreakdown quote(double km, LocalTime time, Concession concession) {
        FareRules r = rules();
        return FareCalculator.calculate(km, time, r, r.discountFor(concession));
    }

    /** Same as quote but with rules already loaded (used in loops). */
    public static FareBreakdown quote(double km, LocalTime time, FareRules rules) {
        return FareCalculator.calculate(km, time, rules, 0);
    }

    @Transactional
    public FareSettings save(FareSettings form, String adminName) {
        if (form.getRatePerKm() == null || form.getRatePerKm().signum() <= 0) {
            throw new BookingException("Rate per km must be more than zero.");
        }
        if (form.getPeakMultiplier() == null || form.getOffPeakMultiplier() == null
                || form.getPeakMultiplier().signum() <= 0 || form.getOffPeakMultiplier().signum() <= 0) {
            throw new BookingException("Multipliers must be more than zero.");
        }
        if (form.getMinFare() == null || form.getMinFare().signum() < 0 || form.getGstPercent() == null
                || form.getGstPercent().signum() < 0) {
            throw new BookingException("Minimum fare and GST cannot be negative.");
        }
        for (int pct : new int[]{form.getStudentDiscountPct(), form.getSeniorDiscountPct(),
                form.getPass1MonthPct(), form.getPass3MonthPct(), form.getPass6MonthPct()}) {
            if (pct < 0 || pct > 90) {
                throw new BookingException("Discounts must be between 0% and 90%.");
            }
        }
        form.setId(1L);
        form.setUpdatedBy(adminName);
        form.setUpdatedAt(LocalDateTime.now());
        return repository.save(form);
    }
}
