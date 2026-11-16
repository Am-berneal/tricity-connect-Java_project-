package com.tricityconnect.repository;

import com.tricityconnect.model.Bus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusRepository extends JpaRepository<Bus, Long> {

    java.util.List<Bus> findAllByOrderByRegNoAsc();

    boolean existsByRegNoIgnoreCase(String regNo);
}
