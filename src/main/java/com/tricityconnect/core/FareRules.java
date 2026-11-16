package com.tricityconnect.core;

import java.math.BigDecimal;

/**
 * The numbers that decide every fare. Admins change them on the Fare Rules page
 * (stored in the fare_settings table); DEFAULT is used until then.
 */
public record FareRules(BigDecimal ratePerKm, BigDecimal peakMultiplier, BigDecimal offPeakMultiplier,
                        BigDecimal minFare, BigDecimal gstPercent, int studentDiscountPct, int seniorDiscountPct,
                        int pass1MonthPct, int pass3MonthPct, int pass6MonthPct) {

    public static final FareRules DEFAULT = new FareRules(new BigDecimal("2.00"), new BigDecimal("1.2"),
            BigDecimal.ONE, BigDecimal.TEN, new BigDecimal("5"), 50, 50, 25, 30, 35);

    public int discountFor(Concession c) {
        if (c == null) {
            return 0;
        }
        return switch (c) {
            case STUDENT -> studentDiscountPct;
            case SENIOR -> seniorDiscountPct;
            default -> 0;
        };
    }

    public int passDiscountFor(int months) {
        return switch (months) {
            case 1 -> pass1MonthPct;
            case 3 -> pass3MonthPct;
            case 6 -> pass6MonthPct;
            default -> throw new IllegalArgumentException("Pass duration must be 1, 3 or 6 months");
        };
    }
}
