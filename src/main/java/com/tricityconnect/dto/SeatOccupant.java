package com.tricityconnect.dto;

import com.tricityconnect.model.Booking;
import com.tricityconnect.model.CommuterPass;

/** One seat on the conductor's floor map for the current stretch of road. */
public record SeatOccupant(String label, boolean ladies, Booking booking, CommuterPass pass,
                           boolean freeingNext, boolean boardingHere) {

    public boolean empty() {
        return booking == null && pass == null;
    }

    public String name() {
        if (booking != null) {
            String n = booking.getPassengerName();
            return n.contains(" ") ? n.substring(0, n.indexOf(' ')) : n;
        }
        if (pass != null) {
            return pass.getUser().getFirstName();
        }
        return "Empty";
    }
}
