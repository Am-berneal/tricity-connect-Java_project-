package com.tricityconnect.model;

public enum BusType {
    AC_ELECTRIC("100% AC Electric"), AC_CNG("AC CNG Express"), NON_AC("Non-AC Ordinary");

    private final String label;

    BusType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
