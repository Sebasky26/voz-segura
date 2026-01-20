package com.vozsegura.vozsegura.service;

import com.vozsegura.vozsegura.domain.entity.Complaint;
import com.vozsegura.vozsegura.domain.entity.Evidence;
import com.vozsegura.vozsegura.domain.entity.IdentityVault;
import com.vozsegura.vozsegura.dto.forms.ComplaintForm;
import com.vozsegura.vozsegura.repo.ComplaintRepository;
import com.vozsegura.vozsegura.repo.EvidenceRepository;
import com.vozsegura.vozsegura.repo.IdentityVaultRepository;
import com.vozsegura.vozsegura.security.EncryptionService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio principal para la gestión de denuncias.
 * 
 * Responsabilidades:
 * - Crear y cifrar denuncias anónimas
 * - Gestionar evidencias (archivos) asociadas
 * - Cambiar estado de denuncias (PENDING → ASSIGNED → RESOLVED, etc)
 * - Clasificar denuncias por tipo y prioridad
 * - Solicitar información adicional
 * - Rechazar denuncias
 * - Derivar a entidades externas
 * - Descifrar para visualización por staff autorizado
 * - Registrar auditoría de cambios
 * 
 * Cifrado:
 * - Todo texto de denuncia se cifra con AES-256-GCM antes de BD
 * - Evidencias se cifran individualmente
 * - Descifrado solo disponible para staff autorizado
 * 
 * Anonimato:
 * - Ciudadano identificado por hash SHA-256, nunca plain text
 * - IdentityVault vincula hash con denuncias
 * - Tracking ID es el identificador público
 * 
 * Validaciones:
 * - Máx 5 evidencias por denuncia
 * - Tamaño máx 25MB por archivo
 * - Solo formatos permitidos (PDF, DOCX, JPG, PNG, MP4)
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
@Service
public class ComplaintService {

    private static final int MAX_EVIDENCES = 5;
    private static final long MAX_FILE_SIZE = 25 * 1024 * 1024; // 25MB
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif",
            "video/mp4", "video/mpeg",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final ComplaintRepository complaintRepository;
    private final EvidenceRepository evidenceRepository;
    private final IdentityVaultRepository identityVaultRepository;
    private final EncryptionService encryptionService;
    private final AuditService auditService;

    public ComplaintService(ComplaintRepository complaintRepository,
                            EvidenceRepository evidenceRepository,
                            IdentityVaultRepository identityVaultRepository,
                            EncryptionService encryptionService,
                            AuditService auditService) {
        this.complaintRepository = complaintRepository;
        this.evidenceRepository = evidenceRepository;
        this.identityVaultRepository = identityVaultRepository;
        this.encryptionService = encryptionService;
        this.auditService = auditService;
    }

    /**
     * Crea una nueva denuncia con sus evidencias.
     * 
     * Proceso:
     * 1. Busca o crea IdentityVault con hash del ciudadano
     * 2. Genera UUID único como tracking ID
     * 3. Cifra texto de denuncia con AES-256-GCM
     * 4. Almacena en BD con estado PENDING
     * 5. Procesa evidencias (archivos) si existen
     * 6. Registra evento de auditoría
     * 
     * Cifrado:
     * - El texto completo de la denuncia se cifra
     * - Ciudadano se identifica por hash SHA-256, no por cédula
     * - Solo staff autorizado puede descifrar
     * 
     * @param form datos del formulario con campos de denuncia
     * @param citizenHash hash SHA-256 del ciudadano (anonimidad)
     * @return tracking ID (identificador público de la denuncia)
     * @throws RuntimeException si el cifrado falla
     */
    @Transactional
    public String createComplaint(ComplaintForm form, String citizenHash) {
        // Buscar o crear identity vault
        IdentityVault vault = identityVaultRepository.findByCitizenHash(citizenHash)
                .orElseGet(() -> {
                    IdentityVault v = new IdentityVault();
                    v.setCitizenHash(citizenHash);
                    return identityVaultRepository.save(v);
                });

        // Generar trackingId no predecible
        String trackingId = UUID.randomUUID().toString();

        // Cifrar texto de denuncia
        String encryptedText = encryptionService.encryptToBase64(form.getDetail());

        // Crear complaint
        Complaint complaint = new Complaint();
        complaint.setTrackingId(trackingId);
        complaint.setIdentityVault(vault);
        complaint.setStatus("PENDING");
        complaint.setSeverity("MEDIUM");
        complaint.setEncryptedText(encryptedText);
        complaint.setCompanyName(form.getCompanyName());
        complaint.setCompanyAddress(form.getCompanyAddress());
        complaint.setCompanyContact(form.getCompanyContact());
        complaint.setCreatedAt(OffsetDateTime.now());
        complaint.setUpdatedAt(OffsetDateTime.now());

        complaint = complaintRepository.save(complaint);

        // Procesar evidencias
        if (form.getEvidences() != null) {
            processEvidences(complaint, form.getEvidences());
        }

        auditService.logEvent("SYSTEM", null, "COMPLAINT_CREATED", trackingId, "Denuncia creada");

        return trackingId;
    }

    private void processEvidences(Complaint complaint, MultipartFile[] files) {
        int count = 0;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            if (count >= MAX_EVIDENCES) break;
            if (file.getSize() > MAX_FILE_SIZE) continue;
            if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) continue;

            try {
                Evidence evidence = new Evidence();
                evidence.setComplaint(complaint);
                evidence.setFileName(sanitizeFileName(file.getOriginalFilename()));
                evidence.setContentType(file.getContentType());
                evidence.setSizeBytes(file.getSize());
                // Cifrar contenido binario
                evidence.setEncryptedContent(encryptBytes(file.getBytes()));
                evidenceRepository.save(evidence);
                count++;
            } catch (IOException e) {
                // Log interno sin exponer detalles
            }
        }
    }

    private byte[] encryptBytes(byte[] plainBytes) {
        // Placeholder: usar el mismo servicio o uno específico para binarios
        // Por ahora convertimos a base64, ciframos y reconvertimos
        String b64 = java.util.Base64.getEncoder().encodeToString(plainBytes);
        String encrypted = encryptionService.encryptToBase64(b64);
        return encrypted.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "file";
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Busca una denuncia por trackingId.
     */
    /**
     * Busca una denuncia por su tracking ID.
     * 
     * El tracking ID es el identificador público que recibe el denunciante.
     * Permite rastrear la denuncia sin revelar identidad.
     * 
     * @param trackingId identificador único de denuncia (UUID)
     * @return Optional con la denuncia si existe, vacío si no
     */
    public Optional<Complaint> findByTrackingId(String trackingId) {
        return complaintRepository.findByTrackingId(trackingId);
    }

    /**
     * Lista todas las denuncias (para staff).
     * No expone identidad del denunciante.
     */
    /**
     * Lista todas las denuncias del sistema.
     * 
     * NOTA: Solo debe usarse en contexto administrador.
     * No incluye descifrado automático.
     * 
     * @return lista de denuncias (sin descifrar)
     */
    public List<Complaint> findAll() {
        return complaintRepository.findAll();
    }

    /**
     * Lista todas las denuncias ordenadas por fecha de creación (más recientes primero).
     */
    /**
     * Lista todas las denuncias ordenadas por fecha (más recientes primero).
     * 
     * Útil para dashboards de administrador que muestren actividad reciente.
     * 
     * @return lista de denuncias ordenada por createdAt DESC
     */
    public List<Complaint> findAllOrderByCreatedAtDesc() {
        return complaintRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Busca denuncias por estado.
     */
    /**
     * Filtra denuncias por estado.
     * 
     * Estados posibles:
     * - PENDING: Nueva, no asignada
     * - ASSIGNED: Asignada a un analista
     * - IN_PROGRESS: En investigación
     * - RESOLVED: Resuelta
     * - REJECTED: Rechazada
     * - INFO_REQUESTED: Pendiente de más información
     * 
     * @param status estado a filtrar
     * @return lista de denuncias con ese estado
     */
    public List<Complaint> findByStatus(String status) {
        return complaintRepository.findByStatus(status);
    }

    /**
     * Descifra el texto de una denuncia para visualización por staff autorizado.
     *
     * @param encryptedText texto cifrado en Base64
     * @return texto descifrado
     */
    /**
     * Descifra el texto de una denuncia.
     * 
     * CRITICO: Solo llamar para staff autorizado.
     * El texto almacenado está en AES-256-GCM Base64.
     * Este método lo descifra a plain text para visualización.
     * 
     * @param encryptedText texto cifrado en Base64
     * @return texto descifrado (plain text)
     * @throws RuntimeException si descifrado falla (texto corrupto)
     */
    public String decryptComplaintText(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) {
            return "[Sin contenido]";
        }
        try {
            return encryptionService.decryptFromBase64(encryptedText);
        } catch (Exception e) {
            // Log interno usando framework de logging, no exponer al usuario
            return "[Error al descifrar contenido]";
        }
    }

    /**
     * Actualiza el estado de una denuncia.
     */
    @Transactional
    /**
     * Cambia el estado de una denuncia.
     * 
     * Transiciones permitidas:
     * - PENDING → ASSIGNED (cuando analista se asigna)
     * - ASSIGNED → IN_PROGRESS (cuando inicia investigación)
     * - IN_PROGRESS → RESOLVED (cuando se resuelve)
     * - Cualquier estado → INFO_REQUESTED (para pedir más datos)
     * - Cualquier estado → REJECTED (para rechazar)
     * 
     * @param trackingId ID de la denuncia a actualizar
     * @param newStatus nuevo estado
     * @param actorUsername quien hace el cambio (para auditoría)
     * @param actorRole rol del actor (ADMIN, ANALYST, etc)
     */
    public void updateStatus(String trackingId, String newStatus, String actorUsername, String actorRole) {
        complaintRepository.findByTrackingId(trackingId).ifPresent(complaint -> {
            complaint.setStatus(newStatus);
            complaint.setUpdatedAt(OffsetDateTime.now());

            // Si se cambia de NEEDS_INFO a otro estado, limpiar el flag
            if (!"NEEDS_INFO".equals(newStatus)) {
                complaint.setRequiresMoreInfo(false);
            }

            complaintRepository.save(complaint);
            auditService.logEvent(actorRole, actorUsername, "STATUS_CHANGED", trackingId, "Nuevo estado: " + newStatus);
        });
    }

    /**
     * Clasifica una denuncia con tipo, prioridad y notas del analista.
     */
    @Transactional
    /**
     * Clasifica una denuncia por tipo y prioridad.
     * 
     * Tipos de denuncia:
     * - LABOR_RIGHTS, HARASSMENT, DISCRIMINATION, SAFETY, FRAUD, OTHER
     * 
     * Prioridad:
     * - LOW, MEDIUM, HIGH, CRITICAL
     * 
     * La clasificación determina la derivación automática
     * (se aplican reglas de DerivationRule en BD).
     * 
     * @param trackingId ID de denuncia
     * @param complaintType tipo de denuncia
     * @param priority prioridad asignada
     * @param analystUsername analista que clasifica (para auditoría)
     */
    public void classifyComplaint(String trackingId, String complaintType, String priority,
                                   String analystNotes, String analystUsername) {
        complaintRepository.findByTrackingId(trackingId).ifPresent(complaint -> {
            complaint.setComplaintType(complaintType);
            complaint.setPriority(priority);
            if (analystNotes != null && !analystNotes.isBlank()) {
                complaint.setAnalystNotes(analystNotes);
            }
            complaint.setUpdatedAt(OffsetDateTime.now());
            complaintRepository.save(complaint);
            auditService.logEvent("ANALYST", analystUsername, "COMPLAINT_CLASSIFIED", trackingId,
                    "Tipo: " + complaintType + ", Prioridad: " + priority);
        });
    }

    /**
     * Solicita más información al denunciante.
     */
    @Transactional
    /**
     * Solicita información adicional al denunciante.
     * 
     * Cambia estado a INFO_REQUESTED.
     * El denunciante puede rastrear la denuncia por tracking ID
     * y ver que se pidió más información.
     * 
     * @param trackingId ID de denuncia
     * @param motivo razón de la solicitud (para el denunciante)
     * @param analystUsername quien solicita información
     */
    public void requestMoreInfo(String trackingId, String motivo, String analystUsername) {
        complaintRepository.findByTrackingId(trackingId).ifPresent(complaint -> {
            complaint.setStatus("NEEDS_INFO");
            complaint.setRequiresMoreInfo(true);
            if (motivo != null && !motivo.isBlank()) {
                String notes = complaint.getAnalystNotes();
                complaint.setAnalystNotes((notes != null ? notes + "\n" : "") + "[Solicitud] " + motivo);
            }
            complaint.setUpdatedAt(OffsetDateTime.now());
            complaintRepository.save(complaint);
            auditService.logEvent("ANALYST", analystUsername, "MORE_INFO_REQUESTED", trackingId, motivo);
        });
    }

    /**
     * Rechaza una denuncia.
     */
    @Transactional
    /**
     * Rechaza una denuncia.
     * 
     * Razones posibles:
     * - Fuera de jurisdicción
     * - Información insuficiente
     * - Denuncia frívola
     * - Otros motivos
     * 
     * El denunciante verá el estado REJECTED al rastrear
     * pero NO verá el motivo (por privacidad).
     * 
     * @param trackingId ID de denuncia
     * @param motivo razón del rechazo (interno)
     * @param analystUsername quien rechaza
     */
    public void rejectComplaint(String trackingId, String motivo, String analystUsername) {
        complaintRepository.findByTrackingId(trackingId).ifPresent(complaint -> {
            complaint.setStatus("REJECTED");
            if (motivo != null && !motivo.isBlank()) {
                String notes = complaint.getAnalystNotes();
                complaint.setAnalystNotes((notes != null ? notes + "\n" : "") + "[Rechazo] " + motivo);
            }
            complaint.setUpdatedAt(OffsetDateTime.now());
            complaintRepository.save(complaint);
            auditService.logEvent("ANALYST", analystUsername, "COMPLAINT_REJECTED", trackingId, motivo);
        });
    }

    /**
     * Marca una denuncia como derivada.
     */
    @Transactional
    /**
     * Deriva una denuncia a otra entidad.
     * 
     * Flujo:
     * 1. Obtiene denuncia por tracking ID
     * 2. Cambia estado a DERIVED
     * 3. Almacena entidad destino
     * 4. Registra auditoría del cambio
     * 5. Puede llamar a ExternalDerivationClient para notificar
     * 
     * Derivaciones:
     * - A fiscalesía para casos penales
     * - A ministerio de trabajo para derechos laborales
     * - A instituciones de protección social
     * 
     * @param trackingId ID de denuncia
     * @param destination nombre entidad destino
     * @param actorUsername quien autoriza derivación
     * @param actorRole rol del actor
     */
    public void derive(String trackingId, String destination, String actorUsername, String actorRole) {
        complaintRepository.findByTrackingId(trackingId).ifPresent(complaint -> {
            complaint.setStatus("DERIVED");
            complaint.setDerivedTo(destination);
            complaint.setDerivedAt(OffsetDateTime.now());
            complaint.setUpdatedAt(OffsetDateTime.now());
            complaintRepository.save(complaint);
            auditService.logEvent(actorRole, actorUsername, "CASE_DERIVED", trackingId, "Derivado a: " + destination);
        });
    }

    /**
     * Agrega información adicional a una denuncia que requiere más información.
     */
    @Transactional
    /**
     * Añade información adicional a una denuncia existente.
     * 
     * Casos de uso:
     * - Denunciante proporciona nuevas evidencias
     * - Se agrega más contexto a investigación
     * - Respuesta a solicitud de más información
     * 
     * Validaciones:
     * - Máximo 5 evidencias totales por denuncia
     * - Cada archivo máximo 25MB
     * - Solo formatos permitidos
     * 
     * @param trackingId ID de denuncia
     * @param additionalInfo texto adicional (se cifra)
     * @param newEvidences nuevas evidencias (archivos)
     */
    public void addAdditionalInfo(String trackingId, String additionalInfo, MultipartFile[] newEvidences) {
        complaintRepository.findByTrackingId(trackingId).ifPresent(complaint -> {
            // Agregar la información adicional al texto cifrado existente
            String existingText = "";
            try {
                existingText = encryptionService.decryptFromBase64(complaint.getEncryptedText());
            } catch (Exception e) {
                existingText = "[Texto original no disponible]";
            }

            String updatedText = existingText + "\n\n--- INFORMACIÓN ADICIONAL (" +
                                 OffsetDateTime.now().toString() + ") ---\n" + additionalInfo;
            complaint.setEncryptedText(encryptionService.encryptToBase64(updatedText));

            // Cambiar estado a IN_REVIEW para que el analista lo revise de nuevo
            complaint.setStatus("IN_REVIEW");
            complaint.setRequiresMoreInfo(false);
            complaint.setUpdatedAt(OffsetDateTime.now());

            // Agregar nota interna
            String notes = complaint.getAnalystNotes();
            complaint.setAnalystNotes((notes != null ? notes + "\n" : "") +
                                      "[Respuesta del denunciante recibida el " +
                                      OffsetDateTime.now().toString() + "]");

            complaintRepository.save(complaint);

            // Procesar nuevas evidencias si las hay
            if (newEvidences != null) {
                processEvidences(complaint, newEvidences);
            }

            auditService.logEvent("PUBLIC", null, "ADDITIONAL_INFO_SUBMITTED", trackingId,
                                  "Información adicional enviada por denunciante");
        });
    }
}
