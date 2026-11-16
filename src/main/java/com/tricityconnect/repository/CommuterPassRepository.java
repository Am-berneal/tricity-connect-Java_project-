package com.tricityconnect.repository;

import com.tricityconnect.model.AppUser;
import com.tricityconnect.model.CommuterPass;
import com.tricityconnect.model.PassStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CommuterPassRepository extends JpaRepository<CommuterPass, Long> {

    List<CommuterPass> findByUserOrderByValidFromDesc(AppUser user);

    Optional<CommuterPass> findByPassNo(String passNo);

    boolean existsByPassNo(String passNo);

    @Query("select count(p) from CommuterPass p where p.status = :status and p.validTo >= :date")
    long countActive(@Param("status") PassStatus status, @Param("date") LocalDate date);

    /** Active passes valid on a date for a route in either direction. */
    @Query("select p from CommuterPass p join p.route r left join r.returnRoute rr " +
           "where p.status = :status and :date between p.validFrom and p.validTo " +
           "and (r.id = :routeId or rr.id = :routeId)")
    List<CommuterPass> findValidForRoute(@Param("routeId") Long routeId,
                                         @Param("date") LocalDate date,
                                         @Param("status") PassStatus status);
}
