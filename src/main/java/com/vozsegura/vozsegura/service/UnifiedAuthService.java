package com.vozsegura.vozsegura.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCrypt;

import com.vozsegura.vozsegura.client.CivilRegistryClient;
import com.vozsegura.vozsegura.client.OtpClient;
import com.vozsegura.vozsegura.client.SecretsManagerClient;
import com.vozsegura.vozsegura.domain.entity.StaffUser;
import com.vozsegura.vozsegura.repo.StaffUserRepository;

/**
 * Servicio de autenticación unificada para todos los usuarios.
 * Todos los usuarios (denunciantes, staff, admin) están registrados en el Registro Civil.
 * 
 * FLUJO MFA PARA STAFF/ADMIN:
 * 1. Verificar cédula + código dactilar (Registro Civil)
 * 2. Verificar clave secreta (AWS Secrets Manager)
 * 3. Enviar OTP por email (AWS SES) - MFA
 * 4. Verificar OTP ingresado
 */
@Service
public class UnifiedAuthService {

    private final CivilRegistryClient civilRegistryClient;
    private final SecretsManagerClient secretsManagerClient;
    private final StaffUserRepository staffUserRepository;
    private final CaptchaService captchaService;
    private final OtpClient otpClient;

    public UnifiedAuthService(
            CivilRegistryClient civilRegistryClient,
            SecretsManagerClient secretsManagerClient,
            StaffUserRepository staffUserRepository,
            CaptchaService captchaService,
            OtpClient otpClient) {
        this.civilRegistryClient = civilRegistryClient;
        this.secretsManagerClient = secretsManagerClient;
        this.staffUserRepository = staffUserRepository;
        this.captchaService = captchaService;
        this.otpClient = otpClient;
    }

    /**
     * Paso 1: Verificar cédula y código dactilar contra Registro Civil.
     * La validación de Turnstile se realiza en el controlador antes de este paso.
     * Retorna el citizenRef si es válido.
     */
    public String verifyCitizenIdentity(String cedula, String codigoDactilar) {
        // Verificar contra Registro Civil (API Externa)
        String citizenRef = civilRegistryClient.verifyCitizen(cedula, codigoDactilar);
        if (citizenRef == null) {
            throw new SecurityException("Identificación inválida. Verifique sus datos.");
        }

        return citizenRef;
    }

    /**
     * Paso 2: Verificar si el ciudadano es Staff/Admin.
     * Retorna el tipo de usuario: "DENUNCIANTE", "ANALYST", "ADMIN"
     */
    public UserType getUserType(String cedula) {
        // Buscar si existe en la tabla staff_user
        Optional<StaffUser> staffUser = staffUserRepository.findByCedulaAndEnabledTrue(cedula);

        if (staffUser.isPresent()) {
            String role = staffUser.get().getRole();
            if ("ADMIN".equals(role)) {
                return UserType.ADMIN;
            } else if ("ANALYST".equals(role)) {
                return UserType.ANALYST;
            }
        }

        return UserType.DENUNCIANTE;
    }

    /**
     * Paso 3: Validar contraseña del usuario contra password_hash en la base de datos (solo para Staff/Admin).
     * Si el hash es "NOT_USED_AWS_SECRET", busca en AWS Secrets Manager.
     * Si es un hash BCrypt válido, usa BCrypt.
     */
    public boolean validateSecretKey(String cedula, String secretKey) {
        // Obtener el usuario de staff desde la base de datos
        Optional<StaffUser> staffUser = staffUserRepository.findByCedulaAndEnabledTrue(cedula);

        if (!staffUser.isPresent()) {
            System.out.println("[AUTH DEBUG] Staff user not found for cedula: " + cedula);
            return false;
        }

        // Validar la contraseña contra el hash usando BCrypt
        String passwordHash = staffUser.get().getPasswordHash();
        System.out.println("[AUTH DEBUG] Password hash from DB: " + (passwordHash != null ? passwordHash.substring(0, Math.min(30, passwordHash.length())) + "..." : "NULL"));
        System.out.println("[AUTH DEBUG] Secret key length: " + secretKey.length());
        
        if (passwordHash == null || passwordHash.isEmpty()) {
            System.out.println("[AUTH DEBUG] Password hash is null or empty!");
            return false;
        }
        
        // Si el hash dice "NOT_USED_AWS_SECRET", buscar en AWS Secrets Manager
        if (passwordHash.contains("NOT_USED_AWS_SECRET")) {
            System.out.println("[AUTH DEBUG] Using AWS Secrets Manager for validation");
            String expectedSecretKey = secretsManagerClient.getSecretString("STAFF_SECRET_KEY_" + cedula);
            if (expectedSecretKey != null && expectedSecretKey.equals(secretKey)) {
                System.out.println("[AUTH DEBUG] AWS Secrets Manager validation: SUCCESS");
                return true;
            } else {
                System.out.println("[AUTH DEBUG] AWS Secrets Manager validation: FAILED");
                return false;
            }
        }
        
        // Si es un hash BCrypt válido (comienza con $2a$ o $2b$), usar BCrypt
        if (passwordHash.startsWith("$2a$") || passwordHash.startsWith("$2b$")) {
            System.out.println("[AUTH DEBUG] Using BCrypt for validation");
            boolean isValid = BCrypt.checkpw(secretKey, passwordHash);
            System.out.println("[AUTH DEBUG] BCrypt validation result: " + isValid);
            return isValid;
        }
        
        // Si es texto plano (compatibilidad)
        System.out.println("[AUTH DEBUG] Using plaintext comparison (legacy)");
        boolean isValid = passwordHash.equals(secretKey);
        System.out.println("[AUTH DEBUG] Plaintext comparison result: " + isValid);
        return isValid;
    }

    /**
     * Obtiene el email del staff user para envío de OTP.
     */
    public String getStaffEmail(String cedula) {
        Optional<StaffUser> staffUser = staffUserRepository.findByCedulaAndEnabledTrue(cedula);
        return staffUser.map(StaffUser::getEmail).orElse(null);
    }

    /**
     * Paso 4: Enviar OTP por email usando AWS SES.
     * Retorna el token de sesión para verificar el OTP posteriormente.
     */
    public String sendEmailOtp(String email) {
        if (email == null || email.isBlank()) {
            throw new SecurityException("Email no configurado para este usuario");
        }
        return otpClient.sendOtp(email);
    }

    /**
     * Paso 5: Verificar el código OTP ingresado.
     */
    public boolean verifyOtp(String otpToken, String otpCode) {
        return otpClient.verifyOtp(otpToken, otpCode);
    }

    /**
     * Tipos de usuario en el sistema.
     */
    public enum UserType {
        DENUNCIANTE,
        ANALYST,
        ADMIN
    }
    
}

