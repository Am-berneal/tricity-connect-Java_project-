package com.tricityconnect.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A registered user: passenger, conductor or admin. */
@Entity
@Table(name = "users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 10)
    private String mobile;

    @Column(unique = true, length = 120)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Column(length = 60)
    private String homeCity;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Role role = Role.PASSENGER;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal walletBalance = BigDecimal.ZERO;

    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(length = 100)
    private String emergencyName;

    @Column(length = 10)
    private String emergencyMobile;

    public AppUser() {
    }

    public AppUser(String fullName, String mobile, String email, Gender gender, String homeCity,
                   String passwordHash, Role role) {
        this.fullName = fullName;
        this.mobile = mobile;
        this.email = email;
        this.gender = gender;
        this.homeCity = homeCity;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public String getFirstName() {
        return fullName.contains(" ") ? fullName.substring(0, fullName.indexOf(' ')) : fullName;
    }

    public String getInitials() {
        String[] parts = fullName.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public BigDecimal getWalletBalance() { return walletBalance; }
    public void setWalletBalance(BigDecimal walletBalance) { this.walletBalance = walletBalance; }
    public String getEmergencyName() { return emergencyName; }
    public void setEmergencyName(String emergencyName) { this.emergencyName = emergencyName; }
    public String getEmergencyMobile() { return emergencyMobile; }
    public void setEmergencyMobile(String emergencyMobile) { this.emergencyMobile = emergencyMobile; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
