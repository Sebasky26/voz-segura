package com.vozsegura.vozsegura.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vozsegura.vozsegura.domain.entity.DiditVerification;
import com.vozsegura.vozsegura.domain.entity.Persona;
import com.vozsegura.vozsegura.dto.webhook.DiditWebhookPayload;
import com.vozsegura.vozsegura.repo.DiditVerificationRepository;
import com.vozsegura.vozsegura.repo.PersonaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio para integración con Didit v3 - Plataforma de verificación de identidad.
 * 
 * Responsabilidades:
 * - Crear sesiones de verificación (escaneo de documento + biometría)
 * - Generar URLs QR para que usuario escanee en su teléfono
 * - Validar firmas de webhooks (HMAC-SHA256) para evitar falsificaciones
 * - Procesar resultados de verificación completados
 * - Guardar datos de verificación en base de datos
 * - Manejar fallos y reintentos
 * 
 * Flujo de verificación Didit:
 * 1. Core app: createVerificationSession(vendorData) → Retorna URL QR
 * 2. Frontend: Mostrar QR para escanear
 * 3. Usuario: Escanea QR en su teléfono
 * 4. Didit app: Abre documento (pasaporte/cédula) + toma selfie
 * 5. Didit: Procesa verificación (OCR + biometría)
 * 6. Didit: Envía webhook a Core app (/webhooks/didit) con resultado
 * 7. Core app: Valida firma webhook, procesa resultado
 * 8. Si OK: Guarda DiditVerification, usuario pasa a siguiente paso
 * 
 * Validación de webhooks:
 * - Didit firma cada webhook con HMAC-SHA256
 * - Firma en header: X-Didit-Signature
 * - Core app valida: HMAC(webhook-secret-key, payload) == firma
 * - Protege contra: falsificaciones, ataques man-in-the-middle
 * 
 * Configuración en properties:
 * - didit.api-key: Clave API de Didit
 * - didit.webhook-secret-key: Clave para validar webhooks
 * - didit.workflow-id: ID del workflow Didit (ej: Ecuador1 con documento + biometría)
 * - didit.api-url: URL de API Didit (https://www.didit.me)
 * - didit.webhook-url: URL de callback (ej: https://core.example.com/webhooks/didit)
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
@Slf4j
@Service
public class DiditService {

    @Value("${didit.api-key}")
    private String apiKey;

    @Value("${didit.webhook-secret-key}")
    private String webhookSecretKey;

    @Value("${didit.workflow-id}")
    private String workflowId;

    @Value("${didit.api-url}")
    private String apiUrl;

    @Value("${didit.webhook-url}")
    private String webhookUrl;

    private final DiditVerificationRepository diditVerificationRepository;
    private final PersonaRepository personaRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DiditService(
            DiditVerificationRepository diditVerificationRepository,
            PersonaRepository personaRepository,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.diditVerificationRepository = diditVerificationRepository;
        this.personaRepository = personaRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Crea una sesión de verificación con Didit
     * Usa el endpoint v3/session para generar una URL interactiva donde el usuario
     * puede escanear un QR para verificar su identidad
     */
    @Transactional
    public Map<String, Object> createVerificationSession(String vendorData) {
        try {
            // Usar endpoint v3/session para crear sesión interactiva (QR, escaneo de documento)
            String url = apiUrl + "/v3/session/";
            
            // Construir URLs: reemplazar /webhooks/didit con /auth/verify-callback
            String returnUrl = webhookUrl.replace("/webhooks/didit", "/auth/verify-callback");
            
            // SEGURIDAD: NO loggear URLs completas ni request bodies (contienen tokens y webhooks)

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("workflow_id", workflowId);
            requestBody.put("callback", webhookUrl);
            requestBody.put("return_url", returnUrl);
            if (vendorData != null && !vendorData.isEmpty()) {
                requestBody.put("vendor_data", vendorData);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Api-Key", apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response != null) {
                String sessionId = (String) response.get("session_id");
                // SEGURIDAD: NO loggear URL completa (contiene session token en path)
                log.info("Didit session created - ID: {}", maskSessionId(sessionId));
            } else {
                log.warn("Didit returned null response");
            }
            return response;

        } catch (Exception e) {
            log.error("Error creating Didit session. Workflow: {}", workflowId, e);
            throw new RuntimeException("Error creando sesión de verificación con Didit: " + e.getMessage(), e);
        }
    }

    /**
     * Enmascara session ID para logs seguros
     */
    private String maskSessionId(String sessionId) {
        if (sessionId == null || sessionId.length() < 8) return "***";
        return sessionId.substring(0, 8) + "...";
    }

    /**
     * Verifica la firma HMAC del webhook de Didit
     * 
     * @param payload el cuerpo del webhook (JSON string)
     * @param signature la firma enviada en el header x-signature
     * @return true si la firma es válida
     */
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            String computedSignature = computeHmacSha256(payload, webhookSecretKey);
            boolean isValid = computedSignature.equals(signature);
            
            if (!isValid) {
                log.warn("Webhook signature verification failed. Expected: {}, Got: {}", 
                        computedSignature, signature);
            } else {
                log.info("Webhook signature verified successfully");
            }
            
            return isValid;
        } catch (Exception e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }

    /**
     * Computa HMAC-SHA256 de un payload con una clave secreta
     */
    private String computeHmacSha256(String payload, String secretKey) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8),
                0,
                secretKey.getBytes(StandardCharsets.UTF_8).length,
                "HmacSHA256"
        );
        mac.init(keySpec);
        byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Procesa el resultado del webhook de Didit
     * Acepta webhooks con "status":"Approved" independientemente del webhook_type
     * Extrae datos del documento desde múltiples posibles ubicaciones
     * 
     * @param payload el JSON payload del webhook
     * @param ipAddress IP desde la que se envió el webhook
     * @return objeto DiditVerification guardado en BD, o null si no es verificación aprobada
     */
    @Transactional
    public DiditVerification processWebhookPayload(String payload, String ipAddress) {
        try {
            log.info("Starting webhook payload processing...");
            DiditWebhookPayload webhookData = objectMapper.readValue(payload, DiditWebhookPayload.class);
            
            log.info("Parsed webhook data. SessionId: {}, WebhookType: {}", 
                    webhookData.getSessionId(), webhookData.getWebhookType());

            // Obtener el status - puede estar en el nivel raíz o en decision
            String webhookStatus = webhookData.getStatus();
            if (webhookStatus == null && webhookData.getDecision() != null) {
                webhookStatus = webhookData.getDecision().getStatus();
                log.info("Status extracted from decision: {}", webhookStatus);
            } else {
                log.info("Status from root level: {}", webhookStatus);
            }

            // Solo procesar si el status es "Approved"
            if (webhookStatus == null || !webhookStatus.equals("Approved")) {
                log.info("Ignoring webhook with status: {}. Only processing 'Approved' verifications.", webhookStatus);
                return null;
            }

            // Mapear el status de Didit al enum de la base de datos
            // Didit envía "Approved" pero la BD espera "VERIFIED", "FAILED", o "PENDING"
            String status = "VERIFIED";  // Mapping: Approved → VERIFIED
            log.info("Mapped webhook status 'Approved' to database value '{}'", status);

            // Intentar obtener document_data del nivel raíz
            DiditWebhookPayload.DocumentData docData = webhookData.getDocumentData();
            log.info("DocumentData from root: {}", docData != null ? "Found" : "Not found");

            // Si no, intentar desde decision.id_verifications
            if (docData == null && webhookData.getDecision() != null && 
                webhookData.getDecision().getIdVerifications() != null && 
                !webhookData.getDecision().getIdVerifications().isEmpty()) {
                
                DiditWebhookPayload.Decision.IdVerification idVerif = webhookData.getDecision().getIdVerifications().get(0);
                log.info("Found IdVerification in decision. PersonalNumber: {}, Name: {}", 
                        idVerif.getPersonalNumber(), idVerif.getFullName());
                
                // Convertir IdVerification a DocumentData
                docData = new DiditWebhookPayload.DocumentData();
                docData.setPersonalNumber(idVerif.getPersonalNumber());
                docData.setFirstName(idVerif.getFirstName());
                docData.setLastName(idVerif.getLastName());
                docData.setFullName(idVerif.getFullName() != null ? idVerif.getFullName() : 
                    (idVerif.getFirstName() + " " + idVerif.getLastName()));
                
                log.info("Converted to DocumentData: {} - {}", docData.getPersonalNumber(), docData.getFullName());
            }

            // Verificar que tenemos los datos del documento
            if (docData == null) {
                log.warn("Webhook payload approved but without document_data");
                throw new IllegalArgumentException("El payload del webhook no contiene datos del documento");
            }

            // Verificar si el usuario ya existe en la BD
            Optional<DiditVerification> existingVerification = diditVerificationRepository.findByDocumentNumber(docData.getPersonalNumber());
            
            DiditVerification verification;
            if (existingVerification.isPresent()) {
                // Actualizar registro existente
                verification = existingVerification.get();
                log.info("User already exists in database. Updating verification. Old sessionId: {}, New sessionId: {}", 
                        verification.getDiditSessionId(), webhookData.getSessionId());
                
                // Actualizar solo campos necesarios
                verification.setDiditSessionId(webhookData.getSessionId());
                verification.setVerificationStatus(status);
                verification.setVerifiedAt(OffsetDateTime.now());
                verification.setUpdatedAt(OffsetDateTime.now());
                
                log.info("Updated record: document={}", docData.getPersonalNumber());
            } else {
                // Crear nuevo registro - Obtener id_registro de registro_civil.personas
                verification = new DiditVerification();
                verification.setDiditSessionId(webhookData.getSessionId());
                verification.setDocumentNumber(docData.getPersonalNumber());
                verification.setVerificationStatus(status);
                verification.setVerifiedAt(OffsetDateTime.now());
                verification.setCreatedAt(OffsetDateTime.now());
                verification.setUpdatedAt(OffsetDateTime.now());
                
                // Generar hash SHA-256 de la cedula para buscar en personas
                String cedulaHash = generateSHA256Hash(docData.getPersonalNumber());
                
                // Buscar id_registro en registro_civil.personas basado en cedula_hash
                Optional<Persona> persona = personaRepository.findByCedulaHash(cedulaHash);
                if (persona.isPresent()) {
                    verification.setIdRegistro(persona.get().getIdRegistro());
                    log.info("Creating new verification record: document={}, id_registro={}, status={}", 
                            docData.getPersonalNumber(), persona.get().getIdRegistro(), status);
                } else {
                    log.error("Persona not found for document_number: {} (cedula_hash: {}). Cannot create verification without id_registro.", 
                            docData.getPersonalNumber(), cedulaHash);
                    throw new IllegalArgumentException("No existe persona registrada con la cédula proporcionada por Didit");
                }
            }

            DiditVerification saved = diditVerificationRepository.save(verification);
            
            log.info("Didit verification saved to database: sessionId={}, document={}, status={}, operation={}", 
                    saved.getDiditSessionId(), saved.getDocumentNumber(), saved.getVerificationStatus(),
                    existingVerification.isPresent() ? "UPDATE" : "INSERT");
            
            return saved;

        } catch (Exception e) {
            log.error("Error processing Didit webhook payload: {}", e.getMessage(), e);
            throw new RuntimeException("Error procesando payload de Didit", e);
        }
    }

    /**
     * Obtiene una verificación por session ID
     */
    public Optional<DiditVerification> getVerificationBySessionId(String sessionId) {
        return diditVerificationRepository.findByDiditSessionId(sessionId);
    }

    /**
     * Obtiene una verificación por número de documento
     */
    public Optional<DiditVerification> getVerificationByDocumentNumber(String documentNumber) {
        return diditVerificationRepository.findByDocumentNumber(documentNumber);
    }

    /**
     * Vincula una verificación de Didit con un hash de ciudadano
     * Esto permite asociar los datos del documento con las denuncias posteriores
     */
    @Transactional
    public void linkVerificationToCitizen(String sessionId, String citizenHash) {
        // Método mantenido por compatibilidad, pero ya no necesario
        // El document_number ahora se vincula directamente con personas.cedula
        DiditVerification verification = diditVerificationRepository
                .findByDiditSessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Verificación no encontrada: " + sessionId));
        
        log.info("Didit verification already linked via document_number constraint");
    }

    /**
     * Obtiene decisión de sesión directamente desde Didit
     * Fallback sincrónico para cuando el webhook falla (endpoint con permisos limitados)
     * También intenta endpoint v2 como fallback si v3 falla
     */
    public Optional<DiditVerification> getSessionDecisionFromDidit(String sessionId) {
        try {
            // Intentar primero con v3
            String urlV3 = apiUrl + "/v3/session/" + sessionId + "/decision/";
            log.debug("Attempting to fetch session decision from v3 endpoint: {}", urlV3);
            
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + apiKey);
                headers.setContentType(MediaType.APPLICATION_JSON);
                
                HttpEntity<String> entity = new HttpEntity<>(headers);
                ResponseEntity<Map> response = restTemplate.exchange(
                    urlV3, 
                    HttpMethod.GET, 
                    entity, 
                    Map.class
                );
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    return processDiditDecisionResponse(response.getBody(), sessionId);
                }
            } catch (Exception e) {
                log.warn("v3 endpoint failed ({}), trying v2 endpoint...", e.getMessage());
            }
            
            // Fallback a v2 endpoint
            String urlV2 = apiUrl + "/v2/session/" + sessionId + "/";
            log.debug("Attempting to fetch session from v2 endpoint: {}", urlV2);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Api-Key", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                urlV2, 
                HttpMethod.GET, 
                entity, 
                Map.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Didit v2 returned non-success status: {}", response.getStatusCode());
                return Optional.empty();
            }
            
            return processDiditDecisionResponse(response.getBody(), sessionId);
            
        } catch (Exception e) {
            log.error("Error fetching session decision from Didit: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Procesa la respuesta de decisión de Didit
     */
    private Optional<DiditVerification> processDiditDecisionResponse(Map<String, Object> body, String sessionId) {
        try {
            log.debug("Processing Didit decision response: {}", body);
            
            String status = (String) body.get("status");
            
            if (!"Approved".equals(status) && !"VERIFIED".equals(status) && !"Completed".equals(status)) {
                log.warn("Didit session status not approved: {}", status);
                return Optional.empty();
            }
            
            // Extraer datos del documento - puede estar en varios lugares según la respuesta
            Map<String, Object> documentData = extractDocumentData(body);
            
            if (documentData == null) {
                log.warn("No document_data found in Didit response. Full body keys: {}", body.keySet());
                return Optional.empty();
            }
            
            String personalNumber = (String) documentData.get("personal_number");
            String firstName = (String) documentData.get("first_name");
            String lastName = (String) documentData.get("last_name");
            String fullName = (String) documentData.get("full_name");
            
            if (personalNumber == null || firstName == null || lastName == null) {
                log.warn("Missing required fields. PersonalNumber: {}, FirstName: {}, LastName: {}", 
                        personalNumber, firstName, lastName);
                return Optional.empty();
            }
            
            // Crear y guardar verificación - Solo guardamos cédula, no nombres por privacidad
            DiditVerification verification = new DiditVerification();
            verification.setDiditSessionId(sessionId);
            verification.setDocumentNumber(personalNumber);
            // No guardamos firstName, lastName, fullName por privacidad
            // Campos firstName, lastName, fullName removidos - solo guardamos cedula
            verification.setVerificationStatus("VERIFIED");
            verification.setVerifiedAt(OffsetDateTime.now());
            // No guardamos el payload por privacidad
            
            DiditVerification saved = diditVerificationRepository.save(verification);
            
            log.info("Session decision retrieved from Didit API and saved: document={} (nombres no almacenados por privacidad)", 
                    personalNumber);
            
            return Optional.of(saved);
            
        } catch (Exception e) {
            log.error("Error processing Didit decision response: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Extrae document_data de la respuesta de Didit (puede estar en varios niveles)
     */
    private Map<String, Object> extractDocumentData(Map<String, Object> response) {
        // Intentar en "document_data"
        if (response.containsKey("document_data")) {
            Object data = response.get("document_data");
            if (data instanceof Map) {
                return (Map<String, Object>) data;
            }
        }
        
        // Intentar en "document_verification" -> "document_data"
        if (response.containsKey("document_verification")) {
            Object docVerif = response.get("document_verification");
            if (docVerif instanceof Map) {
                Map<String, Object> docVerifMap = (Map<String, Object>) docVerif;
                if (docVerifMap.containsKey("document_data")) {
                    Object data = docVerifMap.get("document_data");
                    if (data instanceof Map) {
                        return (Map<String, Object>) data;
                    }
                }
            }
        }
        
        // Intentar en "verification_result" -> "document_data"
        if (response.containsKey("verification_result")) {
            Object verifResult = response.get("verification_result");
            if (verifResult instanceof Map) {
                Map<String, Object> verIfResultMap = (Map<String, Object>) verifResult;
                if (verIfResultMap.containsKey("document_data")) {
                    Object data = verIfResultMap.get("document_data");
                    if (data instanceof Map) {
                        return (Map<String, Object>) data;
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Genera hash SHA-256 de una cadena de texto
     * Usado para buscar personas por cedula_hash
     */
    private String generateSHA256Hash(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error generating SHA-256 hash for: {}", input, e);
            throw new RuntimeException("Error generando hash SHA-256", e);
        }
    }
}

