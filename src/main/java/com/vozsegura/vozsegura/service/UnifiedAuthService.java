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
 * Servicio de autenticación unificada para todos los usuarios del sistema.
 * 
 * Gestiona el flujo de autenticación MFA (Multi-Factor Authentication) de 5 pasos
 * para Staff y Admin, acceso simple para denunciantes públicos.
 * 
 * Responsabilidades:
 * - Verificar identidad contra Registro Civil (cédula + código dactilar biométrico)
 * - Clasificar tipo de usuario (DENUNCIANTE, ANALYST, ADMIN)
 * - Validar secretos (contraseña hash BCrypt o texto plano)
 * - Generar y verificar códigos OTP por email (AWS SES)
 * - Mantener sesión MFA entre verificaciones
 * 
 * Integraciones Externas:
 * - CivilRegistryClient: API de Registro Civil de Costa Rica
 * - OtpClient: AWS SES para envío de códigos de verificación
 * - StaffUserRepository: Base de datos de usuarios staff/admin
 * - CaptchaService: Turnstile reCAPTCHA validation
 * 
 * FLUJO DE AUTENTICACIÓN PARA STAFF/ADMIN (5 pasos MFA):
 * 1. verifyCitizenIdentity() → Validar cédula + código dactilar contra Registro Civil
 * 2. getUserType() → Determinar clasificación de usuario (DENUNCIANTE/ANALYST/ADMIN)
 * 3. validateSecretKey() → Verificar contraseña contra hash BCrypt
 * 4. sendEmailOtp() → Generar OTP y enviar por email (AWS SES)
 * 5. verifyOtp() → Validar código OTP ingresado por usuario
 * 
 * FLUJO PARA DENUNCIANTES:
 * - Solo paso 1 (verificación Registro Civil)
 * - Sin MFA, sin contraseña, sin OTP
 * - Genera sesión anónima con SHA-256 hash de cédula
 * 
 * TOKENS Y SESIONES:
 * - citizenRef: Referencia única del Registro Civil (pasos 1-2)
 * - otpToken: Token de sesión MFA (pasos 4-5), típicamente JWT o UUID con TTL
 * - Sesión HTTP: Mantiene identidad entre requests
 * 
 * @author Voz Segura Team
 * @since 2026-01
 * @see CivilRegistryClient
 * @see OtpClient
 * @see StaffUserRepository
 */
@Slf4j
@Service
public class UnifiedAuthService {

    private final CivilRegistryClient civilRegistryClient;
    private final SecretsManagerClient secretsManagerClient;
    private final StaffUserRepository staffUserRepository;
    private final CaptchaService captchaService;
    private final OtpClient otpClient;
    private final AuditService auditService;

    public UnifiedAuthService(
            CivilRegistryClient civilRegistryClient,
            SecretsManagerClient secretsManagerClient,
            StaffUserRepository staffUserRepository,
            CaptchaService captchaService,
            OtpClient otpClient,
            AuditService auditService) {
        this.civilRegistryClient = civilRegistryClient;
        this.secretsManagerClient = secretsManagerClient;
        this.staffUserRepository = staffUserRepository;
        this.captchaService = captchaService;
        this.otpClient = otpClient;
        this.auditService = auditService;
    }

    /**
     * Paso 1: Verifica identidad contra Registro Civil de Costa Rica.
     * 
     * Valida cédula y código dactilar biométrico de la persona.
     * Este es el primer paso del flujo MFA (Multi-Factor Auth) y se ejecuta ANTES
     * de validar contraseña u OTP.
     * 
     * Precondiciones:
     * - Turnstile reCAPTCHA ya validado en el controlador
     * - Cédula formato válido (9 dígitos)
     * - Código dactilar obtenido de escaneo biométrico o entrada manual
     * 
     * Proceso:
     * 1. Llamar API externa CivilRegistryClient.verifyCitizen()
     * 2. API retorna citizenRef (identificador único en Registro Civil)
     * 3. Si retorna null, significa datos inválidos → throw SecurityException
     * 4. Retornar citizenRef para paso 2
     * 
     * @param cedula número de cédula de identidad (9 dígitos sin formatos)
     * @param codigoDactilar código biométrico de la huella digital (típicamente 001-999)
     * @return citizenRef identificador único en Registro Civil (UUID o similar)
     * @throws SecurityException si cédula o código dactilar son inválidos
     *         (usuario no existe, datos incorrectos, etc.)
     * 
     * @see CivilRegistryClient#verifyCitizen(String, String)
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
     * Paso 2: Clasifica el tipo de usuario en el sistema.
     * 
     * Determina si el usuario es denunciante público, analista de casos o administrador.
     * Se ejecuta DESPUÉS de verificar identidad (paso 1).
     * 
     * Lógica de clasificación:
     * - Si existe en tabla staff_user con role=ADMIN → retorna ADMIN
     * - Si existe en tabla staff_user con role=ANALYST → retorna ANALYST
     * - Si NO existe en staff_user → retorna DENUNCIANTE (público anónimo)
     * 
     * Diferencias de acceso según tipo:
     * - DENUNCIANTE: Solo puede crear denuncias (sin MFA)
     * - ANALYST: Accede a panel staff, puede revisar y derivar casos (requiere MFA)
     * - ADMIN: Acceso total, configuración del sistema (requiere MFA)
     * 
     * @param cedula número de cédula de identidad (ya verificada en paso 1)
     * @return UserType.DENUNCIANTE si no es staff
     *         UserType.ANALYST si es staff con role=ANALYST
     *         UserType.ADMIN si es staff con role=ADMIN
     * 
     * @see UserType
     * @see StaffUserRepository#findByCedulaAndEnabledTrue(String)
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
     * Paso 3: Valida contraseña del usuario Staff/Admin contra BCrypt hash.
     * 
     * Verifica que el secretKey (contraseña) ingresado coincida con el password_hash
     * almacenado en base de datos. Usa BCrypt para comparación criptográfica segura.
     * 
     * Seguridad REFORZADA (v2.1):
     * - SOLO acepta hashes BCrypt válidos ($2a$ o $2b$)
     * - Usa BCrypt.checkpw() para comparación resistente a timing attacks
     * - Logging detallado de intentos fallidos para auditoría
     * - Rechaza cualquier hash que NO sea BCrypt (requiere reset de password)
     * - NO permite bypasses ni texto plano
     *
     * Casos de validación:
     * - staffUser es null → retorna false + log warning
     * - passwordHash es null o vacío → retorna false + log warning
     * - passwordHash NO es BCrypt ($2a$/$2b$) → retorna false + log error
     * - passwordHash es BCrypt válido → valida con BCrypt.checkpw()
     *
     * @param staffUser entidad StaffUser del usuario a validar (nullable)
     * @param secretKey contraseña en plain text ingresada por usuario
     * @return true si contraseña es válida (BCrypt match)
     *         false en cualquier otro caso (usuario no existe, sin contraseña, formato inválido, password incorrecto)
     *
     * @throws IllegalArgumentException si BCrypt hash está corrupto
     *
     * Ejemplo uso:
     * <pre>
     * boolean isValid = unifiedAuthService.validateSecretKey(staffUser, "mi_pass_123");
     * if (!isValid) {
     *     throw new SecurityException("Contraseña incorrecta o formato inválido");
     * }
     * </pre>
     *
     * Nota: Si un usuario tiene un hash inválido (no BCrypt), debe realizar reset de password.
     */
    public boolean validateSecretKey(StaffUser staffUser, String secretKey) {
        if (staffUser == null) {
            log.warn("SECURITY: validateSecretKey called with null staffUser");
            return false;
        }

        String passwordHash = staffUser.getPasswordHash();

        if (passwordHash == null || passwordHash.isEmpty()) {
            log.warn("SECURITY: User {} has no password hash configured", staffUser.getUsername());
            return false;
        }
        
        // Validación SOLO con BCrypt (hash válido $2a$ o $2b$)
        if (passwordHash.startsWith("$2a$") || passwordHash.startsWith("$2b$")) {
            try {
                boolean isValid = BCrypt.checkpw(secretKey, passwordHash);
                if (!isValid) {
                    log.warn("SECURITY: Invalid password attempt for user {}", staffUser.getUsername());
                }
                return isValid;
            } catch (IllegalArgumentException e) {
                log.error("SECURITY: BCrypt validation error for user {}: {}",
                         staffUser.getUsername(), e.getMessage());
                return false;
            }
        }
        
        // Si el hash NO es BCrypt válido, rechazar
        log.error("SECURITY: User {} has invalid password hash format (not BCrypt). Password reset required.",
                 staffUser.getUsername());
        return false;
    }

    /**
     * Obtiene la dirección de email del usuario Staff/Admin.
     * 
     * El email es necesario para enviar códigos OTP (One-Time Password) en el
     * paso 4 del flujo MFA. Se usa para validación de segundo factor.
     * 
     * Búsqueda:
     * - Cédula debe ser válida (ya verificada en pasos 1-3)
     * - Usuario debe estar habilitado (enabled=true en base de datos)
     * - Email se obtiene de la columna email en tabla staff_user
     * 
     * @param cedula número de cédula de staff user
     * @return email del usuario Staff/Admin
     *         null si usuario no existe, está deshabilitado, o sin email configurado
     * 
     * @see StaffUserRepository#findByCedulaAndEnabledTrue(String)
     */
    public String getStaffEmail(String cedula) {
        Optional<StaffUser> staffUser = staffUserRepository.findByCedulaAndEnabledTrue(cedula);
        return staffUser.map(StaffUser::getEmail).orElse(null);
    }

    /**
     * Paso 4: Genera código OTP y lo envía por email al usuario.
     * 
     * Crea un One-Time Password (código de 6 dígitos) con expiración
     * y lo envía por correo electrónico usando AWS SES (Simple Email Service).
     * Este es el segundo factor de autenticación para Staff/Admin.
     * 
     * Precondiciones:
     * - Email debe ser válido y no nulo
     * - Usuario debe tener email configurado en base de datos
     * - AWS SES debe estar configurado y autorizado
     * 
     * Proceso:
     * 1. Validar email no es null o en blanco
     * 2. Llamar OtpClient.sendOtp(email)
     * 3. OtpClient genera código OTP (ej: "123456")
     * 4. OtpClient envía email con código y expiración (típicamente 5-10 min)
     * 5. OtpClient retorna otpToken (token de sesión)
     * 6. Retornar otpToken para paso 5
     * 
     * Efectos secundarios:
     * - Email enviado a buzón del usuario
     * - Token de sesión creado en OTP service (TTL 5-10 minutos típicamente)
     * - Logs de envío registrados en AWS CloudWatch
     * 
     * @param email dirección de correo electrónico del usuario
     * @return otpToken token de sesión para validar OTP en paso 5
     *         (típicamente UUID o JWT con TTL incluido)
     * @throws SecurityException si email es null, en blanco, o no configurado
     * 
     * @see OtpClient#sendOtp(String)
     */
    public String sendEmailOtp(String email) {
        if (email == null || email.isBlank()) {
            throw new SecurityException("Email no configurado para este usuario");
        }
        return otpClient.sendOtp(email);
    }

    /**
     * Paso 5: Valida código OTP ingresado por usuario contra el enviado por email.
     * 
     * Verifica que el código OTP (ej: "123456") ingresado por el usuario
     * coincida con el código generado y enviado al email en paso 4.
     * Este es el último paso del flujo MFA (Multi-Factor Authentication).
     * 
     * Validaciones:
     * - otpToken debe ser válido (creado en paso 4)
     * - otpToken no debe haber expirado (típicamente 5-10 minutos)
     * - otpCode debe coincidir exactamente con código generado
     * - otpCode puede tener formato con/sin espacios (normalizado por OtpClient)
     * 
     * Precondiciones:
     * - sendEmailOtp() debe haber sido llamado exitosamente
     * - Usuario debe haber recibido email con código OTP
     * - Usuario debe ingresar código antes de que expire (típicamente 5 min)
     * 
     * Flujo después de validación:
     * - Si true: Crear sesión auténtica, permitir acceso a panel staff
     * - Si false: Rechazar acceso, permitir reintentos (con rate limiting)
     * 
     * @param otpToken token de sesión retornado en paso 4 (sendEmailOtp)
     * @param otpCode código OTP ingresado por usuario (6 dígitos ej: "123456")
     * @return true si código es válido y no ha expirado
     *         false si código inválido, expirado, o token no encontrado
     * 
     * @see OtpClient#verifyOtp(String, String)
     */
    public boolean verifyOtp(String otpToken, String otpCode) {
        boolean result = otpClient.verifyOtp(otpToken, otpCode);
        if (result) {
            auditService.logEvent("STAFF", null, "LOGIN_SUCCESS", null, "OTP verificado correctamente");
        } else {
            auditService.logEvent("STAFF", null, "LOGIN_FAILED", null, "OTP inválido o expirado");
        }
        return result;
    }

    /**
     * Registra un intento de login fallido en el sistema de auditoría.
     *
     * @param role Rol del usuario (ADMIN, ANALYST, CITIZEN)
     * @param reason Razón del fallo
     */
    public void logLoginFailed(String role, String reason) {
        auditService.logEvent(role != null ? role : "UNKNOWN", null, "LOGIN_FAILED", null, reason);
    }

    /**
     * Registra un login exitoso en el sistema de auditoría.
     *
     * @param role Rol del usuario
     * @param username Username o identificador del usuario (será hasheado)
     */
    public void logLoginSuccess(String role, String username) {
        auditService.logEvent(role, username, "LOGIN_SUCCESS", null, "Acceso exitoso al sistema");
    }

    /**
     * Clasificación de tipos de usuario en el sistema Voz Segura.
     * 
     * Define tres roles con niveles de acceso y requisitos de autenticación diferente.
     * Se determina en paso 2 del flujo de autenticación (getUserType).
     * 
     * Roles:
     * - DENUNCIANTE: Ciudadano anónimo que crea denuncias (sin MFA, sin contraseña)
     * - ANALYST: Personal de institución que revisa y deriva casos (requiere MFA)
     * - ADMIN: Administrador con acceso total a configuración (requiere MFA)
     */
    public enum UserType {
        DENUNCIANTE,
        ANALYST,
        ADMIN
    }
    
}

