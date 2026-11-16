package com.tricityconnect.core;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Monthly commuter pass price.
 *  - A pass covers 2 trips (morning + evening) on 22 working days a month.
 *  - Discount (default): 25% for 1 month, 30% for 3 months, 35% for 6 months.
 *  - Students and senior citizens get their concession on top (50% by default).
 *  - Rounded to the nearest Rs 10.
 */
public final class PassPricing {

    public static final int WORKING_DAYS = 22;

    private PassPricing() {
    }

    public static int discountPercent(int months) {
        return FareRules.DEFAULT.passDiscountFor(months);
    }

    /** Price of buying the same trips as daily tickets. */
    public static BigDecimal fullPrice(BigDecimal singleFare, int months) {
        return singleFare.multiply(BigDecimal.valueOf(2L * WORKING_DAYS * months));
    }

    public static BigDecimal price(BigDecimal singleFare, int months, boolean concession) {
        return price(singleFare, months, concession, FareRules.DEFAULT);
    }

    public static BigDecimal price(BigDecimal singleFare, int months, boolean concession, FareRules rules) {
        BigDecimal full = fullPrice(singleFare, months);
        BigDecimal afterDiscount = full.multiply(BigDecimal.valueOf(100 - rules.passDiscountFor(months)))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (concession) {
            afterDiscount = afterDiscount.multiply(BigDecimal.valueOf(100 - rules.studentDiscountPct()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return afterDiscount.divide(BigDecimal.TEN, 0, RoundingMode.HALF_UP)
                .multiply(BigDecimal.TEN).setScale(2, RoundingMode.HALF_UP);
    }
}
