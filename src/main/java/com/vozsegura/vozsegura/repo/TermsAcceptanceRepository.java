package com.vozsegura.vozsegura.repo;

import com.vozsegura.vozsegura.domain.entity.TermsAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TermsAcceptanceRepository extends JpaRepository<TermsAcceptance, Long> {

    Optional<TermsAcceptance> findBySessionToken(String sessionToken);
}
