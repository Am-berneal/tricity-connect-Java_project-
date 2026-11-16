package com.tricityconnect.repository;

import com.tricityconnect.model.Disruption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisruptionRepository extends JpaRepository<Disruption, Long> {

    List<Disruption> findByActiveTrueOrderByCreatedAtDesc();

    List<Disruption> findByActiveFalseOrderByResolvedAtDesc();

    long countByActiveTrue();

    boolean existsByCode(String code);
}
