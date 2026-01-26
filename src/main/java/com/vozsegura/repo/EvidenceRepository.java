package com.vozsegura.repo;

import com.vozsegura.domain.entity.Complaint;
import com.vozsegura.domain.entity.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository para entidad Evidence.
 * 
 * Gestiona evidencias (archivos) adjuntos a denuncias.
 * 
 * Almacenamiento:
 * - Archivos se guardan en S3/blob storage (no en base de datos)
 * - Base de datos almacena: metadata (nombre, tipo, tamaño, URL, hash)
 * - Máximo 5 evidencias por denuncia
 * - Máximo 25MB por archivo
 * 
 * Validación:
 * - Whitelist de tipos MIME (PDF, DOCX, JPG, PNG, MP4, etc.)
 * - Validación de hash para integridad (SHA-256)
 * - Escaneo antivirus (integración con ClamAV)
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, Long> {

    /**
     * Busca todas las evidencias asociadas a una denuncia.
     * 
     * @param complaint Objeto Complaint (se busca por su ID)
     * @return Lista de evidencias de esa denuncia (puede estar vacía)
     */
    List<Evidence> findByComplaint(Complaint complaint);

    /**
     * Busca todas las evidencias por ID de denuncia.
     * Alternativa más eficiente que findByComplaint (evita cargar la entidad completa).
     * 
     * @param complaintId ID de la denuncia
     * @return Lista de evidencias de esa denuncia
     */
    List<Evidence> findByComplaintId(Long complaintId);

    /**
     * Cuenta la cantidad de evidencias adjuntas a una denuncia.
     * Útil para validar no exceder máximo (5 archivos por denuncia).
     * 
     * @param complaint Objeto Complaint
     * @return Número de evidencias adjuntas (0-5)
     */
    int countByComplaint(Complaint complaint);
}
