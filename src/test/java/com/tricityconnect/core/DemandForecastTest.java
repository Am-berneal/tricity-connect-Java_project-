package com.tricityconnect.core;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DemandForecastTest {

    @Test
    void indexIsAPercentageAndHasALevel() {
        LocalDate d = LocalDate.of(2026, 11, 10);
        for (int i = 0; i < 60; i++) {
            int idx = DemandForecast.index(d.plusDays(i));
            assertTrue(idx >= 0 && idx <= 100);
            assertNotNull(DemandForecast.level(idx));
        }
    }

    @Test
    void weekdaysAreBusierThanSundays() {
        LocalDate monday = LocalDate.of(2026, 11, 16);
        LocalDate sunday = LocalDate.of(2026, 11, 22);
        assertTrue(DemandForecast.index(monday) > DemandForecast.index(sunday));
    }
}
