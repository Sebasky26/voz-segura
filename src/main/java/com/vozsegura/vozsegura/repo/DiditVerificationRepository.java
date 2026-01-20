package com.vozsegura.vozsegura.repo;

import com.vozsegura.vozsegura.domain.entity.DiditVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DiditVerificationRepository extends JpaRepository<DiditVerification, Long> {

    Optional<DiditVerification> findByDiditSessionId(String diditSessionId);

    Optional<DiditVerification> findByDocumentNumber(String documentNumber);
    
    Optional<DiditVerification> findByIdRegistro(Long idRegistro);
}

