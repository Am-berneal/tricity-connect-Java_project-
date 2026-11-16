package com.tricityconnect.dto;

import java.time.LocalDateTime;

/**
 * A stop on the live-tracking timeline. state is "passed", "next" or "upcoming".
 * x, y are the stop's position on the schematic SVG map (800 x 420).
 */
public record TrackStop(String name, String city, LocalDateTime expectedAt, String state,
                        boolean boarding, boolean drop, int positionPercent, int x, int y) {
}
