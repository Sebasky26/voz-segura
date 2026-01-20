package com.vozsegura.vozsegura.repo;

import com.vozsegura.vozsegura.domain.entity.TermsAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository para entidad TermsAcceptance.
 * 
 * Gestiona el registro de aceptación de términos y condiciones por ciudadanos.
 * 
 * Propósito Legal:
 * - Prueba de que ciudadano leyó y aceptó términos
 * - Registro de consentimiento para procesamiento de datos
 * - Compliance con regulaciones de privacidad (GDPR, leyes locales)
 * 
 * Flujo:
 * 1. Ciudadano carga formulario de acceso a denuncias
 * 2. Sistema valida: checkbox termsAccepted = true
 * 3. Se genera sessionToken (UUID)
 * 4. Se crea TermsAcceptance record con timestamp
 * 5. Ciudadano continúa flujo (autenticación, creación denuncia)
 * 
 * Cada sesión = un TermsAcceptance (auditoría de consentimiento)
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
public interface TermsAcceptanceRepository extends JpaRepository<TermsAcceptance, Long> {

    /**
     * Busca un registro de aceptación de términos por su token de sesión.
     * 
     * El sessionToken es un UUID único generado en cada sesión.
     * Permite vincular aceptación de términos con acciones posteriores.
     * 
     * @param sessionToken UUID único de sesión
     * @return Optional con registro de aceptación si existe
     */
    Optional<TermsAcceptance> findBySessionToken(String sessionToken);
}
