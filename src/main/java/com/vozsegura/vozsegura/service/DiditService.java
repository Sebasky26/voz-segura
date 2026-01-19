package com.vozsegura.vozsegura.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vozsegura.vozsegura.domain.entity.DiditVerification;
import com.vozsegura.vozsegura.dto.webhook.DiditWebhookPayload;
import com.vozsegura.vozsegura.repo.DiditVerificationRepository;
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
 * Servicio para integración con Didit - Verificación biométrica y escaneo de documentos.
 * 
 * Funcionalidades:
 * - Crear sesiones de verificación
 * - Validar firmas de webhooks
 * - Procesar resultados de verificación
 * - Guardar datos de documento en Supabase
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
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DiditService(
            DiditVerificationRepository diditVerificationRepository,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.diditVerificationRepository = diditVerificationRepository;
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
            
            log.debug("Creating Didit verification session with:");
            log.debug("- API URL: {}", url);
            log.debug("- Webhook URL (callback): {}", webhookUrl);
            log.debug("- Return URL: {}", returnUrl);
            log.debug("- Workflow ID: {}", workflowId);

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
            
            log.debug("Didit request body: {}", requestBody);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response != null) {
                log.info("Didit session created successfully: session_id={}, url={}", 
                        response.get("session_id"), response.get("url"));
                log.debug("Full Didit response: {}", response);
            } else {
                log.warn("Didit returned null response");
            }
            return response;

        } catch (Exception e) {
            log.error("Error creating Didit session. API URL: {}, Workflow: {}, API Key: {}", 
                    apiUrl, workflowId, (apiKey != null ? "***" : "NOT SET"), e);
            throw new RuntimeException("Error creando sesión de verificación con Didit: " + e.getMessage(), e);
        }
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
            log.info("📍 Starting webhook payload processing...");
            DiditWebhookPayload webhookData = objectMapper.readValue(payload, DiditWebhookPayload.class);
            
            log.info("📍 Parsed webhook data. SessionId: {}, WebhookType: {}", 
                    webhookData.getSessionId(), webhookData.getWebhookType());

            // Obtener el status - puede estar en el nivel raíz o en decision
            String status = webhookData.getStatus();
            if (status == null && webhookData.getDecision() != null) {
                status = webhookData.getDecision().getStatus();
                log.info("📍 Status extracted from decision: {}", status);
            } else {
                log.info("📍 Status from root level: {}", status);
            }

            // Solo procesar si el status es "Approved"
            if (status == null || !status.equals("Approved")) {
                log.info("ℹ️ Ignoring webhook with status: {}. Only processing 'Approved' verifications.", status);
                return null;
            }

            // Intentar obtener document_data del nivel raíz
            DiditWebhookPayload.DocumentData docData = webhookData.getDocumentData();
            log.info("📍 DocumentData from root: {}", docData != null ? "Found" : "Not found");

            // Si no, intentar desde decision.id_verifications
            if (docData == null && webhookData.getDecision() != null && 
                webhookData.getDecision().getIdVerifications() != null && 
                !webhookData.getDecision().getIdVerifications().isEmpty()) {
                
                DiditWebhookPayload.Decision.IdVerification idVerif = webhookData.getDecision().getIdVerifications().get(0);
                log.info("📍 Found IdVerification in decision. PersonalNumber: {}, Name: {}", 
                        idVerif.getPersonalNumber(), idVerif.getFullName());
                
                // Convertir IdVerification a DocumentData
                docData = new DiditWebhookPayload.DocumentData();
                docData.setPersonalNumber(idVerif.getPersonalNumber());
                docData.setFirstName(idVerif.getFirstName());
                docData.setLastName(idVerif.getLastName());
                docData.setFullName(idVerif.getFullName() != null ? idVerif.getFullName() : 
                    (idVerif.getFirstName() + " " + idVerif.getLastName()));
                
                log.info("📍 Converted to DocumentData: {} - {}", docData.getPersonalNumber(), docData.getFullName());
            }

            // Verificar que tenemos los datos del documento
            if (docData == null) {
                log.warn("❌ Webhook payload approved but without document_data");
                throw new IllegalArgumentException("El payload del webhook no contiene datos del documento");
            }

            // Verificar si el usuario ya existe en la BD
            Optional<DiditVerification> existingVerification = diditVerificationRepository.findByDocumentNumber(docData.getPersonalNumber());
            
            DiditVerification verification;
            if (existingVerification.isPresent()) {
                // Actualizar registro existente
                verification = existingVerification.get();
                log.info("📍 User already exists in database. Updating verification. Old sessionId: {}, New sessionId: {}", 
                        verification.getDiditSessionId(), webhookData.getSessionId());
                
                // Actualizar todos los campos
                verification.setDiditSessionId(webhookData.getSessionId());
                verification.setFirstName(docData.getFirstName());
                verification.setLastName(docData.getLastName());
                verification.setFullName(docData.getFullName());
                verification.setVerificationStatus(status);
                verification.setWebhookIp(ipAddress);
                verification.setVerifiedAt(OffsetDateTime.now());
                verification.setWebhookPayload(payload);
                
                log.info("🔄 Updated record: document={}, fullName={}", docData.getPersonalNumber(), docData.getFullName());
            } else {
                // Crear nuevo registro
                verification = new DiditVerification();
                verification.setDiditSessionId(webhookData.getSessionId());
                verification.setDocumentNumber(docData.getPersonalNumber());
                verification.setFirstName(docData.getFirstName());
                verification.setLastName(docData.getLastName());
                verification.setFullName(docData.getFullName());
                verification.setVerificationStatus(status);
                verification.setWebhookIp(ipAddress);
                verification.setVerifiedAt(OffsetDateTime.now());
                verification.setWebhookPayload(payload);
                
                log.info("🆕 Creating new verification record: document={}, fullName={}", docData.getPersonalNumber(), docData.getFullName());
            }

            DiditVerification saved = diditVerificationRepository.save(verification);
            
            log.info("✅ Didit verification saved to database: sessionId={}, document={}, fullName={}, status={}, operation={}", 
                    saved.getDiditSessionId(), saved.getDocumentNumber(), saved.getFullName(), saved.getVerificationStatus(),
                    existingVerification.isPresent() ? "UPDATE" : "INSERT");
            
            return saved;

        } catch (Exception e) {
            log.error("❌ Error processing Didit webhook payload: {}", e.getMessage(), e);
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
        DiditVerification verification = diditVerificationRepository
                .findByDiditSessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Verificación no encontrada: " + sessionId));

        verification.setCitizenHash(citizenHash);
        diditVerificationRepository.save(verification);
        
        log.info("Didit verification linked to citizen hash: {}", citizenHash);
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
            
            // Crear y guardar verificación
            DiditVerification verification = new DiditVerification();
            verification.setDiditSessionId(sessionId);
            verification.setDocumentNumber(personalNumber);
            verification.setFirstName(firstName);
            verification.setLastName(lastName);
            verification.setFullName(fullName != null ? fullName : (firstName + " " + lastName));
            verification.setVerificationStatus("VERIFIED");
            verification.setVerifiedAt(OffsetDateTime.now());
            
            try {
                verification.setWebhookPayload(objectMapper.writeValueAsString(body));
            } catch (Exception e) {
                log.debug("Could not serialize payload: {}", e.getMessage());
            }
            
            DiditVerification saved = diditVerificationRepository.save(verification);
            
            log.info("✅ Session decision retrieved from Didit API and saved: document={}, fullName={}", 
                    personalNumber, verification.getFullName());
            
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
}

