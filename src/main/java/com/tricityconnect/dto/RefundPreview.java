package com.tricityconnect.dto;

import java.math.BigDecimal;

/** What the passenger would get back if they cancelled now. */
public record RefundPreview(int percent, BigDecimal amount, long minutesLeft, boolean cancellable) {
}
