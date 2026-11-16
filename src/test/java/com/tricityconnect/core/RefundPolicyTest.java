package com.tricityconnect.core;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RefundPolicyTest {

    private static final BigDecimal PAID = new BigDecimal("45.00");

    @Test
    void ninetyPercentTwoHoursOrMoreBefore() {
        assertEquals(new BigDecimal("40.50"), RefundPolicy.refundAmount(PAID, 120));
        assertEquals(new BigDecimal("40.50"), RefundPolicy.refundAmount(PAID, 600));
    }

    @Test
    void fiftyPercentBetweenThirtyMinutesAndTwoHours() {
        assertEquals(new BigDecimal("22.50"), RefundPolicy.refundAmount(PAID, 30));
        assertEquals(new BigDecimal("22.50"), RefundPolicy.refundAmount(PAID, 119));
    }

    @Test
    void nothingInTheLastThirtyMinutes() {
        assertEquals(new BigDecimal("0.00"), RefundPolicy.refundAmount(PAID, 29));
        assertEquals(new BigDecimal("0.00"), RefundPolicy.refundAmount(PAID, -5));
    }
}
