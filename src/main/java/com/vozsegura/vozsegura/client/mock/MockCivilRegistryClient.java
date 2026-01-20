package com.vozsegura.vozsegura.client.mock;

import com.vozsegura.vozsegura.client.CivilRegistryClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Implementación Mock del Registro Civil del Ecuador con validación real.
 * 
 * Propósito:
 * - Permitir desarrollo y testing sin depender de API del Registro Civil
 * - Simular comportamiento de servicio real con validaciones reales
 * - Validar formato de cédula ecuatoriana (algoritmo Módulo 10)
 * - Permitir cédulas de prueba específicas para diferentes roles
 * 
 * Diferencias con producción:
 * - NO consulta base de datos real del Registro Civil
 * - NO realiza verif icación facial/biométrica real
 * - Usa cédulas de prueba permitidas en desarrollo
 * 
 * Validaciones implementadas (reales):
 * - Algoritmo Módulo 10 para validación de cédula ecuatoriana
 * - Validación de código provincial (01-24)
 * - Validación de tipo de identificación (0-5 para personas)
 * - Validación de formato de código dactilar
 * 
 * Flujo de uso:
 * 
 * 1. Verificación de identidad:
 *    civicRef = mockRegistry.verifyCitizen("1712345678", "A1234B5678")
 *    → true si cédula + código dactilar válidos
 *    → Retorna "CITIZEN-1712345678"
 * 
 * 2. Verificación biométrica (mock):
 *    resultado = mockRegistry.verifyBiometric("CITIZEN-1712345678", sampleBytes)
 *    → Siempre retorna true en dev (sin validación real)
 * 
 * 3. Obtener email:
 *    email = mockRegistry.getEmailForCitizen("CITIZEN-1712345678")
 *    → Mock: genera email basado en cédula
 * 
 * Cédulas de prueba permitidas en desarrollo:
 * - "1234567890": Rol ADMIN
 * - "0987654321": Rol ANALYST  
 * - "1712345678": Ciudadano común Pichincha
 * - "0912345674": Ciudadano común Guayas
 * 
 * Configuración:
 * - Activo en profiles: "dev", "default" (solo desarrollo)
 * - En producción: reemplazar por CivilRegistryClientImpl (API real)
 * - Spring inyecta esta clase si profile es dev
 * 
 * @author Voz Segura Team
 * @version 2.0 - 2026
 */
@Component
@Profile({"dev", "default"})
public class MockCivilRegistryClient implements CivilRegistryClient {

    /**
     * Verifica identidad de ciudadano contra validaciones reales.
     * 
     * Validaciones:
     * 1. Cédula válida según algoritmo Módulo 10 ecuatoriano
     *    O es una cédula de prueba permitida en desarrollo
     * 
     * 2. Código dactilar válido (formato + estructura)
     * 
     * 3. Retornar citizenRef único o null si falla
     * 
     * Flujo:
     * 1. Validar formato de cédula (10 dígitos, Módulo 10, provincia, tipo ID)
     * 2. Si es cédula de prueba en dev, permitir
     * 3. Validar formato de código dactilar (LNNNNLNNNN)
     * 4. Si todo válido, retornar "CITIZEN-" + cédula
     * 5. Si alguna validación falla, retornar null
     * 
     * @param cedula Cédula ecuatoriana (10 dígitos)
     * @param codigoDactilar Código dactilar (formato: LNNNNLNNNN)
     * @return "CITIZEN-" + cedula si válida, null si no
     */
    @Override
    public String verifyCitizen(String cedula, String codigoDactilar) {
        // PASO 1: Validación matemática con algoritmo Módulo 10
        // En desarrollo, también permitimos cédulas de prueba específicas
        if (!validarCedulaEcuatoriana(cedula) && !esCedulaDePrueba(cedula)) {
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

    /**
     * Verifica si es una cédula de prueba permitida en desarrollo.
     * 
     * Cédulas de prueba:
     * - Están configuradas en base de datos con roles específicos
     * - Admin: "1234567890", "0987654321"
     * - Citizen: "1712345678" (Pichincha), "0912345674" (Guayas)
     * 
     * Estas cédulas se usan para testing manual y desarrollo.
     * En producción, solo se aceptan cédulas válidas por Módulo 10.
     * 
     * @param cedula Cédula a verificar
     * @return true si es cédula de prueba permitida en dev
     */
    private boolean esCedulaDePrueba(String cedula) {
        // Lista de cédulas de prueba permitidas solo en desarrollo
        return cedula != null && (
            cedula.equals("1234567890") ||    // admin de prueba
            cedula.equals("0987654321") ||    // analista de prueba
            cedula.equals("1712345678") ||    // cédula de prueba válida Pichincha
            cedula.equals("0912345674")       // cédula de prueba válida Guayas
        );
    }

    /**
     * Verifica biometría (mock: siempre retorna true en desarrollo).
     * 
     * En producción:
     * - Enviar muestra biométrica a API de Registro Civil
     * - Validar contra plantilla almacenada
     * - Retornar score > umbral de confianza
     * 
     * En desarrollo:
     * - Simular éxito (true) para permitir testing end-to-end
     * - NUNCA usar esto en producción (aceptaría cualquier biometría)
     * 
     * @param citizenRef Referencia del ciudadano verificado
     * @param sampleBytes Bytes de la muestra biométrica
     * @return true siempre en dev (mock), score>umbral en prod
     */
    @Override
    public boolean verifyBiometric(String citizenRef, byte[] sampleBytes) {
        // En desarrollo, siempre aceptamos
        // En producción, enviar a API de verificación facial
        return true;
    }

    /**
     * Obtiene email del ciudadano (mock: genera basado en cédula).
     * 
     * En producción:
     * - Obtener email verificado del Registro Civil
     * - NUNCA aceptar email del usuario
     * - Email debe ser verificado y estar en registro
     * 
     * En desarrollo:
     * - Generar email mock basado en cédula
     * - Formato: "<cedula-sin-digitos>@ciudadano.gob.ec"
     * 
     * @param citizenRef Referencia del ciudadano
     * @return Email (mock en dev, real en prod)
     */
    @Override
    public String getEmailForCitizen(String citizenRef) {
        // Mock: generar email basado en referencia
        return citizenRef.toLowerCase().replace("citizen-", "") + "@ciudadano.gob.ec";
    }

    /**
     * Valida una cédula ecuatoriana usando el algoritmo Módulo 10.
     * 
     * Estructura de cédula ecuatoriana (10 dígitos):
     * - Posiciones 0-1: Código de provincia (01-24)
     * - Posición 2: Tipo de identificación (0-5 para personas)
     * - Posiciones 3-8: Número secuencial
     * - Posición 9: Dígito verificador (calculado por Módulo 10)
     * 
     * Algoritmo Módulo 10:
     * 1. Multiplicar cada dígito 1-9 por: 1,2,1,2,1,2,1,2,1
     * 2. Si resultado > 9, restar 9
     * 3. Sumar todos los resultados
     * 4. Sacar módulo 10
     * 5. Dígito verificador = 10 - módulo (o 0 si módulo = 0)
     * 
     * @param cedula Cédula a validar (10 dígitos)
     * @return true si pasa todas las validaciones, false si no
     */
    private boolean validarCedulaEcuatoriana(String cedula) {
        if (cedula == null || !cedula.matches("\\d{10}")) {
            return false;
        }
        
        try {
            // Validar código de provincia (01-24)
            int provincia = Integer.parseInt(cedula.substring(0, 2));
            if (provincia < 1 || provincia > 24) {
                return false;
            }
            
            // Validar tipo de identificación (0-5 para personas)
            int tercerDigito = Integer.parseInt(cedula.substring(2, 3));
            if (tercerDigito > 5) {
                return false;
            }
            
            // Calcular y validar dígito verificador (Módulo 10)
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
     * Valida el código dactilar emitido por Registro Civil.
     * 
     * Formato requerido: LNNNNLNNNN
     * - L: Letra mayúscula (A-Z)
     * - N: Número del 1-9 (nunca 0 en código dactilar)
     * 
     * Ejemplo válido: "A1234B5678"
     * Ejemplos inválidos:
     * - "A1234B0678" (número 0 no permitido)
     * - "a1234b5678" (letras minúsculas)
     * - "1A234B567" (estructura incorrecta)
     * 
     * @param codigoDactilar Código a validar
     * @return true si formato es correcto, false si no
     */
    private boolean validarCodigoDactilar(String codigoDactilar) {
        if (codigoDactilar == null || codigoDactilar.isBlank()) {
            return false;
        }
        
        // Debe tener exactamente 10 caracteres
        if (codigoDactilar.length() != 10) {
            return false;
        }
        
        // Todos deben ser mayúsculas o números
        if (!codigoDactilar.matches("[A-Z0-9]{10}")) {
            return false;
        }
        
        // Validar estructura específica: L+NNNN+L+NNNN (sin 0)
        return codigoDactilar.matches("^[A-Z][1-9]{4}[A-Z][1-9]{4}$");
    }
}
