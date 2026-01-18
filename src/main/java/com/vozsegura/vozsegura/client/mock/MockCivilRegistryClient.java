package com.vozsegura.vozsegura.client.mock;

import com.vozsegura.vozsegura.client.CivilRegistryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Implementación Mock del Registro Civil con validación real de cédula ecuatoriana.
 * 
 * Incluye:
 * - Algoritmo Módulo 10 para validación matemática de cédulas
 * - Simulación de verificación de identidad
 * - Simulación de verificación biométrica
 * 
 * En producción, reemplazar por integración con API del Registro Civil del Ecuador.
 * 
 * @author Voz Segura Team
 * @version 2.0 - 2026
 */
@Component
@Profile({"dev", "default"})
public class MockCivilRegistryClient implements CivilRegistryClient {

    private static final Logger log = LoggerFactory.getLogger(MockCivilRegistryClient.class);

    @Override
    public String verifyCitizen(String cedula, String codigoDactilar) {
        // PASO 1: Validación matemática con algoritmo Módulo 10
        if (!validarCedulaEcuatoriana(cedula)) {
            return null;
        }
        
        // PASO 2: Validar código dactilar (formato: 1 letra, 4 números, 1 letra, 4 números)
        if (!validarCodigoDactilar(codigoDactilar)) {
            return null;
        }
        
        // PASO 3: Simulación de consulta exitosa al Registro Civil
        String citizenRef = "CITIZEN-" + cedula;
        return citizenRef;
    }

    @Override
    public boolean verifyBiometric(String citizenRef, byte[] sampleBytes) {
        // En desarrollo, siempre aceptamos
        // En producción, enviar a API de verificación facial
        return true;
    }

    @Override
    public String getEmailForCitizen(String citizenRef) {
        // Mock: generar email basado en referencia
        return citizenRef.toLowerCase().replace("citizen-", "") + "@ciudadano.gob.ec";
    }

    /**
     * Valida una cédula ecuatoriana usando el algoritmo Módulo 10.
     */
    private boolean validarCedulaEcuatoriana(String cedula) {
        if (cedula == null || !cedula.matches("\\d{10}")) {
            return false;
        }
        
        try {
            int provincia = Integer.parseInt(cedula.substring(0, 2));
            if (provincia < 1 || provincia > 24) {
                return false;
            }
            
            int tercerDigito = Integer.parseInt(cedula.substring(2, 3));
            if (tercerDigito > 5) {
                return false;
            }
            
            int suma = 0;
            int digitoVerificador = Integer.parseInt(cedula.substring(9, 10));
            
            for (int i = 0; i < 9; i++) {
                int digito = Integer.parseInt(cedula.substring(i, i + 1));
                if (i % 2 == 0) {
                    digito = digito * 2;
                    if (digito > 9) {
                        digito -= 9;
                    }
                }
                suma += digito;
            }
            
            int modulo = suma % 10;
            int verificadorCalculado = (modulo == 0) ? 0 : 10 - modulo;
            
            return verificadorCalculado == digitoVerificador;

        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Valida el código dactilar con el formato especificado.
     * Formato: 1 letra (A-Z) + 4 números (1-9) + 1 letra (A-Z) + 4 números (1-9)
     */
    private boolean validarCodigoDactilar(String codigoDactilar) {
        if (codigoDactilar == null || codigoDactilar.isBlank()) {
            return false;
        }
        
        if (codigoDactilar.length() != 10) {
            return false;
        }
        
        if (!codigoDactilar.matches("[A-Z0-9]{10}")) {
            return false;
        }
        
        return codigoDactilar.matches("^[A-Z][1-9]{4}[A-Z][1-9]{4}$");
    }
}
