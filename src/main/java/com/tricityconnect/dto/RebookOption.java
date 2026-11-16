package com.tricityconnect.dto;

import com.tricityconnect.model.Trip;

import java.time.LocalDateTime;

/** A later bus a disrupted passenger can move to. */
public record RebookOption(Trip trip, LocalDateTime boardingTime, int seatsFree) {
}
