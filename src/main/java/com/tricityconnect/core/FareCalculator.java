package com.tricityconnect.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;

/**
 * Calculates the fare for one passenger between two stops.
 *
 * Rules (default values; admins can change them on the Fare Rules page):
 *  - Rs 2.00 per km travelled between the passenger's own boarding and drop stops.
 *  - Peak hours (07:00-10:00 and 17:00-20:00) cost 1.2x; off-peak multiplier 1.0x.
 *  - Student / senior citizen discount (50% by default).
 *  - The final fare is rounded to the nearest Rs 5 (easy for conductors), minimum Rs 10.
 *  - The fare is GST-inclusive: 5% GST, split equally into CGST and SGST.
 *
 * Plain Java (no Spring) so it can be unit tested on its own.
 */
public final class FareCalculator {

    public static final BigDecimal RATE_PER_KM = FareRules.DEFAULT.ratePerKm();
    public static final BigDecimal PEAK_MULTIPLIER = FareRules.DEFAULT.peakMultiplier();
    public static final BigDecimal MIN_FARE = FareRules.DEFAULT.minFare();

    private FareCalculator() {
    }

    public static boolean isPeak(LocalTime time) {
        boolean morning = !time.isBefore(LocalTime.of(7, 0)) && time.isBefore(LocalTime.of(10, 0));
        boolean evening = !time.isBefore(LocalTime.of(17, 0)) && time.isBefore(LocalTime.of(20, 0));
        return morning || evening;
    }

    /** Fare with the default rules and no concession. */
    public static FareBreakdown calculate(double distanceKm, LocalTime departure) {
        return calculate(distanceKm, departure, FareRules.DEFAULT, 0);
    }

    public static FareBreakdown calculate(double distanceKm, LocalTime departure, FareRules rules, int discountPct) {
        if (distanceKm <= 0) {
            throw new IllegalArgumentException("Distance must be positive");
        }
        BigDecimal raw = rules.ratePerKm().multiply(BigDecimal.valueOf(distanceKm));
        boolean peak = isPeak(departure);
        raw = raw.multiply(peak ? rules.peakMultiplier() : rules.offPeakMultiplier());
        if (discountPct > 0) {
            raw = raw.multiply(BigDecimal.valueOf(100 - discountPct)).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        }
        // round to nearest 5 rupees
        BigDecimal total = raw.divide(BigDecimal.valueOf(5), 0, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(5));
        if (total.compareTo(rules.minFare()) < 0) {
            total = rules.minFare();
        }
        total = total.setScale(2, RoundingMode.HALF_UP);

        // total = base * (1 + gst%)  ->  base = total / (1 + gst%)
        BigDecimal gstRate = rules.gstPercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal base = total.divide(BigDecimal.ONE.add(gstRate), 2, RoundingMode.HALF_UP);
        BigDecimal gst = total.subtract(base);
        BigDecimal cgst = gst.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal sgst = gst.subtract(cgst);
        return new FareBreakdown(base, cgst, sgst, total, peak);
    }
}
