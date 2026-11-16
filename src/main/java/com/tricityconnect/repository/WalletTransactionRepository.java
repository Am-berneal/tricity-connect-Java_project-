package com.tricityconnect.repository;

import com.tricityconnect.model.AppUser;
import com.tricityconnect.model.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction> findByUserOrderByCreatedAtDesc(AppUser user);
}
