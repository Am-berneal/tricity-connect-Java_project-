package com.tricityconnect.dto;

import com.tricityconnect.core.FareBreakdown;
import com.tricityconnect.model.Route;
import com.tricityconnect.model.Trip;

/** A busy-route card on the home page. */
public record CorridorCard(Route route, Trip nextTrip, int seatsLeft, FareBreakdown fare) {
}
