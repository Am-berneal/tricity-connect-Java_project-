package com.tricityconnect.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Stops: Kharar(1) Phase 7(2) Phase 3B2(3) Sector 43(4) Sector 17(5) */
class SeatSegmentsTest {

    @Test
    void backToBackJourneysDoNotOverlap() {
        // A gets off at Phase 7 where B gets on
        assertFalse(SeatSegments.overlaps(1, 2, 2, 5));
        assertFalse(SeatSegments.overlaps(2, 5, 1, 2));
    }

    @Test
    void sharedPartOfRouteOverlaps() {
        assertTrue(SeatSegments.overlaps(1, 3, 2, 5));
        assertTrue(SeatSegments.overlaps(1, 5, 2, 3));
        assertTrue(SeatSegments.overlaps(2, 4, 2, 4));
    }

    @Test
    void dropMustComeAfterBoarding() {
        assertThrows(IllegalArgumentException.class, () -> SeatSegments.overlaps(3, 3, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> SeatSegments.validate(4, 2));
    }
}
