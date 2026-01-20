package com.vozsegura.vozsegura.repo;

import com.vozsegura.vozsegura.domain.entity.StaffUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository para entidad StaffUser.
 * 
 * Gestiona usuarios internos (analistas, administradores).
 * 
 * Diferencia con Persona:
 * - Persona: Ciudadanos que crean denuncias (anónimos)
 * - StaffUser: Personal interno de Voz Segura (autenticados)
 * 
 * Autenticación:
 * - username + secretKey (BCrypt hash) → login inicial
 * - Luego MFA: OTP vía email
 * - Roles: ANALYST (revisa denuncias), ADMIN (administración)
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
public interface StaffUserRepository extends JpaRepository<StaffUser, Long> {

    /**
     * Busca un usuario staff por username que esté habilitado.
     * 
     * La condición enabled=true permite soft-delete (marcar como inactivo sin eliminar).
     * 
     * @param username Nombre de usuario único
     * @return Optional con StaffUser si existe y está habilitado
     */
    Optional<StaffUser> findByUsernameAndEnabledTrue(String username);

    /**
     * Busca un usuario staff por su número de cédula (habilitado).
     * 
     * Alternativa de búsqueda: algunos sistemas identifican staff por cédula.
     * 
     * @param cedula Número de cédula (diferente a Persona)
     * @return Optional con StaffUser si existe y está habilitado
     */
    Optional<StaffUser> findByCedulaAndEnabledTrue(String cedula);
}
