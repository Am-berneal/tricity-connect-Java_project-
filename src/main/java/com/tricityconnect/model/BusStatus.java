package com.tricityconnect.model;

public enum BusStatus {
    ACTIVE("Active · On Route", "green"),
    CHARGING("Depot Charging", "amber"),
    STANDBY("Standby Ready", "blue"),
    MAINTENANCE("In Workshop", "red");

    private final String label;
    private final String color;

    BusStatus(String label, String color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() { return label; }
    public String getColor() { return color; }
}
