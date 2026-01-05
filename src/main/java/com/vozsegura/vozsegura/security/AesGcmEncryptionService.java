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

@Service
public class AesGcmEncryptionService implements EncryptionService {

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

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
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
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
            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] cipherBytes = new byte[buffer.remaining()];
            buffer.get(cipherBytes);

            Cipher cipher = Cipher.getInstance(ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, loadKey(), spec);
            byte[] plain = cipher.doFinal(cipherBytes);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failure", e);
        }
    }
}
