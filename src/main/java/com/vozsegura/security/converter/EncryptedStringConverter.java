package com.vozsegura.security.converter;

import com.vozsegura.security.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter para cifrado automático de strings en BD.
 *
 * Uso en entidades:
 * ```java
 * @Column(name = "nombre_encrypted", columnDefinition = "text")
 * @Convert(converter = EncryptedStringConverter.class)
 * private String nombre;
 * ```
 *
 * Comportamiento:
 * - convertToDatabaseColumn(): Cifra con AES-256-GCM antes de guardar
 * - convertToEntityAttribute(): Descifra al recuperar de BD
 * - Transparente: La aplicación trabaja con texto plano
 *
 * Seguridad:
 * - Utiliza EncryptionService configurado
 * - IV aleatorio por cada operación
 * - AEAD: detecta manipulación
 * - Si descifrado falla, retorna "[CIFRADO CORRUPTO]"
 *
 * @author Voz Segura Team
 * @since 2026-01-22
 */
@Slf4j
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static EncryptionService encryptionService;

    @Autowired
    public void setEncryptionService(EncryptionService service) {
        EncryptedStringConverter.encryptionService = service;
    }

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }

        try {
            String encrypted = encryptionService.encryptToBase64(plaintext);
            log.trace("Cifrado exitoso (longitud: {} → {})", plaintext.length(), encrypted.length());
            return encrypted;
        } catch (Exception e) {
            log.error("Error cifrando dato para BD", e);
            throw new IllegalStateException("Fallo en cifrado de dato sensible", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) {
            return null;
        }

        // Si ya es texto plano (migración incompleta), retornar as-is
        if (encryptedBase64.length() < 100 && !encryptedBase64.contains("==")) {
            log.warn("Dato en texto plano detectado en BD (longitud: {})", encryptedBase64.length());
            return encryptedBase64;
        }

        try {
            String decrypted = encryptionService.decryptFromBase64(encryptedBase64);
            log.trace("Descifrado exitoso (longitud: {} → {})", encryptedBase64.length(), decrypted.length());
            return decrypted;
        } catch (Exception e) {
            log.error("Error descifrando dato desde BD (longitud: {})", encryptedBase64.length(), e);
            return "[CIFRADO CORRUPTO]";
        }
    }
}
