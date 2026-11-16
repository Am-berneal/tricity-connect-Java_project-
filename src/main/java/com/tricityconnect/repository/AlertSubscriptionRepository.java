package com.tricityconnect.repository;

import com.tricityconnect.model.AlertSubscription;
import com.tricityconnect.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertSubscriptionRepository extends JpaRepository<AlertSubscription, Long> {

    List<AlertSubscription> findByUser(AppUser user);
}
