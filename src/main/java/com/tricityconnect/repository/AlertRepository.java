package com.tricityconnect.repository;

import com.tricityconnect.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByActiveTrueOrderByPriorityAscCreatedAtDesc();

    List<Alert> findByActiveTrueAndRouteCodeIn(List<String> routeCodes);
}
