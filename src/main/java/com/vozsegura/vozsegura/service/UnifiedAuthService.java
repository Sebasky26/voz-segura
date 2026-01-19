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
     * Paso 3: Validar clave secreta contra el password_hash de staff_user.
     * Acepta tanto texto plano como BCrypt.
     * Si es "NOT_USED_AWS_SECRET", permite pasar la validación.
     */
    public boolean validateSecretKey(StaffUser staffUser, String secretKey) {
        if (staffUser == null) {
            return false;
        }

        String passwordHash = staffUser.getPasswordHash();

        if (passwordHash == null || passwordHash.isEmpty()) {
            return false;
        }
        
        // Si el hash es el marcador "NOT_USED_AWS_SECRET", permitir cualquier input
        // (es un bypass temporal para usuarios sin clave asignada)
        if ("NOT_USED_AWS_SECRET".equals(passwordHash)) {
            System.out.println("✅ User has NOT_USED_AWS_SECRET marker, allowing verification");
            return true;
        }
        
        // Si es un hash BCrypt válido (comienza con $2a$ o $2b$), usar BCrypt
        if (passwordHash.startsWith("$2a$") || passwordHash.startsWith("$2b$")) {
            return BCrypt.checkpw(secretKey, passwordHash);
        }
        
        // Si es texto plano (compatibilidad)
        return passwordHash.equals(secretKey);
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

