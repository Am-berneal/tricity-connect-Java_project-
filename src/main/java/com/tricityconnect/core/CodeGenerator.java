package com.tricityconnect.core;

import java.security.SecureRandom;

/** Generates PNR numbers (TC7K29XQ) and pass numbers (TC-PASS-4821). */
public final class CodeGenerator {

    // no 0/O/1/I to avoid confusion when read aloud
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private CodeGenerator() {
    }

    public static String pnr() {
        StringBuilder sb = new StringBuilder("TC");
        for (int i = 0; i < 6; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public static String passNumber() {
        return "TC-PASS-" + (1000 + RANDOM.nextInt(9000));
    }
}
