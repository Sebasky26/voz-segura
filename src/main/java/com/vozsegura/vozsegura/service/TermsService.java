package com.vozsegura.vozsegura.service;

import com.vozsegura.vozsegura.domain.entity.TermsAcceptance;
import com.vozsegura.vozsegura.repo.TermsAcceptanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Servicio para gestión de aceptación de términos.
 * Registra la aceptación sin almacenar datos personales (solo token de sesión).
 */
@Service
public class TermsService {

    private final TermsAcceptanceRepository termsAcceptanceRepository;

    public TermsService(TermsAcceptanceRepository termsAcceptanceRepository) {
        this.termsAcceptanceRepository = termsAcceptanceRepository;
    }

    /**
     * Registra la aceptación de términos.
     *
     * @return token de sesión para vincular con el flujo
     */
    @Transactional
    public String recordAcceptance() {
        String sessionToken = UUID.randomUUID().toString();

        TermsAcceptance acceptance = new TermsAcceptance();
        acceptance.setSessionToken(sessionToken);
        acceptance.setAcceptedAt(OffsetDateTime.now());
        termsAcceptanceRepository.save(acceptance);

        return sessionToken;
    }

    /**
     * Verifica si un token de sesión tiene términos aceptados.
     */
    public boolean hasAccepted(String sessionToken) {
        return termsAcceptanceRepository.findBySessionToken(sessionToken).isPresent();
    }
}

