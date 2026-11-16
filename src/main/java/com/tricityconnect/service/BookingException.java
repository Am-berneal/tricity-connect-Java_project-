package com.tricityconnect.service;

/** A booking rule was broken (seat taken, ladies seat, low wallet balance ...). The message is shown to the user. */
public class BookingException extends RuntimeException {

    public BookingException(String message) {
        super(message);
    }
}
