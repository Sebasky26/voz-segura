package com.vozsegura.repo;

import com.vozsegura.domain.entity.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Spring Data JPA repository para entidad Persona.
 * 
 * Gestiona el registro de ciudadanos verificados contra Registro Civil.
 * 
 * SEGURIDAD CRÍTICA:
 * - findByCedulaHash: Método ÚNICO para búsquedas (SHA-256 irreversible, anónimo)
 * - NUNCA almacena cédula en texto plano
 * - El hash es unidireccional: imposible recuperar cédula original
 *
 * @author Voz Segura Team
 * @since 2026-01
 */
public interface PersonaRepository extends JpaRepository<Persona, Long> {

    /**
     * Busca una persona por el hash SHA-256 de su cedula.
     *
     * Este es el metodo UNICO para busquedas por cedula.
     *
     * @param cedulaHash Hash SHA-256 de la cedula (hex string, 64 caracteres)
     * @return Optional con Persona si existe
     */
    Optional<Persona> findByCedulaHash(String cedulaHash);

    /**
     * Busca una persona por su identity_vault_id (relacion 1:1 con identity_vault).
     *
     * @param identityVaultId ID de la boveda de identidad
     * @return Optional con Persona si existe
     */
    Optional<Persona> findByIdentityVaultId(Long identityVaultId);

    /**
     * Busca una persona por el hash de su nombre completo.
     *
     * @param nombreCompletoHash Hash SHA-256 del nombre completo
     * @return Optional con Persona si existe
     */
    Optional<Persona> findByNombreCompletoHash(String nombreCompletoHash);
}
