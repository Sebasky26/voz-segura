package com.vozsegura.vozsegura.security;

public interface EncryptionService {

    String encryptToBase64(String plaintext);

    String decryptFromBase64(String ciphertextBase64);
}
