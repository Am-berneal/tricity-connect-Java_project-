package com.tricityconnect.model;

public enum PaymentMethod {
    UPI("UPI"), WALLET("TriCity Wallet"), CARD("Debit / Credit Card"), NET_BANKING("Net Banking");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
