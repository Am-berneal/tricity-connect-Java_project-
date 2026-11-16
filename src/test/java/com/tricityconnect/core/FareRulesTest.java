package com.tricityconnect.core;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class FareRulesTest {

    @Test
    void defaultsGiveConcessionsAndPassDiscounts() {
        FareRules r = FareRules.DEFAULT;
        assertEquals(0, r.discountFor(Concession.GENERAL));
        assertEquals(50, r.discountFor(Concession.STUDENT));
        assertEquals(25, r.passDiscountFor(1));
        assertEquals(30, r.passDiscountFor(3));
        assertEquals(35, r.passDiscountFor(6));
    }

    @Test
    void studentPaysAboutHalf() {
        FareRules r = FareRules.DEFAULT;
        BigDecimal general = FareCalculator.calculate(19, LocalTime.of(12, 0), r, 0).total();
        BigDecimal student = FareCalculator.calculate(19, LocalTime.of(12, 0), r, 50).total();
        assertTrue(student.compareTo(general) < 0);
        assertTrue(student.compareTo(BigDecimal.TEN) >= 0, "Minimum fare still applies");
    }
}
