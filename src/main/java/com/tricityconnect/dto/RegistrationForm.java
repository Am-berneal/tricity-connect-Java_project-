package com.tricityconnect.dto;

import com.tricityconnect.model.Gender;
import jakarta.validation.constraints.*;

public class RegistrationForm {

    @NotBlank(message = "Please enter your full name")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Please enter your mobile number")
    @Pattern(regexp = "[6-9]\\d{9}", message = "Enter a valid 10-digit Indian mobile number")
    private String mobile;

    @Email(message = "Enter a valid email address")
    private String email;

    @NotNull(message = "Please select your gender")
    private Gender gender;

    @NotBlank(message = "Please choose your home city")
    private String homeCity;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;

    @AssertTrue(message = "Please accept the terms to continue")
    private boolean acceptTerms;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
    public String getHomeCity() { return homeCity; }
    public void setHomeCity(String homeCity) { this.homeCity = homeCity; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    public boolean isAcceptTerms() { return acceptTerms; }
    public void setAcceptTerms(boolean acceptTerms) { this.acceptTerms = acceptTerms; }
}
