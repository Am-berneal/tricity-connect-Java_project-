package com.tricityconnect.dto;

import com.tricityconnect.model.Trip;

/**
 * How full a trip is. peakLoad = most seats occupied at the same time on any part of the route
 * (stop-wise bookings + pass holders), so it is the real load factor.
 */
public record TripLoad(Trip trip, int peakLoad, int bookings, int passHolders, int boarded) {

    public int capacity() {
        return trip.getBus().getCapacity();
    }

    public int percent() {
        return (int) Math.round(100.0 * peakLoad / capacity());
    }
}
