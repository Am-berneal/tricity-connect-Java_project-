package com.tricityconnect.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One line in the passenger's transaction history (shown on Profile & Wallet).
 * amount is negative for payments and positive for refunds / top-ups.
 * walletImpact = true when the TriCity Wallet balance actually changed.
 */
@Entity
@Table(name = "wallet_transactions", indexes = @Index(name = "idx_wallet_user", columnList = "user_id"))
public class WalletTransaction {

    public enum Kind { TOPUP, BOOKING, REFUND, DISRUPTION_REFUND, PASS }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Kind kind;

    @Column(nullable = false, length = 160)
    private String description;

    @Column(length = 30)
    private String reference;

    @Column(length = 12)
    private String routeCode;

    @Column(length = 40)
    private String method;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    private boolean walletImpact;

    private LocalDateTime createdAt = LocalDateTime.now();

    public WalletTransaction() {
    }

    public WalletTransaction(AppUser user, Kind kind, String description, String reference, String routeCode,
                             String method, BigDecimal amount, boolean walletImpact) {
        this.user = user;
        this.kind = kind;
        this.description = description;
        this.reference = reference;
        this.routeCode = routeCode;
        this.method = method;
        this.amount = amount;
        this.walletImpact = walletImpact;
    }

    public boolean isCredit() {
        return amount.signum() > 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public Kind getKind() { return kind; }
    public void setKind(Kind kind) { this.kind = kind; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getRouteCode() { return routeCode; }
    public void setRouteCode(String routeCode) { this.routeCode = routeCode; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public boolean isWalletImpact() { return walletImpact; }
    public void setWalletImpact(boolean walletImpact) { this.walletImpact = walletImpact; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
