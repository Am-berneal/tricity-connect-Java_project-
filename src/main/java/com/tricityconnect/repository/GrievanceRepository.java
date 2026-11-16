package com.tricityconnect.repository;

import com.tricityconnect.model.AppUser;
import com.tricityconnect.model.Grievance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrievanceRepository extends JpaRepository<Grievance, Long> {

    List<Grievance> findByUserOrderByCreatedAtDesc(AppUser user);

    boolean existsByTicketNo(String ticketNo);
}
