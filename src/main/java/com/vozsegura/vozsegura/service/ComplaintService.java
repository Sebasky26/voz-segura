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
 * Maneja cifrado, persistencia y reglas de negocio.
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
     * El texto se cifra antes de almacenarse.
     * Las evidencias también se cifran.
     *
     * @param form datos del formulario
     * @param citizenHash hash del ciudadano (no reversible)
     * @return trackingId generado
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
        complaint.setCompanyEmail(form.getCompanyEmail());
        complaint.setCompanyPhone(form.getCompanyPhone());
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
    public Optional<Complaint> findByTrackingId(String trackingId) {
        return complaintRepository.findByTrackingId(trackingId);
    }

    /**
     * Lista todas las denuncias (para staff).
     * No expone identidad del denunciante.
     */
    public List<Complaint> findAll() {
        return complaintRepository.findAll();
    }

    /**
     * Actualiza el estado de una denuncia.
     */
    @Transactional
    public void updateStatus(String trackingId, String newStatus, String actorUsername, String actorRole) {
        complaintRepository.findByTrackingId(trackingId).ifPresent(complaint -> {
            complaint.setStatus(newStatus);
            complaint.setUpdatedAt(OffsetDateTime.now());
            complaintRepository.save(complaint);
            auditService.logEvent(actorRole, actorUsername, "STATUS_CHANGED", trackingId, "Nuevo estado: " + newStatus);
        });
    }

    /**
     * Marca una denuncia como derivada.
     */
    @Transactional
    public void derive(String trackingId, String destination, String actorUsername, String actorRole) {
        complaintRepository.findByTrackingId(trackingId).ifPresent(complaint -> {
            complaint.setStatus("DERIVED");
            complaint.setUpdatedAt(OffsetDateTime.now());
            complaintRepository.save(complaint);
            auditService.logEvent(actorRole, actorUsername, "CASE_DERIVED", trackingId, "Derivado a: " + destination);
        });
    }
}
