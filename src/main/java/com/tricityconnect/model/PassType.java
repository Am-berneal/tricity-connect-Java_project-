package com.tricityconnect.model;

public enum PassType {
    CORRIDOR("Corridor Pass"), STUDENT_SENIOR("Student / Senior Citizen Pass");

    private final String label;

    PassType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
