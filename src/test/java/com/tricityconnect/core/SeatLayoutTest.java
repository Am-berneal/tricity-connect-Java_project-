package com.tricityconnect.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SeatLayoutTest {

    @Test
    void thirtySixSeatBusHasNineRows() {
        List<String> seats = SeatLayout.seatLabels(36);
        assertEquals(36, seats.size());
        assertEquals("1A", seats.get(0));
        assertEquals("9D", seats.get(35));
    }

    @Test
    void firstTwoRowsAreLadiesSeats() {
        assertTrue(SeatLayout.isLadiesSeat("1A"));
        assertTrue(SeatLayout.isLadiesSeat("2D"));
        assertFalse(SeatLayout.isLadiesSeat("3A"));
        assertFalse(SeatLayout.isLadiesSeat("12B"));
    }

    @Test
    void windowSeats() {
        assertTrue(SeatLayout.isWindow("4A"));
        assertTrue(SeatLayout.isWindow("4D"));
        assertFalse(SeatLayout.isWindow("4B"));
    }

    @Test
    void validSeats() {
        assertTrue(SeatLayout.isValid("5C", 36));
        assertFalse(SeatLayout.isValid("10A", 36));
        assertFalse(SeatLayout.isValid(null, 36));
    }
}
