package com.vozsegura.vozsegura.client.mock;

import com.vozsegura.vozsegura.client.CivilRegistryClient;
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

    @Override
    public String verifyCitizen(String cedula, String codigoDactilar) {
        System.out.println("[REGISTRO CIVIL] Verificando ciudadano: " + cedula);
        
        // PASO 1: Validación matemática con algoritmo Módulo 10
        if (!validarCedulaEcuatoriana(cedula)) {
            System.err.println("[REGISTRO CIVIL] Cédula rechazada: formato inválido");
            return null;
        }
        
        // PASO 2: Validar código dactilar (formato: 1 letra, 4 números, 1 letra, 4 números)
        if (!validarCodigoDactilar(codigoDactilar)) {
            System.err.println("[REGISTRO CIVIL] Código dactilar inválido: " + codigoDactilar);
            return null;
        }
        
        // PASO 3: Simulación de consulta exitosa al Registro Civil
        String citizenRef = "CITIZEN-" + cedula;
        System.out.println("[REGISTRO CIVIL] Identidad verificada: " + citizenRef);
        System.out.println("[REGISTRO CIVIL] Código dactilar validado correctamente");
        
        return citizenRef;
    }

    @Override
    public boolean verifyBiometric(String citizenRef, byte[] sampleBytes) {
        System.out.println("[REGISTRO CIVIL] Verificación biométrica para: " + citizenRef);
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
     * 
     * Reglas:
     * - Debe tener exactamente 10 dígitos
     * - Los primeros 2 dígitos indican la provincia (01-24)
     * - El tercer dígito debe ser 0-5 (personas naturales)
     * - El décimo dígito es el verificador (Módulo 10)
     * 
     * @param cedula Número de cédula a validar
     * @return true si la cédula es matemáticamente válida
     */
    private boolean validarCedulaEcuatoriana(String cedula) {
        // Validar formato básico
        if (cedula == null || !cedula.matches("\\d{10}")) {
            return false;
        }
        
        try {
            // Validar provincia (01-24)
            int provincia = Integer.parseInt(cedula.substring(0, 2));
            if (provincia < 1 || provincia > 24) {
                return false;
            }
            
            // Validar tercer dígito (tipo de documento)
            int tercerDigito = Integer.parseInt(cedula.substring(2, 3));
            if (tercerDigito > 5) {
                return false;
            }
            
            // Algoritmo Módulo 10
            int suma = 0;
            int digitoVerificador = Integer.parseInt(cedula.substring(9, 10));
            
            for (int i = 0; i < 9; i++) {
                int digito = Integer.parseInt(cedula.substring(i, i + 1));
                
                // Posiciones impares (0, 2, 4, 6, 8) se multiplican por 2
                if (i % 2 == 0) {
                    digito = digito * 2;
                    if (digito > 9) {
                        digito -= 9;
                    }
                }
                suma += digito;
            }
            
            // Calcular dígito verificador esperado
            int modulo = suma % 10;
            int verificadorCalculado = (modulo == 0) ? 0 : 10 - modulo;
            
            boolean esValida = verificadorCalculado == digitoVerificador;
            
            if (!esValida) {
                System.err.println("[REGISTRO CIVIL] Cédula inválida: dígito verificador incorrecto");
            }
            
            return esValida;
            
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Valida el código dactilar con el formato especificado.
     * 
     * Formato esperado: 1 letra (A-Z) + 4 números (1-9) + 1 letra (A-Z) + 4 números (1-9)
     * Ejemplo válido: A1234B5678
     * 
     * Seguridad implementada:
     * - Rechaza valores nulos o vacíos
     * - Valida patrón regex estricto
     * - Rechaza caracteres especiales (prevención de inyección)
     * - Solo permite mayúsculas (normalización)
     * - No permite números 0 en posiciones de dígitos
     * 
     * @param codigoDactilar Código dactilar a validar
     * @return true si el código dactilar cumple el formato especificado
     */
    private boolean validarCodigoDactilar(String codigoDactilar) {
        // VALIDACIÓN 1: No nulo y no vacío
        if (codigoDactilar == null || codigoDactilar.isBlank()) {
            System.err.println("[VALIDACIÓN DACTILAR] Código dactilar nulo o vacío");
            return false;
        }
        
        // VALIDACIÓN 2: Tiene exactamente 10 caracteres
        if (codigoDactilar.length() != 10) {
            System.err.println("[VALIDACIÓN DACTILAR] Longitud inválida: " + codigoDactilar.length() + " (esperado: 10)");
            return false;
        }
        
        // VALIDACIÓN 3: Contiene solo mayúsculas y números (sin caracteres especiales)
        // Previene inyección de caracteres maliciosos (XSS, SQLi, etc.)
        if (!codigoDactilar.matches("[A-Z0-9]{10}")) {
            System.err.println("[VALIDACIÓN DACTILAR] Contiene caracteres inválidos: " + codigoDactilar);
            return false;
        }
        
        // VALIDACIÓN 4: Patrón específico: [A-Z][1-9][1-9][1-9][1-9][A-Z][1-9][1-9][1-9][1-9]
        if (!codigoDactilar.matches("^[A-Z][1-9]{4}[A-Z][1-9]{4}$")) {
            System.err.println("[VALIDACIÓN DACTILAR] Patrón inválido: " + codigoDactilar);
            System.err.println("[VALIDACIÓN DACTILAR] Esperado: 1 letra, 4 números (1-9), 1 letra, 4 números (1-9)");
            return false;
        }
        
        System.out.println("[VALIDACIÓN DACTILAR] ✓ Código dactilar válido: " + codigoDactilar);
        return true;
    }
}
