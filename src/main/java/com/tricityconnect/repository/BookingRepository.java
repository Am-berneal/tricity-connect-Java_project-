package com.tricityconnect.repository;

import com.tricityconnect.model.AppUser;
import com.tricityconnect.model.Booking;
import com.tricityconnect.model.BookingStatus;
import com.tricityconnect.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByTripAndStatus(Trip trip, BookingStatus status);

    List<Booking> findByTripInAndStatus(Collection<Trip> trips, BookingStatus status);

    List<Booking> findByUserOrderByCreatedAtDesc(AppUser user);

    Optional<Booking> findByPnr(String pnr);

    boolean existsByPnr(String pnr);

    @Query("select b from Booking b where b.trip.departureTime between :from and :to")
    List<Booking> findByTripDepartureBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select count(b) from Booking b where b.trip.route.id = :routeId and (b.fromSeq = :seq or b.toSeq = :seq)")
    long countUsingStop(@Param("routeId") Long routeId, @Param("seq") int seq);

    // When an admin inserts or removes a stop, stop numbers after it move by +1 / -1.
    // Bookings keep pointing at the same physical stops by shifting their numbers too.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Booking b set b.fromSeq = b.fromSeq + :delta where b.fromSeq >= :fromSeq " +
           "and b.trip.id in (select t.id from Trip t where t.route.id = :routeId)")
    int shiftFromSeq(@Param("routeId") Long routeId, @Param("fromSeq") int fromSeq, @Param("delta") int delta);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Booking b set b.toSeq = b.toSeq + :delta where b.toSeq >= :fromSeq " +
           "and b.trip.id in (select t.id from Trip t where t.route.id = :routeId)")
    int shiftToSeq(@Param("routeId") Long routeId, @Param("fromSeq") int fromSeq, @Param("delta") int delta);
}
