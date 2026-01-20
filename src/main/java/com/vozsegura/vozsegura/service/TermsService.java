package com.vozsegura.vozsegura.service;

import com.vozsegura.vozsegura.domain.entity.TermsAcceptance;
import com.vozsegura.vozsegura.repo.TermsAcceptanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Servicio para gestión y auditoría de aceptación de términos y condiciones.
 * 
 * Propósito:
 * - Registrar que usuario aceptó términos (compliance legal)
 * - Vinculación anónima: solo guarda token de sesión (NO datos personales)
 * - Auditoría: fecha/hora de aceptación
 * - NUNCA vincular término aceptado con cédula directamente
 * 
 * Flujo de uso:
 * 1. Usuario accede a página de términos (inicio de denuncia)
 * 2. UI genera sessionToken único (UUID)
 * 3. Usuario lee y acepta términos
 * 4. Frontend llama: recordAcceptance() → guarda sessionToken + timestamp
 * 5. sessionToken pasa a siguiente paso (OTP, etc.)
 * 6. En final de denuncia: registrar si tuvo términos aceptados previamente
 * 
 * Nota de seguridad:
 * - Tabla TermsAcceptance tiene SOLO: sessionToken, acceptedAt
 * - NO: cédula, IP, navegador (eso va en AuditLog)
 * - Linking: sessionToken es el puente (anonimidad)
 * 
 * @author Voz Segura Team
 * @since 2026-01
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

