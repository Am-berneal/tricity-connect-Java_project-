package com.tricityconnect.repository;

import com.tricityconnect.model.Route;
import com.tricityconnect.model.Trip;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByRouteAndDepartureTimeBetweenOrderByDepartureTime(Route route,
                                                                     LocalDateTime from,
                                                                     LocalDateTime to);

    List<Trip> findByDepartureTimeBetweenOrderByDepartureTime(LocalDateTime from, LocalDateTime to);

    List<Trip> findByDisruption(com.tricityconnect.model.Disruption disruption);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Trip t set t.terminatesAtSeq = t.terminatesAtSeq + :delta " +
           "where t.route.id = :routeId and t.terminatesAtSeq is not null and t.terminatesAtSeq >= :fromSeq")
    int shiftTerminateSeq(@Param("routeId") Long routeId, @Param("fromSeq") int fromSeq, @Param("delta") int delta);

    /**
     * Locks the trip row (SELECT ... FOR UPDATE) while a seat is being booked, so two
     * passengers clicking "Pay" at the same moment cannot both get the same seat.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trip t where t.id = :id")
    Optional<Trip> lockById(@Param("id") Long id);
}
