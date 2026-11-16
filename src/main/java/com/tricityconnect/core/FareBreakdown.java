package com.tricityconnect.core;

import java.math.BigDecimal;

/** Result of a fare calculation. total = base + cgst + sgst. */
public record FareBreakdown(BigDecimal base, BigDecimal cgst, BigDecimal sgst, BigDecimal total, boolean peak) {
}
