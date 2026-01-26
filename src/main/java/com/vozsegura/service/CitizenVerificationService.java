package com.vozsegura.service;

import com.vozsegura.client.CivilRegistryClient;
import com.vozsegura.client.OtpClient;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Servicio de verificación de ciudadanos anónimos.
 * 
 * Responsabilidades:
 * - Validar cédula + código dactilar contra Registro Civil externo
 * - Verificar muestras biométricas (huellas, rostro, iris)
 * - Generar y enviar OTP por email
 * - Verificar códigos OTP (segundo factor de autenticación)
 * - Generar hashes SHA-256 del citizenRef para anonimato
 * 
 * Integraciones Externas:
 * - CivilRegistryClient: API de Registro Civil de Costa Rica
 *   * verifyCitizen(cedula, codigoDactilar) → citizenRef único
 *   * verifyBiometric(citizenRef, sampleBytes) → boolean
 *   * getEmailForCitizen(citizenRef) → email para OTP
 * - OtpClient: AWS SES para envío de OTP
 *   * sendOtp(email) → otpId con TTL
 *   * verifyOtp(otpId, code) → boolean
 * 
 * Flujo de Verificación Pública (Denunciantes):
 * 1. verifyCitizen(cedula, codigoDactilar) → obtener citizenRef
 * 2. Optional: verifyBiometric() si requiere segundo nivel
 * 3. sendOtp(citizenRef) → enviar código a email
 * 4. verifyOtp(otpId, otpCode) → validar código ingresado
n * 5. hashCitizenRef(citizenRef) → generar hash SHA-256 anónimo
 * 6. Crear sesión anónima con hash (nunca con citizenRef plain text)\n *
 * SEGURIDAD - ANONIMATO:
 * - citizenRef NUNCA se almacena en base de datos
 * - Siempre se convierte a hash SHA-256 inmediatamente después de verificación
 * - El hash es unidireccional: imposible recuperar ciudadanía desde hash
 * - Email se obtiene dinámicamente, NO se almacena
 * - OTP expira en 5-10 minutos típicamente
 * 
 * Estados de Verificación:
 * - NO_VERIFIED: Antes de verificar identidad
 * - VERIFIED: Pasó verificación de cédula + dactilar
 * - BIOMETRIC_VERIFIED: También pasó verificación biométrica (si aplica)
 * - OTP_VERIFIED: Pasó verificación de OTP (MFA completa)
 * - HASHED: Convertido a hash SHA-256 para anonimato permanente
 * 
 * @author Voz Segura Team
 * @since 2026-01
 * @see CivilRegistryClient
 * @see OtpClient
 */
@Service
public class CitizenVerificationService {

    private final CivilRegistryClient civilRegistryClient;
    private final OtpClient otpClient;

    public CitizenVerificationService(CivilRegistryClient civilRegistryClient, OtpClient otpClient) {
        this.civilRegistryClient = civilRegistryClient;
        this.otpClient = otpClient;
    }

    /**
     * Verifica identidad del ciudadano contra Registro Civil.
     * 
     * Primer paso del flujo de verificación.
     * Valida cédula + código dactilar biométrico contra API externa.
     * 
     * Llamadas Externas:
     * - CivilRegistryClient.verifyCitizen(cedula, codigoDactilar)
     * - Retorna citizenRef único (ej: UUID del Registro Civil)
     * - Retorna null si cédula/código son inválidos
     * 
     * @param cedula número de cédula (9 dígitos sin formatos)
     * @param codigoDactilar código biométrico de huella (001-999 típicamente)
     * @return citizenRef identificador único en Registro Civil
     *         null si validación falla (datos inválidos, usuario no existe)
     * 
     * @see CivilRegistryClient#verifyCitizen(String, String)
     */
    public String verifyCitizen(String cedula, String codigoDactilar) {
        return civilRegistryClient.verifyCitizen(cedula, codigoDactilar);
    }

    /**
     * Verifica la muestra biométrica del ciudadano.
     * 
     * Segundo nivel de verificación (opcional).
     * Valida características biométricas (huella dactilar, rostro, iris, etc.)
     * contra el Registro Civil.
     * 
     * Precondiciones:
     * - citizenRef debe ser válido (de verifyCitizen previo)
     * - sampleBytes es la muestra capturada (ej: imagen de huella, rostro)
     * 
     * @param citizenRef referencia única del Registro Civil (de verifyCitizen)
     * @param sampleBytes datos biométricos crudos a verificar
     * @return true si la muestra biométrica coincide y es válida
     *         false si no coincide, está corrupta, o no es válida
     * 
     * @see CivilRegistryClient#verifyBiometric(String, byte[])
     */
    public boolean verifyBiometric(String citizenRef, byte[] sampleBytes) {
        return civilRegistryClient.verifyBiometric(citizenRef, sampleBytes);
    }

    /**
     * Genera OTP y lo envía por email al ciudadano.
     * 
     * Tercer paso del flujo (segundo factor de autenticación MFA).
     * Obtiene email desde Registro Civil (dinámicamente, NO se almacena).
     * Genera OTP de 6 dígitos con expiración.
     * Envía OTP por AWS SES.
     * 
     * Flujo:
     * 1. Llamar CivilRegistryClient.getEmailForCitizen(citizenRef)
     * 2. Obtener email del ciudadano (solo para este envío)
     * 3. Llamar OtpClient.sendOtp(email)
     * 4. OtpClient genera OTP (ej: "123456")
     * 5. OtpClient envía email
     * 6. Retorna otpId (token de sesión para validar después)
     * 
     * Seguridad:
     * - Email NO se almacena nunca
     * - Se obtiene dinámicamente cada vez
     * - OTP tiene TTL (5-10 minutos típicamente)
     * 
     * @param citizenRef referencia única del Registro Civil (de verifyCitizen)
     * @return otpId token de sesión para validar OTP en paso 4
     *         (típicamente UUID o JWT con TTL incluido)
     * 
     * @see CivilRegistryClient#getEmailForCitizen(String)
     * @see OtpClient#sendOtp(String)
     */
    public String sendOtp(String citizenRef) {
        String email = civilRegistryClient.getEmailForCitizen(citizenRef);
        // No almacenamos el email, solo lo usamos para enviar
        return otpClient.sendOtp(email);
    }

    /**
     * Verifica el código OTP ingresado por el ciudadano.
     * 
     * Cuarto y último paso del flujo de verificación pública (MFA completa).
     * Valida que el código ingresado coincida con el enviado por email.
     * 
     * Validaciones:
     * - otpId debe ser válido (creado por sendOtp previo)
     * - otpId no debe haber expirado (5-10 minutos típicamente)
     * - code debe coincidir exactamente con OTP generado
     * - code puede tener formato con/sin espacios (normalizado por OtpClient)
     * 
     * Flujo después de validación exitosa:
     * - Marcar ciudadano como "OTP_VERIFIED"
     * - Llamar hashCitizenRef() para generar hash anónimo
     * - Crear sesión anónima con hash (NUNCA con citizenRef plain text)
     * - Permitir crear denuncia anónima
     * 
     * @param otpId token de sesión retornado por sendOtp()
     * @param code código OTP ingresado por usuario (6 dígitos ej: "123456")
     * @return true si código es válido y no ha expirado
     *         false si código inválido, expirado, o otpId no encontrado
     * 
     * @see OtpClient#verifyOtp(String, String)
     */
    public boolean verifyOtp(String otpId, String code) {
        return otpClient.verifyOtp(otpId, code);
    }

    /**
     * Genera hash SHA-256 irreversible del citizenRef para anonimato.
     * 
     * CRÍTICO PARA SEGURIDAD: Este hash es el mecanismo de anonimato del sistema.
     * Después de verificar identidad (pasos 1-4), se convierte inmediatamente
     * a hash SHA-256 y el citizenRef plain text se descarta.
     * 
     * Propiedades del Hash:
     * - SHA-256 (256 bits = 64 caracteres hexadecimales)
     * - Determinístico: mismo citizenRef → mismo hash siempre
     * - Unidireccional: IMPOSIBLE recuperar citizenRef desde hash
     * - Colisiones: Probabilidad matemáticamente nula
     * - Fast to compute: <1ms por hash
     * 
     * Uso en Sistema:
     * - Se almacena hash en tabla denuncias (nunca citizenRef)
     * - Se almacena hash en tabla identity_vault
     * - Se usa para tracking anónimo (tracking_id = hash)
     * - Se usa para vincular múltiples denuncias del mismo ciudadano (sin revelar identidad)
     * 
     * Algoritmo:
     * 1. Codificar citizenRef a UTF-8 bytes
     * 2. Aplicar SHA-256
     * 3. Convertir resultado a hexadecimal (64 caracteres)
     * 4. Retornar hash (nunca citizenRef original)
     * 
     * @param citizenRef referencia única del Registro Civil (completamente descartada después)
     * @return hash SHA-256 en formato hexadecimal (64 caracteres)
     *         ej: "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6..."
     * @throws IllegalStateException si SHA-256 no está disponible (nunca debería ocurrir)
     * 
     * Ejemplo uso:
     * <pre>
     * String citizenRef = "uuid-from-civil-registry";
     * String anonymousHash = citizenVerificationService.hashCitizenRef(citizenRef);
     * // anonymousHash se almacena en base de datos
     * // citizenRef se descarta, nunca vuelve a usarse
     * </pre>
     */
    public String hashCitizenRef(String citizenRef) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(citizenRef.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

