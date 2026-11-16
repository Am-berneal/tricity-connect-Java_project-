package com.tricityconnect.dto;

import com.tricityconnect.model.Gender;
import com.tricityconnect.model.PaymentMethod;
import jakarta.validation.constraints.*;

public class CheckoutForm {

    @NotNull
    private Long tripId;

    @NotBlank
    private String seatNo;

    private int fromSeq;
    private int toSeq;

    @NotBlank(message = "Please enter the passenger's name")
    @Size(max = 100)
    private String passengerName;

    @Min(value = 1, message = "Enter a valid age")
    @Max(value = 120, message = "Enter a valid age")
    private int passengerAge;

    @NotNull(message = "Please select gender")
    private Gender passengerGender;

    @NotBlank(message = "Please enter a mobile number")
    @Pattern(regexp = "[6-9]\\d{9}", message = "Enter a valid 10-digit mobile number")
    private String passengerMobile;

    @Email(message = "Enter a valid email address")
    private String passengerEmail;

    private boolean whatsappTicket = true;

    private boolean womenSafety;

    @Pattern(regexp = "^$|[6-9]\\d{9}", message = "Enter a valid 10-digit emergency contact")
    private String emergencyContact;

    private com.tricityconnect.core.Concession concession = com.tricityconnect.core.Concession.GENERAL;

    @NotNull(message = "Please choose a payment method")
    private PaymentMethod paymentMethod = PaymentMethod.UPI;

    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public String getSeatNo() { return seatNo; }
    public void setSeatNo(String seatNo) { this.seatNo = seatNo; }
    public int getFromSeq() { return fromSeq; }
    public void setFromSeq(int fromSeq) { this.fromSeq = fromSeq; }
    public int getToSeq() { return toSeq; }
    public void setToSeq(int toSeq) { this.toSeq = toSeq; }
    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }
    public int getPassengerAge() { return passengerAge; }
    public void setPassengerAge(int passengerAge) { this.passengerAge = passengerAge; }
    public Gender getPassengerGender() { return passengerGender; }
    public void setPassengerGender(Gender passengerGender) { this.passengerGender = passengerGender; }
    public String getPassengerMobile() { return passengerMobile; }
    public void setPassengerMobile(String passengerMobile) { this.passengerMobile = passengerMobile; }
    public String getPassengerEmail() { return passengerEmail; }
    public void setPassengerEmail(String passengerEmail) { this.passengerEmail = passengerEmail; }
    public boolean isWhatsappTicket() { return whatsappTicket; }
    public void setWhatsappTicket(boolean whatsappTicket) { this.whatsappTicket = whatsappTicket; }
    public boolean isWomenSafety() { return womenSafety; }
    public void setWomenSafety(boolean womenSafety) { this.womenSafety = womenSafety; }
    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
    public com.tricityconnect.core.Concession getConcession() { return concession; }
    public void setConcession(com.tricityconnect.core.Concession concession) { this.concession = concession; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
}
