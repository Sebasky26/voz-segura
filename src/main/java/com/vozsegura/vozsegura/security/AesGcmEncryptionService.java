package com.vozsegura.vozsegura.security;

import com.vozsegura.vozsegura.client.SecretsManagerClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Implementación de EncryptionService usando AES-256-GCM (AEAD).
 * 
 * Algorit mo: AES-256-GCM (Advanced Encryption Standard, 256-bit key, Galois/Counter Mode).
 * 
 * Parámetros:
 * - Algoritmo: AES/GCM/NoPadding (no padding porque GCM es stream cipher)
 * - Clave: 256 bits (32 bytes) desde AWS Secrets Manager
 * - IV: 12 bytes aleatorios por cada encriptación (secureRandom)
 * - Tag de autenticación: 128 bits (detecta manipulación)
 * - Encoding: Base64 para guardar en BD (ASCII-safe)
 * 
 * Ventajas de GCM:
 * - AEAD: Authenticated Encryption with Associated Data
 * - Detecta si ciphertext fue modificado (integrity check)
 * - Hardware acceleration en CPUs modernas (rápido)
 * - No requiere MAC separado (todo en uno)
 * 
 * Seguridad:
 * - IV nunca se reutiliza (aleatorio cada vez)
 * - Clave obtenida de AWS Secrets Manager (nunca hardcoded)
 * - Tag verificado automáticamente en desencriptación
 * - Si alguien modifica ciphertext, desencriptación falla
 * 
 * Formato del ciphertext:
 * Base64(IV(12B) + ciphertext + authTag(16B))
 * 
 * Fallos:
 * - Wrong key: RuntimeException en desencriptación
 * - Corrupted ciphertext: RuntimeException
 * - Missing key: IllegalStateException
 * 
 * @author Voz Segura Team
 * @since 2026-01
 * @see EncryptionService - Interface implementada
 */
@Service
public class AesGcmEncryptionService implements EncryptionService {

    private static final String ALGO = "AES/GCM/NoPadding";
    
    /** IV length en bytes (configurable, default: 12 bytes = estándar AES-GCM) */
    @Value("${encryption.gcm.iv-length:12}")
    private int ivLengthBytes;
    
    /** Tag length en bits (configurable, default: 128 bits = estándar AES-GCM) */
    @Value("${encryption.gcm.tag-length:128}")
    private int tagLengthBits;

    private final SecretsManagerClient secretsManagerClient;
    private final String secretName;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmEncryptionService(SecretsManagerClient secretsManagerClient,
                                   @Value("${voice.encryption-key-secret-name}") String secretName) {
        this.secretsManagerClient = secretsManagerClient;
        this.secretName = secretName;
    }

    private SecretKey loadKey() {
        String b64 = secretsManagerClient.getSecretString(secretName);
        if (b64 == null || b64.isBlank()) {
            throw new IllegalStateException("Encryption key not configured");
        }
        byte[] keyBytes = Base64.getDecoder().decode(b64);
        if (keyBytes.length != 32) {
            throw new IllegalStateException("Expected 256-bit key");
        }
        return new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public String encryptToBase64(String plaintext) {
        try {
            byte[] iv = new byte[ivLengthBytes];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(tagLengthBits, iv);
            cipher.init(Cipher.ENCRYPT_MODE, loadKey(), spec);
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failure", e);
        }
    }

    @Override
    public String decryptFromBase64(String ciphertextBase64) {
        try {
            byte[] all = Base64.getDecoder().decode(ciphertextBase64);
            ByteBuffer buffer = ByteBuffer.wrap(all);
            byte[] iv = new byte[ivLengthBytes];
            buffer.get(iv);
            byte[] cipherBytes = new byte[buffer.remaining()];
            buffer.get(cipherBytes);

            Cipher cipher = Cipher.getInstance(ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(tagLengthBits, iv);
            cipher.init(Cipher.DECRYPT_MODE, loadKey(), spec);
            byte[] plain = cipher.doFinal(cipherBytes);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failure", e);
        }
    }
}
