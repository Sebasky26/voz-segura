package com.vozsegura.repo;

import com.vozsegura.domain.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para Complaint.
 *
 * Tabla: denuncias.denuncia
 *
 * Proporciona operaciones CRUD y busquedas especializadas para denuncias.
 *
 * Seguridad:
 * - Busquedas por tracking_id para seguimiento anonimo
 * - Busquedas por identity_vault_id para ownership (denunciante)
 * - Contenido siempre cifrado (encrypted_text, company_*_encrypted)
 *
 * @author Voz Segura Team
 * @since 2026-01
 */
@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    /**
     * Busca denuncia por tracking ID unico.
     *
     * @param trackingId Identificador publico de seguimiento
     * @return Optional con denuncia si existe
     */
    Optional<Complaint> findByTrackingId(String trackingId);

    /**
     * Busca denuncias por estado.
     *
     * @param status Estado (PENDING, IN_REVIEW, RESOLVED, DERIVED, REJECTED, NEEDS_INFO, ARCHIVED)
     * @return Lista de denuncias con ese estado
     */
    List<Complaint> findByStatus(String status);

    /**
     * Busca denuncias por identity vault ID (ownership).
     * Permite que un denunciante vea solo sus propias denuncias.
     *
     * @param identityVaultId ID de la boveda de identidad
     * @return Lista de denuncias de ese denunciante
     */
    List<Complaint> findByIdentityVaultIdOrderByCreatedAtDesc(Long identityVaultId);

    /**
     * Busca denuncias asignadas a un staff especifico.
     *
     * @param staffId ID del staff asignado
     * @return Lista de denuncias asignadas
     */
    List<Complaint> findByAssignedStaffIdOrderByCreatedAtDesc(Long staffId);

    /**
     * Obtiene denuncias recientes (mas recientes primero).
     *
     * @return Lista de todas las denuncias ordenadas por fecha
     */
    List<Complaint> findAllByOrderByCreatedAtDesc();

    /**
     * Cuenta denuncias por estado.
     *
     * @param status Estado a contar
     * @return Numero de denuncias en ese estado
     */
    long countByStatus(String status);

    /**
     * Busca denuncias que requieren mas informacion.
     *
     * @return Lista de denuncias con requires_more_info = true
     */
    List<Complaint> findByRequiresMoreInfoTrueOrderByCreatedAtDesc();
}
