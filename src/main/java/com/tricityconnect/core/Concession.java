package com.tricityconnect.core;

/** Passenger category. Students and senior citizens get a discount (set on the Fare Rules page). */
public enum Concession {
    GENERAL("General"), STUDENT("Student"), SENIOR("Senior Citizen");

    private final String label;

    Concession(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
