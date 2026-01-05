package com.vozsegura.vozsegura.repo;

import com.vozsegura.vozsegura.domain.entity.StaffUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffUserRepository extends JpaRepository<StaffUser, Long> {

    Optional<StaffUser> findByUsernameAndEnabledTrue(String username);

    Optional<StaffUser> findByCedulaAndEnabledTrue(String cedula);
}
