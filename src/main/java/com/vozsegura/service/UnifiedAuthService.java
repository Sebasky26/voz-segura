package com.vozsegura.service;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCrypt;

import com.vozsegura.domain.entity.StaffUser;
import com.vozsegura.repo.StaffUserRepository;
import com.vozsegura.security.EncryptionService;

/**
 * Servicio de autenticacion unificada para usuarios del sistema.
 *
 * Tabla: staff.staff_user
 *
 * Flujo de autenticacion:
 * 1. Staff/Admin: username + password (BCrypt) + MFA (OTP via email cifrado)
 * 2. Denunciante: Verificacion Didit (biometrica) via webhook
 *
 * Seguridad:
 * - Staff se identifica por username (citext, case-insensitive)
 * - No se persiste ni busca por cedula de staff
 * - Password hash siempre BCrypt
 * - Email cifrado, se descifra solo para envio de OTP
 * - Logs sin PII, solo hash de username
 *
 * @author Voz Segura Team
 * @since 2026-01
 */
@Slf4j
@Service
public class UnifiedAuthService {

    private final StaffUserRepository staffUserRepository;
    private final CaptchaService captchaService;
    private final AuditService auditService;
    private final EncryptionService encryptionService;

    public UnifiedAuthService(
            StaffUserRepository staffUserRepository,
            CaptchaService captchaService,
            AuditService auditService,
            EncryptionService encryptionService) {
        this.staffUserRepository = staffUserRepository;
        this.captchaService = captchaService;
        this.auditService = auditService;
        this.encryptionService = encryptionService;
    }

    /**
     * Busca StaffUser por username.
     * Username es citext (case-insensitive).
     *
     * @param username Nombre de usuario
     * @return Optional con StaffUser si existe y esta habilitado
     */
    public Optional<StaffUser> findStaffByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return staffUserRepository.findByUsernameAndEnabledTrue(username.trim());
    }

    /**
     * Valida password contra BCrypt hash.
     *
     * Seguridad:
     * - Solo acepta hashes BCrypt validos ($2a$ o $2b$)
     * - Resistente a timing attacks via BCrypt.checkpw()
     * - Logs sin exponer password ni username completo
     *
     * @param staffUser Usuario a validar
     * @param password Password en texto plano
     * @return true si password es correcto
     */
    public boolean validatePassword(StaffUser staffUser, String password) {
        if (staffUser == null || password == null || password.isEmpty()) {
            log.warn("validatePassword: Invalid parameters");
            return false;
        }

        String passwordHash = staffUser.getPasswordHash();
        String userHash = hashUserId(staffUser.getUsername());

        if (passwordHash == null || passwordHash.isEmpty()) {
            log.warn("validatePassword: Password hash is NULL for user [{}]", userHash);
            return false;
        }

        if (!passwordHash.startsWith("$2a$") && !passwordHash.startsWith("$2b$")) {
            log.warn("validatePassword: Invalid hash format for user [{}]", userHash);
            return false;
        }

        try {
            boolean isValid = BCrypt.checkpw(password, passwordHash);
            if (!isValid) {
                log.warn("SECURITY: Invalid password attempt for user [{}]", userHash);
            } else {
                log.info("SECURITY: Valid password for user [{}]", userHash);
            }
            return isValid;
        } catch (IllegalArgumentException e) {
            log.error("validatePassword: BCrypt error for user [{}]", userHash);
            return false;
        }
    }

    /**
     * Obtiene email descifrado del staff para envio de OTP.
     *
     * @param staffUser Usuario staff
     * @return Email en texto plano, o null si no tiene email configurado
     */
    public String getStaffEmail(StaffUser staffUser) {
        if (staffUser == null) {
            return null;
        }

        String emailEncrypted = staffUser.getEmailEncrypted();
        if (emailEncrypted == null || emailEncrypted.isBlank()) {
            log.warn("getStaffEmail: No email configured for user [{}]", hashUserId(staffUser.getUsername()));
            return null;
        }

        try {
            return encryptionService.decryptFromBase64(emailEncrypted);
        } catch (Exception e) {
            log.error("getStaffEmail: Failed to decrypt email for user [{}]", hashUserId(staffUser.getUsername()));
            return null;
        }
    }

    /**
     * Genera hash corto del username para logs seguros.
     * Formato: USR-[8 chars]
     */
    private String hashUserId(String username) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(username.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String b64 = java.util.Base64.getEncoder().encodeToString(hash);
            return "USR-" + b64.substring(0, 8).replaceAll("[+/=]", "X");
        } catch (Exception e) {
            return "USR-" + username.hashCode();
        }
    }

    /**
     * Registra intento de login fallido.
     */
    public void logLoginFailed(String role, String reason) {
        auditService.logEvent(role != null ? role : "UNKNOWN", null, "LOGIN_FAILED", null, reason);
    }

    /**
     * Registra login exitoso.
     */
    public void logLoginSuccess(String role, String username) {
        auditService.logEvent(role, username, "LOGIN_SUCCESS", null, "Acceso exitoso al sistema");
    }

    /**
     * Clasificación de tipos de usuario en el sistema Voz Segura.
     */
    public enum UserType {
        DENUNCIANTE,
        ANALYST,
        ADMIN
    }
}
