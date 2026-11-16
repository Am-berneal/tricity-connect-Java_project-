package com.tricityconnect.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Seat numbering for a 2+2 city express bus: rows 1..N, columns A B | aisle | C D.
 * Rows 1 and 2 (front, near the door) are reserved for women.
 */
public final class SeatLayout {

    public static final String[] COLUMNS = {"A", "B", "C", "D"};
    public static final int LADIES_ROWS = 2;

    private SeatLayout() {
    }

    public static List<String> seatLabels(int capacity) {
        if (capacity <= 0 || capacity % COLUMNS.length != 0) {
            throw new IllegalArgumentException("Capacity must be a positive multiple of 4");
        }
        List<String> labels = new ArrayList<>();
        int rows = capacity / COLUMNS.length;
        for (int row = 1; row <= rows; row++) {
            for (String col : COLUMNS) {
                labels.add(row + col);
            }
        }
        return labels;
    }

    public static int rowOf(String label) {
        return Integer.parseInt(label.substring(0, label.length() - 1));
    }

    public static String columnOf(String label) {
        return label.substring(label.length() - 1);
    }

    public static boolean isLadiesSeat(String label) {
        return rowOf(label) <= LADIES_ROWS;
    }

    public static boolean isWindow(String label) {
        String c = columnOf(label);
        return c.equals("A") || c.equals("D");
    }

    public static boolean isValid(String label, int capacity) {
        return label != null && seatLabels(capacity).contains(label);
    }
}
