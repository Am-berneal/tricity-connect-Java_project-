package com.tricityconnect.repository;

import com.tricityconnect.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {

    Optional<Route> findByCode(String code);

    /** Morning-direction corridors only (return directions end with "R"). */
    @Query("select r from Route r where r.code not like '%R' order by r.code")
    List<Route> findMainCorridors();

    List<Route> findAllByOrderByCodeAsc();
}
