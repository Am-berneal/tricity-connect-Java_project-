package com.tricityconnect.core;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PassPricingTest {

    private static final BigDecimal FARE = new BigDecimal("45");

    @Test
    void oneMonthIsTwentyFivePercentOff() {
        // 45 x 2 trips x 22 days = 1980; 25% off = 1485 -> nearest 10 = 1490
        assertEquals(new BigDecimal("1980"), PassPricing.fullPrice(FARE, 1));
        assertEquals(new BigDecimal("1490.00"), PassPricing.price(FARE, 1, false));
    }

    @Test
    void longerPassesSaveMore() {
        assertEquals(new BigDecimal("4160.00"), PassPricing.price(FARE, 3, false)); // 5940 x 0.70
        assertTrue(PassPricing.price(FARE, 6, false).compareTo(PassPricing.price(FARE, 3, false).multiply(BigDecimal.valueOf(2))) < 0);
    }

    @Test
    void concessionHalvesThePrice() {
        assertEquals(new BigDecimal("740.00"), PassPricing.price(FARE, 1, true));
    }

    @Test
    void onlyOneThreeOrSixMonths() {
        assertThrows(IllegalArgumentException.class, () -> PassPricing.price(FARE, 2, false));
    }
}
