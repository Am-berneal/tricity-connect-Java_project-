package com.tricityconnect.dto;

/** One seat on the seat-selection map. */
public record SeatView(String label, int row, String column, boolean ladies, boolean window, SeatState state) {

    public enum SeatState {
        AVAILABLE, BOOKED, PASS_RESERVED, LADIES_LOCKED
    }

    public boolean available() {
        return state == SeatState.AVAILABLE;
    }
}
