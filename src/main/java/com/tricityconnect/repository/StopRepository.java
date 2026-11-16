package com.tricityconnect.repository;

import com.tricityconnect.model.Stop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StopRepository extends JpaRepository<Stop, Long> {

    List<Stop> findAllByOrderByCityAscNameAsc();

    Optional<Stop> findByName(String name);
}
