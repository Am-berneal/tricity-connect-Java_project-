package com.tricityconnect.core;

/**
 * Stop-wise seat booking.
 *
 * Stops on a route are numbered in order (seq 1, 2, 3 ...). A booking holds a seat
 * only from its boarding stop (fromSeq) up to its drop stop (toSeq). The seat is free
 * again from toSeq onwards, so another passenger can book it for a later part of the route.
 *
 * Two bookings of the same seat clash only if their stop ranges overlap:
 *     newFrom &lt; oldTo  AND  oldFrom &lt; newTo
 *
 * Example on Kharar(1) - Phase 7(2) - Phase 3B2(3) - Sector 43(4) - Sector 17(5):
 *     A books seat 4A from 1 to 2  (Kharar to Phase 7)
 *     B books seat 4A from 2 to 5  (Phase 7 to Sector 17)  -> allowed, no overlap
 *     C books seat 4A from 1 to 3                          -> rejected, overlaps A and B
 */
public final class SeatSegments {

    private SeatSegments() {
    }

    public static boolean overlaps(int fromA, int toA, int fromB, int toB) {
        validate(fromA, toA);
        validate(fromB, toB);
        return fromA < toB && fromB < toA;
    }

    public static void validate(int fromSeq, int toSeq) {
        if (fromSeq >= toSeq) {
            throw new IllegalArgumentException("Drop stop must come after boarding stop");
        }
    }
}
