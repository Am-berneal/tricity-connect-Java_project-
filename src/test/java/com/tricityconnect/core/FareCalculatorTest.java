package com.tricityconnect.core;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class FareCalculatorTest {

    @Test
    void peakFareForKhararToSector17() {
        // 19 km x Rs 2 x 1.2 peak = 45.6 -> rounded to nearest 5 = 45
        FareBreakdown f = FareCalculator.calculate(19, LocalTime.of(7, 45));
        assertEquals(new BigDecimal("45.00"), f.total());
        assertEquals(new BigDecimal("42.86"), f.base());
        assertEquals(new BigDecimal("1.07"), f.cgst());
        assertEquals(new BigDecimal("1.07"), f.sgst());
        assertTrue(f.peak());
    }

    @Test
    void offPeakFareIsCheaper() {
        FareBreakdown f = FareCalculator.calculate(19, LocalTime.NOON);
        assertEquals(new BigDecimal("40.00"), f.total());
        assertFalse(f.peak());
    }

    @Test
    void partsAlwaysAddUpToTotal() {
        for (int km = 1; km <= 25; km++) {
            FareBreakdown f = FareCalculator.calculate(km, LocalTime.of(18, 0));
            assertEquals(0, f.base().add(f.cgst()).add(f.sgst()).compareTo(f.total()), "km=" + km);
        }
    }

    @Test
    void minimumFareIsTenRupees() {
        assertEquals(new BigDecimal("10.00"), FareCalculator.calculate(1, LocalTime.NOON).total());
    }

    @Test
    void peakHourBoundaries() {
        assertTrue(FareCalculator.isPeak(LocalTime.of(7, 0)));
        assertFalse(FareCalculator.isPeak(LocalTime.of(10, 0)));
        assertTrue(FareCalculator.isPeak(LocalTime.of(19, 59)));
        assertFalse(FareCalculator.isPeak(LocalTime.of(20, 0)));
    }

    @Test
    void rejectsZeroDistance() {
        assertThrows(IllegalArgumentException.class, () -> FareCalculator.calculate(0, LocalTime.NOON));
    }
}
