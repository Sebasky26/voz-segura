package com.vozsegura.vozsegura.repo;

import com.vozsegura.vozsegura.domain.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository para entidad Complaint.
 * 
 * Proporciona:
 * - Operaciones CRUD automáticas (create, read, update, delete) vía Spring Data
 * - Métodos personalizados para búsquedas por trackingId y estado
 * 
 * Notas de Seguridad:
 * - findByTrackingId: Retorna denuncia por su ID de seguimiento (usado en búsqueda anónima)
 * - No expone datos sensibles (contenido cifrado en la entidad)
 * - Todas las denuncias tienen contenido cifrado con AES-256-GCM
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    /**
     * Busca una denuncia por su tracking ID (UUID hexadecimal).
     * 
     * @param trackingId Identificador único de seguimiento anónimo
     * @return Optional con denuncia si existe, vacío si no
     */
    Optional<Complaint> findByTrackingId(String trackingId);

    /**
     * Busca todas las denuncias con un estado específico.
     * 
     * Estados válidos: PENDING, ASSIGNED, IN_PROGRESS, RESOLVED, REJECTED, INFO_REQUESTED
     * 
     * @param status Estado de la denuncia
     * @return Lista de denuncias con ese estado
     */
    List<Complaint> findByStatus(String status);

    /**
     * Obtiene todas las denuncias ordenadas por fecha de creación descendente.
     * Útil para dashboards que muestran denuncias recientes primero.
     * 
     * @return Lista de denuncias más recientes primero
     */
    List<Complaint> findAllByOrderByCreatedAtDesc();
}
