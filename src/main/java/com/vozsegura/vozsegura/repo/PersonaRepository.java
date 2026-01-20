package com.vozsegura.vozsegura.repo;

import com.vozsegura.vozsegura.domain.entity.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Spring Data JPA repository para entidad Persona.
 * 
 * Gestiona el registro de ciudadanos verificados contra Registro Civil.
 * 
 * Seguridad - CRÍTICO:
 * - findByCedula: SOLO para búsqueda inicial/verificación (NUNCA se almacena plain text después)
 * - findByCedulaHash: Métodos principales (SHA-256 irreversible, anónimo)
 * - La cédula plain text se descarta inmediatamente después de ser hasheada
 * - Los hashes SHA-256 permiten verificación sin exposición de identidad
 * 
 * Flujo:
 * 1. Usuario ingresa cédula (plain text)
 * 2. Sistema verifica con Registro Civil
 * 3. Si válido, calcula SHA-256 hash
 * 4. Busca por cedulaHash (nunca plain)
 * 5. Cédula plain se descarta de memoria
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
public interface PersonaRepository extends JpaRepository<Persona, Long> {

    /**
     * Busca una persona por su número de cédula en plain text.
     * 
     * ⚠️ ADVERTENCIA: Solo usar durante verificación inicial.
     * Nunca almacenar/retornar cédula plain text después de esto.
     * 
     * @param cedula Número de cédula (plain text)
     * @return Optional con Persona si existe
     */
    Optional<Persona> findByCedula(String cedula);

    /**
     * Busca una persona por el hash SHA-256 de su cédula.
     * 
     * Este es el método PRINCIPAL para búsquedas normales (después de verificación).
     * El hash es unidireccional: imposible recuperar cédula original.
     * 
     * @param cedulaHash SHA-256 hash de la cédula (hex string)
     * @return Optional con Persona si existe
     */
    Optional<Persona> findByCedulaHash(String cedulaHash);
}
