package com.tricityconnect.service;

import com.tricityconnect.model.AppUser;
import com.tricityconnect.model.WalletTransaction;
import com.tricityconnect.model.WalletTransaction.Kind;
import com.tricityconnect.repository.UserRepository;
import com.tricityconnect.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** TriCity Wallet: balance changes and the transaction history shown on the profile page. */
@Service
public class WalletService {

    public static final BigDecimal MAX_TOPUP = new BigDecimal("5000");

    private final WalletTransactionRepository repository;
    private final UserRepository userRepository;

    public WalletService(WalletTransactionRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    /** Records a money event; if walletImpact, the balance changes by amount (negative = payment). */
    @Transactional
    public WalletTransaction record(AppUser user, Kind kind, String description, String reference,
                                    String routeCode, String method, BigDecimal amount, boolean walletImpact) {
        if (walletImpact) {
            BigDecimal newBalance = user.getWalletBalance().add(amount);
            if (newBalance.signum() < 0) {
                throw new BookingException("Your TriCity Wallet balance (₹" + user.getWalletBalance() + ") is not enough.");
            }
            user.setWalletBalance(newBalance);
        }
        return repository.save(new WalletTransaction(user, kind, description, reference, routeCode, method,
                amount, walletImpact));
    }

    @Transactional
    public AppUser topUp(AppUser user, BigDecimal amount, String method) {
        if (amount == null || amount.signum() <= 0 || amount.compareTo(MAX_TOPUP) > 0) {
            throw new BookingException("Top-up amount must be between ₹1 and ₹5,000.");
        }
        AppUser u = userRepository.findById(user.getId()).orElseThrow();
        record(u, Kind.TOPUP, "Wallet top-up", null, null, method, amount, true);
        return u;
    }

    @Transactional(readOnly = true)
    public List<WalletTransaction> history(AppUser user) {
        return repository.findByUserOrderByCreatedAtDesc(user);
    }
}
