package com.vozsegura.vozsegura.client.mock;

import com.vozsegura.vozsegura.client.CivilRegistryClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "default"})
public class MockCivilRegistryClient implements CivilRegistryClient {

    @Override
    public String verifyCitizen(String cedula, String codigoDactilar) {
        return "CITIZEN-" + cedula;
    }

    @Override
    public boolean verifyBiometric(String citizenRef, byte[] sampleBytes) {
        return true;
    }

    @Override
    public String getEmailForCitizen(String citizenRef) {
        return citizenRef.toLowerCase() + "@example.com";
    }
}
