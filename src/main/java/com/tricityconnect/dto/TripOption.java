package com.tricityconnect.dto;

import com.tricityconnect.core.FareBreakdown;
import com.tricityconnect.model.RouteStop;
import com.tricityconnect.model.Trip;

import java.time.LocalDateTime;

/** One bus shown in the search results, for the passenger's own boarding and drop stops. */
public record TripOption(Trip trip, RouteStop from, RouteStop to, FareBreakdown fare,
                         int seatsLeft, int ladiesFree, String freeAfterNote) {

    public LocalDateTime departAt() {
        return trip.expectedAt(from);
    }

    public LocalDateTime arriveAt() {
        return trip.expectedAt(to);
    }

    public int minutes() {
        return to.getMinutesFromStart() - from.getMinutesFromStart();
    }

    public int stopsBetween() {
        return to.getSeq() - from.getSeq() - 1;
    }

    public double km() {
        return to.getKmFromStart() - from.getKmFromStart();
    }

    public int capacity() {
        return trip.getBus().getCapacity();
    }

    public boolean full() {
        return seatsLeft == 0;
    }

    public int occupancyPercent() {
        return (int) Math.round(100.0 * (capacity() - seatsLeft) / capacity());
    }

    public boolean delayed() {
        return trip.getDelayMinutes() > 0;
    }
}
