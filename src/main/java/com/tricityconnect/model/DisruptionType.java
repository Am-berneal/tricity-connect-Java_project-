package com.tricityconnect.model;

public enum DisruptionType {
    SUSPENDED("Service Suspended"),
    SHORT_TERMINATED("Short-Terminated"),
    DELAYED("Delayed Corridor"),
    DIVERTED("Route Diverted");

    private final String label;

    DisruptionType(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
