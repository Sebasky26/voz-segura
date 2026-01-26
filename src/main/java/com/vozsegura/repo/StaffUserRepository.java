package com.vozsegura.repo;

import com.vozsegura.domain.entity.StaffUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para StaffUser.
 *
 * Tabla: staff.staff_user
 *
 * Gestiona usuarios internos del sistema (analistas y administradores).
 *
 * Autenticacion:
 * - username (citext, case-insensitive) es el identificador unico
 * - password_hash contiene BCrypt hash
 * - MFA obligatorio via email cifrado
 *
 * Seguridad:
 * - Email, phone y mfa_secret se almacenan cifrados
 * - No se persiste cedula ni PII sin cifrado
 * - Busquedas por username son case-insensitive (citext)
 *
 * @author Voz Segura Team
 * @since 2026-01
 */
@Repository
public interface StaffUserRepository extends JpaRepository<StaffUser, Long> {

    /**
     * Busca usuario staff por username habilitado.
     * Username es citext (case-insensitive).
     *
     * @param username Nombre de usuario
     * @return Optional con StaffUser si existe y esta habilitado
     */
    Optional<StaffUser> findByUsernameAndEnabledTrue(String username);

    /**
     * Busca usuario staff por username sin importar si está habilitado.
     * Usado para validar duplicados al crear usuarios.
     *
     * @param username Nombre de usuario
     * @return Optional con StaffUser si existe
     */
    Optional<StaffUser> findByUsername(String username);

    /**
     * Busca todos los usuarios staff con un rol especifico.
     *
     * @param role Rol (ADMIN o ANALYST)
     * @return Lista de usuarios con ese rol
     */
    List<StaffUser> findByRoleAndEnabledTrue(String role);

    /**
     * Busca todos los usuarios staff habilitados.
     *
     * @return Lista de usuarios habilitados
     */
    List<StaffUser> findByEnabledTrue();

    /**
     * Busca usuario staff por hash de cédula.
     * Utilizado para enlazar verificación Didit con staff sin exponer cédula.
     *
     * @param cedulaHashIdx Hash SHA-256 de la cédula
     * @return Optional con StaffUser si existe
     */
    Optional<StaffUser> findByCedulaHashIdx(String cedulaHashIdx);
}
