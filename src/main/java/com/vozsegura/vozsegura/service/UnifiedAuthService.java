package com.vozsegura.vozsegura.service;

import com.vozsegura.vozsegura.client.CivilRegistryClient;
import com.vozsegura.vozsegura.client.SecretsManagerClient;
import com.vozsegura.vozsegura.domain.entity.StaffUser;
import com.vozsegura.vozsegura.repo.StaffUserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Servicio de autenticación unificada para todos los usuarios.
 * Todos los usuarios (denunciantes, staff, admin) están registrados en el Registro Civil.
 */
@Service
public class UnifiedAuthService {

    private final CivilRegistryClient civilRegistryClient;
    private final SecretsManagerClient secretsManagerClient;
    private final StaffUserRepository staffUserRepository;
    private final CaptchaService captchaService;

    public UnifiedAuthService(
            CivilRegistryClient civilRegistryClient,
            SecretsManagerClient secretsManagerClient,
            StaffUserRepository staffUserRepository,
            CaptchaService captchaService) {
        this.civilRegistryClient = civilRegistryClient;
        this.secretsManagerClient = secretsManagerClient;
        this.staffUserRepository = staffUserRepository;
        this.captchaService = captchaService;
    }

    /**
     * Paso 1: Verificar cédula y código dactilar contra Registro Civil.
     * Retorna el citizenRef si es válido.
     */
    public String verifyCitizenIdentity(String cedula, String codigoDactilar, String captcha, String sessionId) {
        // Validar CAPTCHA
        if (!captchaService.validateCaptcha(sessionId, captcha)) {
            throw new SecurityException("CAPTCHA inválido");
        }

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
     * Paso 3: Validar clave secreta de AWS Secrets Manager (solo para Staff/Admin).
     */
    public boolean validateSecretKey(String cedula, String secretKey) {
        // Obtener la clave secreta desde AWS Secrets Manager
        String expectedSecretKey = secretsManagerClient.getSecretString("STAFF_SECRET_KEY_" + cedula);

        if (expectedSecretKey == null) {
            return false;
        }

        return expectedSecretKey.equals(secretKey);
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

