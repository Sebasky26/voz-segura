package com.vozsegura.vozsegura.security;

/**
 * Interface para servicio de encriptación.
 * 
 * Propósito:
 * - Abstracción de algoritmo de encriptación (permite cambiar sin tocar código)
 * - Encriptar texto plano a Base64 (para guardar en BD)
 * - Desencriptar Base64 de vuelta a texto plano
 * 
 * Implementaciones:
 * - AesGcmEncryptionService: AES-256-GCM (recomendado, AEAD)
 * - Futuros: ChaCha20-Poly1305, RSA (asimétrico)
 * 
 * Uso:
 * - Encriptar denuncias: texto plano → AES-256-GCM → Base64 → guardar en BD
 * - Desencriptar denuncias: Base64 → AES-256-GCM → texto plano → mostrar a admin
 * 
 * Seguridad:
 * - Clave de 256 bits (32 bytes) obtenida de AWS Secrets Manager
 * - IV aleatorio de 12 bytes (por cada encriptación)
 * - Tag authentication de 128 bits (AEAD - authenticated encryption)
 * - NO es reversible sin clave (imposible descifrar sin key)
 * 
 * @author Voz Segura Team
 * @since 2026-01
 * @see AesGcmEncryptionService - Implementación recomendada
 */
public interface EncryptionService {

    /**
     * Encripta texto plano a Base64.
     * 
     * Proceso:
     * 1. Generar IV aleatorio (12 bytes)
     * 2. Inicializar cipher AES-256-GCM
     * 3. Encriptar texto
     * 4. Generar tag de autenticación (128 bits)
     * 5. Combinar: IV + ciphertext + tag
     * 6. Encodear a Base64
     * 
     * @param plaintext Texto a encriptar
     * @return Base64-encoded: IV (12B) + ciphertext + auth-tag (16B)
     * @throws IllegalStateException si clave no disponible
     * @throws RuntimeException si encriptación falla
     */
    String encryptToBase64(String plaintext);

    /**
     * Desencripta Base64 a texto plano.
     * 
     * Proceso:
     * 1. Decodear Base64
     * 2. Extraer IV (primeros 12 bytes)
     * 3. Extraer ciphertext + auth-tag
     * 4. Inicializar cipher AES-256-GCM con IV
     * 5. Desencriptar y verificar tag
     * 6. Retornar texto plano
     * 
     * @param ciphertextBase64 Base64-encoded ciphertext con IV
     * @return Texto plano desencriptado
     * @throws IllegalStateException si clave no disponible
     * @throws RuntimeException si desencriptación falla (wrong key o corrupted)
     */
    String decryptFromBase64(String ciphertextBase64);
}
