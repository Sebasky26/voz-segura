package com.vozsegura.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vozsegura.domain.entity.DiditVerification;
import com.vozsegura.domain.entity.Persona;
import com.vozsegura.dto.webhook.DiditWebhookPayload;
import com.vozsegura.repo.DiditVerificationRepository;
import com.vozsegura.repo.PersonaRepository;
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
 * - Guardar datos de verificación en base de datos (hasheados/cifrados)
 *
 * SEGURIDAD:
 * - La cédula recibida de Didit se hashea (SHA-256) antes de guardar
 * - Se cifra (AES-256-GCM) para auditoría
 * - NUNCA se almacena en texto plano
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
    private final com.vozsegura.repo.IdentityVaultRepository identityVaultRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CryptoService cryptoService;

    public DiditService(
            DiditVerificationRepository diditVerificationRepository,
            PersonaRepository personaRepository,
            com.vozsegura.repo.IdentityVaultRepository identityVaultRepository,
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            CryptoService cryptoService) {
        this.diditVerificationRepository = diditVerificationRepository;
        this.personaRepository = personaRepository;
        this.identityVaultRepository = identityVaultRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.cryptoService = cryptoService;
    }

    /**
     * Crea una sesión de verificación con Didit
     */
    @Transactional
    public Map<String, Object> createVerificationSession(String vendorData) {
        try {
            String url = apiUrl + "/v3/session/";
            String returnUrl = webhookUrl.replace("/webhooks/didit", "/auth/verify-callback");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("workflow_id", workflowId);
            requestBody.put("callback", webhookUrl);
            requestBody.put("return_url", returnUrl);
            if (vendorData != null && !vendorData.isEmpty()) {
                requestBody.put("vendor_data", vendorData);
            }

            // Log solo si hay problema con localhost (no producción)
            if (webhookUrl.contains("localhost") || webhookUrl.contains("127.0.0.1")) {
                log.warn("Webhook URL points to localhost - Didit cannot send webhooks. Use ngrok or public URL.");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Api-Key", apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response == null) {
                throw new RuntimeException("Didit returned null response");
            }

            log.info("Didit verification session created successfully");

            return response;

        } catch (Exception e) {
            log.error("Error creating Didit session", e);
            throw new RuntimeException("Error creando sesión de verificación con Didit: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica la firma HMAC del webhook de Didit
     */
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            String computedSignature = computeHmacSha256(payload, webhookSecretKey);
            boolean isValid = computedSignature.equals(signature);
            
            if (!isValid) {
                log.warn("Webhook signature mismatch");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }

    /**
     * Computa HMAC-SHA256
     */
    private String computeHmacSha256(String payload, String secretKey) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Procesa el resultado del webhook de Didit.
     *
     * Flujo:
     * 1. Parsear payload JSON
     * 2. Verificar status "Approved"
     * 3. Extraer cédula (personalNumber) del documento
     * 4. Generar hash SHA-256 de la cédula
     * 5. Buscar persona en registro_civil.personas por cedula_hash
     * 6. Crear DiditVerification con hash y cifrado (NO texto plano)
     * 7. Guardar en BD
     *
     * @param payload JSON payload del webhook
     * @param ipAddress IP del webhook (para auditoría)
     * @return DiditVerification guardado, o null si no aprobado
     */
    @Transactional
    public DiditVerification processWebhookPayload(String payload, String ipAddress) {
        log.info("Processing webhook payload");

        try {
            DiditWebhookPayload webhookData = objectMapper.readValue(payload, DiditWebhookPayload.class);

            // Obtener el status
            String webhookStatus = webhookData.getStatus();
            if (webhookStatus == null && webhookData.getDecision() != null) {
                webhookStatus = webhookData.getDecision().getStatus();
            }

            // Solo procesar si el status es "Approved"
            if (webhookStatus == null || !webhookStatus.equals("Approved")) {
                log.info("Webhook status not Approved, skipping");
                return null;
            }

            // Mapear status: Approved → VERIFIED
            String status = "VERIFIED";

            // Extraer document_data
            DiditWebhookPayload.DocumentData docData = webhookData.getDocumentData();

            // Si no está en raíz, buscar en decision.id_verifications
            if (docData == null && webhookData.getDecision() != null &&
                webhookData.getDecision().getIdVerifications() != null && 
                !webhookData.getDecision().getIdVerifications().isEmpty()) {
                
                DiditWebhookPayload.Decision.IdVerification idVerif =
                    webhookData.getDecision().getIdVerifications().get(0);

                docData = new DiditWebhookPayload.DocumentData();
                docData.setPersonalNumber(idVerif.getPersonalNumber());
                docData.setFirstName(idVerif.getFirstName());
                docData.setLastName(idVerif.getLastName());
                docData.setFullName(idVerif.getFullName() != null ? idVerif.getFullName() : 
                    (idVerif.getFirstName() + " " + idVerif.getLastName()));
            }

            if (docData == null || docData.getPersonalNumber() == null) {
                throw new IllegalArgumentException("El payload del webhook no contiene número de documento");
            }

            String personalNumber = docData.getPersonalNumber();

            // Generar hash SHA-256 de la cedula
            final String documentHash = cryptoService.hashCedula(personalNumber);

            // Cifrar la cedula para auditoria
            String documentEncrypted = cryptoService.encryptPII(personalNumber);

            // Buscar verificacion existente por hash
            Optional<DiditVerification> existingVerification =
                diditVerificationRepository.findByDocumentNumberHash(documentHash);

            DiditVerification verification;
            if (existingVerification.isPresent()) {
                // Actualizar registro existente
                verification = existingVerification.get();
                verification.setDiditSessionId(webhookData.getSessionId());
                verification.setVerificationStatus(status);
                verification.setVerifiedAt(OffsetDateTime.now());
                verification.setUpdatedAt(OffsetDateTime.now());
                log.info("Updating existing Didit verification for document hash: {}...",
                    documentHash.substring(0, 8));
            } else {
                // Buscar o crear identity_vault
                final DiditWebhookPayload.DocumentData finalDocData = docData;
                com.vozsegura.domain.entity.IdentityVault identityVault =
                    identityVaultRepository.findByDocumentHash(documentHash)
                        .orElseGet(() -> {
                            log.info("Creating new IdentityVault for hash: {}...",
                                documentHash.substring(0, 8));

                            com.vozsegura.domain.entity.IdentityVault newVault =
                                new com.vozsegura.domain.entity.IdentityVault();
                            newVault.setDocumentHash(documentHash);

                            // Cifrar identity blob (JSON con datos de Didit)
                            String identityJson = buildIdentityJson(finalDocData);
                            newVault.setIdentityBlobEncrypted(cryptoService.encryptPII(identityJson));
                            newVault.setKeyVersion(1);

                            return identityVaultRepository.save(newVault);
                        });

                Long identityVaultId = identityVault.getId();

                // Buscar persona por identity_vault_id
                Optional<Persona> persona = personaRepository.findByIdentityVaultId(identityVaultId);

                if (persona.isEmpty()) {
                    // Crear nueva persona
                    log.info("Creating new Persona for identity_vault_id: {}", identityVaultId);

                    Persona nuevaPersona = new Persona();
                    nuevaPersona.setIdentityVaultId(identityVaultId);
                    nuevaPersona.setCedulaHash(documentHash);
                    nuevaPersona.setCedulaEncrypted(documentEncrypted);

                    // Cifrar nombres si estan disponibles
                    if (docData.getFirstName() != null) {
                        nuevaPersona.setPrimerNombreEncrypted(cryptoService.encryptPII(docData.getFirstName()));
                    }
                    if (docData.getLastName() != null) {
                        nuevaPersona.setPrimerApellidoEncrypted(cryptoService.encryptPII(docData.getLastName()));
                    }

                    nuevaPersona.setCreatedAt(OffsetDateTime.now());
                    nuevaPersona.setUpdatedAt(OffsetDateTime.now());

                    personaRepository.save(nuevaPersona);
                    log.info("Created new Persona for identity_vault_id: {}", identityVaultId);
                }

                // Crear nuevo registro de verificacion
                verification = new DiditVerification();
                verification.setIdentityVaultId(identityVaultId);
                verification.setDiditSessionId(webhookData.getSessionId());
                verification.setDocumentNumberHash(documentHash);
                verification.setDocumentNumberEncrypted(documentEncrypted);
                verification.setVerificationStatus(status);
                verification.setVerifiedAt(OffsetDateTime.now());
                verification.setCreatedAt(OffsetDateTime.now());
                verification.setUpdatedAt(OffsetDateTime.now());
                
                log.info("Creating new Didit verification for identity_vault_id: {}", identityVaultId);
            }

            DiditVerification savedVerification = diditVerificationRepository.save(verification);
            log.info("Webhook payload processed successfully");

            return savedVerification;

        } catch (Exception e) {
            log.error("processWebhookPayload: ERROR - {}", e.getMessage(), e);
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
     * Obtiene una verificación por número de documento (usando hash)
     *
     * @param documentNumber Cédula en texto plano (se hashea internamente)
     * @return Optional con DiditVerification si existe
     */
    public Optional<DiditVerification> getVerificationByDocumentNumber(String documentNumber) {
        String documentHash = cryptoService.hashCedula(documentNumber);
        return diditVerificationRepository.findByDocumentNumberHash(documentHash);
    }

    /**
     * Obtiene decisión de sesión directamente desde Didit API
     * Fallback sincrónico para cuando el webhook falla
     */
    public Optional<DiditVerification> getSessionDecisionFromDidit(String sessionId) {
        try {
            log.info("getSessionDecisionFromDidit: Fetching session {} from Didit API", sessionId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Api-Key", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Intentar múltiples endpoints en orden de prioridad
            String[] endpoints = {
                apiUrl + "/v3/verification/" + sessionId + "/",      // Endpoint de verificación (más común para resultados)
                apiUrl + "/v3/session/" + sessionId + "/decision/",  // Endpoint de decisión específico
                apiUrl + "/v3/session/" + sessionId + "/",           // Endpoint de sesión general
                apiUrl + "/v2/verification/" + sessionId + "/",      // Fallback v2 verificación
                apiUrl + "/v2/session/" + sessionId + "/decision/",  // Fallback v2 decisión
                apiUrl + "/v2/session/" + sessionId + "/"            // Fallback v2 general
            };

            for (String url : endpoints) {
                try {
                    log.debug("Trying Didit API endpoint");

                    ResponseEntity<Map> response = restTemplate.exchange(
                        url, HttpMethod.GET, entity, Map.class);

                    log.debug("Didit API response received");

                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        log.info("SUCCESS! Response body keys: {}", response.getBody().keySet());
                        return processDiditDecisionResponse(response.getBody(), sessionId);
                    } else {
                        log.warn("Endpoint {} returned non-success status: {}", url, response.getStatusCode());
                    }
                } catch (org.springframework.web.client.HttpClientErrorException e) {
                    log.warn("Endpoint {} failed: {} - {}", url, e.getStatusCode(), e.getMessage());
                    // Continuar con el siguiente endpoint
                    continue;
                } catch (Exception e) {
                    log.warn("Endpoint {} error: {}", url, e.getMessage());
                    // Continuar con el siguiente endpoint
                    continue;
                }
            }

            log.error("All Didit API endpoints failed for session: {}", sessionId);
            log.error("This usually means:");
            log.error("  1. The session has not been completed by the user yet");
            log.error("  2. The session has expired (Didit sessions typically expire after 24-48 hours)");
            log.error("  3. The session ID is invalid");
            log.error("  4. The API key doesn't have permissions to access this session");
            log.error("  5. The webhook should be delivering this data instead (check webhook URL configuration)");
            log.error("IMPORTANT: If running locally, Didit cannot send webhooks to localhost.");
            log.error("           You need to use ngrok or a similar tool to expose your local server,");
            log.error("           or deploy to a server with a public URL.");

            return Optional.empty();

        } catch (Exception e) {
            log.error("Error fetching session decision from Didit: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Procesa la respuesta de decisión de Didit
     */
    @SuppressWarnings("unchecked")
    private Optional<DiditVerification> processDiditDecisionResponse(Map<String, Object> body, String sessionId) {
        try {
            log.debug("Processing Didit decision response");

            String status = (String) body.get("status");

            if (!"Approved".equals(status) && !"VERIFIED".equals(status) && !"Completed".equals(status)) {
                log.info("Session status not approved, skipping");
                return Optional.empty();
            }
            
            Map<String, Object> documentData = extractDocumentData(body);
            
            if (documentData == null) {
                log.warn("No document_data found in Didit response");
                log.warn("  Available keys in response: {}", body.keySet());
                return Optional.empty();
            }
            
            log.info("  Document data keys: {}", documentData.keySet());

            String personalNumber = (String) documentData.get("personal_number");

            if (personalNumber == null) {
                log.warn("Missing personal_number in Didit response");
                return Optional.empty();
            }
            
            // Generar hash y cifrado
            String documentHash = cryptoService.hashCedula(personalNumber);
            String documentEncrypted = cryptoService.encryptPII(personalNumber);

            // Buscar o crear identity_vault
            com.vozsegura.domain.entity.IdentityVault identityVault =
                identityVaultRepository.findByDocumentHash(documentHash)
                    .orElseGet(() -> {
                        log.info("Creating new IdentityVault from Didit API, hash: {}...",
                            documentHash.substring(0, 8));

                        com.vozsegura.domain.entity.IdentityVault newVault =
                            new com.vozsegura.domain.entity.IdentityVault();
                        newVault.setDocumentHash(documentHash);

                        // Cifrar identity blob
                        String firstName = (String) documentData.get("first_name");
                        String lastName = (String) documentData.get("last_name");
                        String identityJson = String.format(
                            "{\"firstName\":\"%s\",\"lastName\":\"%s\",\"verifiedAt\":\"%s\"}",
                            firstName != null ? firstName : "",
                            lastName != null ? lastName : "",
                            OffsetDateTime.now()
                        );
                        newVault.setIdentityBlobEncrypted(cryptoService.encryptPII(identityJson));
                        newVault.setKeyVersion(1);

                        return identityVaultRepository.save(newVault);
                    });

            Long identityVaultId = identityVault.getId();

            // Buscar persona por identity_vault_id
            Optional<Persona> persona = personaRepository.findByIdentityVaultId(identityVaultId);

            if (persona.isEmpty()) {
                // Crear nueva persona
                log.info("Creating new Persona from Didit API for identity_vault_id: {}", identityVaultId);

                Persona nuevaPersona = new Persona();
                nuevaPersona.setIdentityVaultId(identityVaultId);
                nuevaPersona.setCedulaHash(documentHash);
                nuevaPersona.setCedulaEncrypted(documentEncrypted);

                // Cifrar nombres si estan disponibles
                String firstName = (String) documentData.get("first_name");
                String lastName = (String) documentData.get("last_name");
                if (firstName != null) {
                    nuevaPersona.setPrimerNombreEncrypted(cryptoService.encryptPII(firstName));
                }
                if (lastName != null) {
                    nuevaPersona.setPrimerApellidoEncrypted(cryptoService.encryptPII(lastName));
                }

                nuevaPersona.setCreatedAt(OffsetDateTime.now());
                nuevaPersona.setUpdatedAt(OffsetDateTime.now());

                personaRepository.save(nuevaPersona);
                log.info("Created new Persona for identity_vault_id: {}", identityVaultId);
            }

            // Crear y guardar verificacion
            DiditVerification verification = new DiditVerification();
            verification.setIdentityVaultId(identityVaultId);
            verification.setDiditSessionId(sessionId);
            verification.setDocumentNumberHash(documentHash);
            verification.setDocumentNumberEncrypted(documentEncrypted);
            verification.setVerificationStatus("VERIFIED");
            verification.setVerifiedAt(OffsetDateTime.now());
            verification.setCreatedAt(OffsetDateTime.now());
            verification.setUpdatedAt(OffsetDateTime.now());

            DiditVerification saved = diditVerificationRepository.save(verification);
            
            log.info("Session decision retrieved from Didit API and saved for identity_vault_id: {}",
                identityVaultId);

            return Optional.of(saved);
            
        } catch (Exception e) {
            log.error("Error processing Didit decision response: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Extrae document_data de la respuesta de Didit
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractDocumentData(Map<String, Object> response) {
        log.info("extractDocumentData: Searching for document data...");

        // Intentar en "document_data" directo
        if (response.containsKey("document_data")) {
            log.info("  Found in 'document_data'");
            Object data = response.get("document_data");
            if (data instanceof Map) {
                return (Map<String, Object>) data;
            }
        }
        
        // Intentar en "id_verifications" directo (formato v3 común)
        if (response.containsKey("id_verifications")) {
            log.info("  Checking 'id_verifications' array at root level");
            Object idVerifs = response.get("id_verifications");
            if (idVerifs instanceof java.util.List) {
                java.util.List<?> list = (java.util.List<?>) idVerifs;
                if (!list.isEmpty() && list.get(0) instanceof Map) {
                    log.info("  Found in 'id_verifications[0]'");
                    return (Map<String, Object>) list.get(0);
                }
            }
        }

        // Intentar en "decision" -> "id_verifications" (formato alternativo)
        if (response.containsKey("decision")) {
            log.info("  Checking 'decision' object");
            Object decision = response.get("decision");
            if (decision instanceof Map) {
                Map<String, Object> decisionMap = (Map<String, Object>) decision;

                // Buscar en id_verifications
                if (decisionMap.containsKey("id_verifications")) {
                    Object idVerifs = decisionMap.get("id_verifications");
                    if (idVerifs instanceof java.util.List) {
                        java.util.List<?> list = (java.util.List<?>) idVerifs;
                        if (!list.isEmpty() && list.get(0) instanceof Map) {
                            log.info("  Found in 'decision.id_verifications[0]'");
                            return (Map<String, Object>) list.get(0);
                        }
                    }
                }
            }
        }

        // Intentar en "document_verification" -> "document_data"
        if (response.containsKey("document_verification")) {
            log.info("  Checking 'document_verification' object");
            Object docVerif = response.get("document_verification");
            if (docVerif instanceof Map) {
                Map<String, Object> docVerifMap = (Map<String, Object>) docVerif;
                if (docVerifMap.containsKey("document_data")) {
                    Object data = docVerifMap.get("document_data");
                    if (data instanceof Map) {
                        log.info("  Found in 'document_verification.document_data'");
                        return (Map<String, Object>) data;
                    }
                }
            }
        }
        
        // Intentar en "verification_result" -> "document_data"
        if (response.containsKey("verification_result")) {
            Object verifResult = response.get("verification_result");
            if (verifResult instanceof Map) {
                Map<String, Object> verifResultMap = (Map<String, Object>) verifResult;
                if (verifResultMap.containsKey("document_data")) {
                    Object data = verifResultMap.get("document_data");
                    if (data instanceof Map) {
                        return (Map<String, Object>) data;
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Construye JSON con datos de identidad (sin incluir vendor_data ni biometria).
     */
    private String buildIdentityJson(DiditWebhookPayload.DocumentData docData) {
        try {
            Map<String, Object> identity = new HashMap<>();
            if (docData.getFirstName() != null) {
                identity.put("firstName", docData.getFirstName());
            }
            if (docData.getLastName() != null) {
                identity.put("lastName", docData.getLastName());
            }
            if (docData.getFullName() != null) {
                identity.put("fullName", docData.getFullName());
            }
            identity.put("verifiedAt", OffsetDateTime.now().toString());
            return objectMapper.writeValueAsString(identity);
        } catch (Exception e) {
            log.error("Error building identity JSON", e);
            return "{}";
        }
    }
}
