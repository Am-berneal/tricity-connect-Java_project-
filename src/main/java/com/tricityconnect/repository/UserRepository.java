package com.tricityconnect.repository;

import com.tricityconnect.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByMobile(String mobile);

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByMobile(String mobile);

    boolean existsByEmailIgnoreCase(String email);
}
