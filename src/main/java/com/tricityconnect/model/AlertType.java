package com.tricityconnect.model;

public enum AlertType {
    DETOUR("High Priority Detour", "red"),
    SUSPENSION("Service Suspended", "red"),
    DELAY("Traffic / Delay", "amber"),
    FESTIVAL("Festive Service Surge", "orange"),
    NEW_ROUTE("New Corridor Inaugurated", "green"),
    PASS_OFFER("Commuter Reward", "blue"),
    SYSTEM("Scheduled Maintenance", "gray");

    private final String label;
    private final String color;

    AlertType(String label, String color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public String getColor() {
        return color;
    }
}
