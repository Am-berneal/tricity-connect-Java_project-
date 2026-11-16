package com.tricityconnect.core;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cancellation refund rules (shown to passengers in the cancel dialog):
 *  - 2 hours or more before departure : 90% refund
 *  - 30 minutes to 2 hours            : 50% refund
 *  - less than 30 minutes / departed  : no refund
 */
public final class RefundPolicy {

    private RefundPolicy() {
    }

    public static int refundPercent(long minutesBeforeDeparture) {
        if (minutesBeforeDeparture >= 120) {
            return 90;
        }
        if (minutesBeforeDeparture >= 30) {
            return 50;
        }
        return 0;
    }

    public static BigDecimal refundAmount(BigDecimal paid, long minutesBeforeDeparture) {
        int percent = refundPercent(minutesBeforeDeparture);
        return paid.multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
