package com.vozsegura.vozsegura.service;

import com.vozsegura.vozsegura.client.CivilRegistryClient;
import com.vozsegura.vozsegura.client.OtpClient;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Servicio de verificación de ciudadanos.
 * Gestiona la validación con el Registro Civil y OTP.
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
     * Verifica al ciudadano con cédula y código dactilar.
     *
     * @return referencia del ciudadano si es válido, null si falla
     */
    public String verifyCitizen(String cedula, String codigoDactilar) {
        return civilRegistryClient.verifyCitizen(cedula, codigoDactilar);
    }

    /**
     * Verifica la muestra biométrica.
     */
    public boolean verifyBiometric(String citizenRef, byte[] sampleBytes) {
        return civilRegistryClient.verifyBiometric(citizenRef, sampleBytes);
    }

    /**
     * Envía OTP al ciudadano.
     *
     * @param citizenRef referencia del ciudadano
     * @return ID del OTP enviado
     */
    public String sendOtp(String citizenRef) {
        String email = civilRegistryClient.getEmailForCitizen(citizenRef);
        // No almacenamos el email, solo lo usamos para enviar
        return otpClient.sendOtp(email);
    }

    /**
     * Verifica el código OTP.
     */
    public boolean verifyOtp(String otpId, String code) {
        return otpClient.verifyOtp(otpId, code);
    }

    /**
     * Genera un hash del citizenRef para almacenamiento.
     * El hash no es reversible.
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

