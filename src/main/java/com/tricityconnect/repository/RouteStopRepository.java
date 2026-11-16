package com.tricityconnect.repository;

import com.tricityconnect.model.RouteStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {

    List<RouteStop> findByStopId(Long stopId);

    /**
     * Boarding stops on every route that serves the journey from one stop to another
     * in the right direction (boarding stop comes before the drop stop).
     */
    @Query("select a from RouteStop a, RouteStop b " +
           "where a.route = b.route and a.stop.id = :fromStopId and b.stop.id = :toStopId and a.seq < b.seq")
    List<RouteStop> findBoardingStopsServing(@Param("fromStopId") Long fromStopId,
                                             @Param("toStopId") Long toStopId);
}
