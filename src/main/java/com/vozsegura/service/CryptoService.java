package com.vozsegura.service;

import com.vozsegura.security.EncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Servicio centralizado de criptografía para el sistema Voz Segura.
 *
 * Responsabilidades:
 * - Generación de hashes SHA-256 para identificación anónima (cédulas, emails)
 * - Delegación de cifrado/descifrado AES-256-GCM a EncryptionService
 * - Estandarización del formato de hash (Hex, 64 caracteres)
 *
 * SEGURIDAD CRÍTICA:
 * - Todos los hashes usan formato Hexadecimal (64 chars) para consistencia con BD
 * - NUNCA almacenar datos PII en texto plano
 * - Los hashes son unidireccionales (imposible recuperar valor original)
 *
 * @author Voz Segura Team
 * @since 2026-01
 */
@Slf4j
@Service
public class CryptoService {

    private final EncryptionService encryptionService;

    public CryptoService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    /**
     * Genera hash SHA-256 de una cédula en formato Hexadecimal.
     *
     * Uso:
     * - Identificación anónima de denunciantes
     * - Búsqueda en registro_civil.personas.cedula_hash
     * - Búsqueda en staff.staff_user.cedula_hash_idx
     * - Búsqueda en didit_verification.document_number_hash
     *
     * @param cedula Número de cédula en texto plano
     * @return Hash SHA-256 en formato Hex (64 caracteres)
     * @throws RuntimeException si el algoritmo SHA-256 no está disponible
     */
    public String hashCedula(String cedula) {
        if (cedula == null || cedula.isBlank()) {
            throw new IllegalArgumentException("Cédula no puede ser nula o vacía");
        }
        return generateSHA256Hex(cedula.trim());
    }

    /**
     * Genera hash SHA-256 de un email en formato Hexadecimal.
     * El email se normaliza a minúsculas y sin espacios.
     *
     * @param email Email en texto plano
     * @return Hash SHA-256 en formato Hex (64 caracteres)
     */
    public String hashEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email no puede ser nulo o vacío");
        }
        return generateSHA256Hex(email.trim().toLowerCase());
    }

    /**
     * Genera hash SHA-256 del nombre completo para búsquedas.
     *
     * @param primerNombre Primer nombre
     * @param segundoNombre Segundo nombre (puede ser null)
     * @param primerApellido Primer apellido
     * @param segundoApellido Segundo apellido (puede ser null)
     * @return Hash SHA-256 en formato Hex (64 caracteres)
     */
    public String hashNombreCompleto(String primerNombre, String segundoNombre,
                                      String primerApellido, String segundoApellido) {
        String nombreCompleto = String.join(" ",
                nullToEmpty(primerNombre),
                nullToEmpty(segundoNombre),
                nullToEmpty(primerApellido),
                nullToEmpty(segundoApellido)
        ).trim().replaceAll("\\s+", " ");

        return generateSHA256Hex(nombreCompleto);
    }

    /**
     * Cifra un valor PII usando AES-256-GCM.
     *
     * @param plaintext Texto a cifrar
     * @return Base64-encoded ciphertext (IV + ciphertext + auth-tag)
     */
    public String encryptPII(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return null;
        }
        return encryptionService.encryptToBase64(plaintext);
    }

    /**
     * Descifra un valor PII usando AES-256-GCM.
     *
     * @param ciphertextBase64 Base64-encoded ciphertext
     * @return Texto plano descifrado
     */
    public String decryptPII(String ciphertextBase64) {
        if (ciphertextBase64 == null || ciphertextBase64.isBlank()) {
            return null;
        }
        return encryptionService.decryptFromBase64(ciphertextBase64);
    }

    /**
     * Genera hash SHA-256 en formato Hexadecimal (64 caracteres).
     * Este es el formato estándar usado en toda la base de datos.
     *
     * @param input Cadena de texto a hashear
     * @return Hash en formato Hex (64 caracteres, minúsculas)
     */
    private String generateSHA256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convertir bytes a Hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Error generando hash SHA-256", e);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
