package com.vozsegura.vozsegura.client;

public interface CivilRegistryClient {

    String verifyCitizen(String cedula, String codigoDactilar);

    boolean verifyBiometric(String citizenRef, byte[] sampleBytes);

    String getEmailForCitizen(String citizenRef);
}
