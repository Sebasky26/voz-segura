package com.vozsegura.vozsegura.client;

/**
 * Interface para cliente del Registro Civil del Ecuador.
 * 
 * Responsabilidades:
 * - Verificar identidad de ciudadanos (cédula + código dactilar)
 * - Verificar biometría (huella dactilar contra Registro Civil)
 * - Obtener email asociado a identidad verificada
 * 
 * Flujo de uso (en CitizenVerificationService):
 * 
 * PASO 1: Verificación Inicial (UnifiedAuthService Step 1)
 *   civicRegistry.verifyCitizen("1712345678", "A1234B5678") → "CITIZEN-1712345678"
 *   Retorna citizenRef que identifica al ciudadano en el sistema
 * 
 * PASO 2: Verificación Biométrica (UnifiedAuthService Step 3)
 *   civicRegistry.verifyBiometric("CITIZEN-1712345678", sampleBytes) → true/false
 *   Valida huella dactilar contra base de datos del Registro Civil
 * 
 * PASO 3: Obtener Email (para OTP)
 *   civicRegistry.getEmailForCitizen("CITIZEN-1712345678") → "ciudadano@gob.ec"
 *   Obtiene email verificado del Registro Civil para paso de OTP
 * 
 * Formato de cédula ecuatoriana:
 * - 10 dígitos: XXYYZZZZZZZZ
 * - XX: Código de provincia (01-24)
 * - Y: Tipo de identificación (0-5 para personas)
 * - ZZZZZZZZ: Número secuencial
 * - Último dígito: Dígito verificador (calculado por Módulo 10)
 * 
 * Código dactilar:
 * - Formato: LNNNLNNNN (L=letra, N=número)
 * - Emitido por Registro Civil con huella dactilar
 * - Usado para validar que la persona física está presente
 * 
 * Seguridad Zero Trust:
 * - NUNCA confiar en solo cédula + código dactilar
 * - SIEMPRE requerir verificación biométrica adicional
 * - NUNCA retornar datos del Registro Civil sin autenticación MFA
 * 
 * Implementaciones:
 * - MockCivilRegistryClient (dev): Valida formato, permite cédulas de prueba
 * - API Real (prod): Integración con API del Registro Civil del Ecuador
 * 
 * @author Voz Segura Team
 * @since 2026-01
 * 
 * @see CitizenVerificationService - Usa este cliente para verificar ciudadanos
 * @see UnifiedAuthService - Orquesta los 3 pasos de verificación
 * @see MockCivilRegistryClient - Implementación mock para desarrollo
 */
public interface CivilRegistryClient {

    /**
     * Verifica identidad de un ciudadano contra Registro Civil.
     * 
     * Validaciones:
     * - Cédula: 10 dígitos, código de provincia válido (01-24), tipo de ID válido (0-5)
     * - Cédula: Dígito verificador correcto (algoritmo Módulo 10)
     * - Código dactilar: Formato LNNNLNNNN, registrado en Registro Civil
     * - Ciudadano: No tiene antecedentes de fraude
     * 
     * Flujo:
     * 1. Validar formato de cédula (10 dígitos)
     * 2. Validar código de provincia y tipo de ID
     * 3. Validar dígito verificador (Módulo 10)
     * 4. Validar formato de código dactilar (L+N+N+N+L+N+N+N+N)
     * 5. Consultar Registro Civil API
     * 6. Retornar citizenRef o null si falla
     * 
     * @param cedula Cédula de identidad del ciudadano (10 dígitos, ej: "1712345678")
     * @param codigoDactilar Código dactilar emitido por Registro Civil (ej: "A1234B5678")
     * @return citizenRef: ID único del ciudadano en el sistema (ej: "CITIZEN-1712345678")
     *         null: Si cédula, código dactilar, o verificación en Registro Civil falla
     * 
     * @throws IllegalArgumentException si formato de cédula/código dactilar no válido
     */
    String verifyCitizen(String cedula, String codigoDactilar);

    /**
     * Verifica que la biometría (huella) enviada coincida con Registro Civil.
     * 
     * Proceso:
     * 1. Recibir muestra biométrica (bytes de imagen/template de huella)
     * 2. Enviar a servicio biométrico del Registro Civil
     * 3. Comparar con plantilla almacenada
     * 4. Retornar coincidencia (score > umbral de confianza)
     * 
     * Seguridad:
     * - La muestra se envía cifrada (TLS)
     * - La comparación ocurre en servidores del Registro Civil
     * - El sistema NUNCA almacena la muestra (se descarta después)
     * - Solo se registra si hubo coincidencia o no
     * 
     * @param citizenRef Referencia del ciudadano obtenida en verifyCitizen()
     * @param sampleBytes Bytes de la muestra biométrica (imagen/template)
     * @return true si hay coincidencia > umbral de confianza
     *         false si no coincide, está corrupta, o servicio no disponible
     * 
     * @throws RuntimeException si servicio biométrico no está disponible
     */
    boolean verifyBiometric(String citizenRef, byte[] sampleBytes);

    /**
     * Obtiene el email verificado del ciudadano en el Registro Civil.
     * 
     * Usada para:
     * - Enviar OTP por email (Step 2 del MFA)
     * - Enviar confirmación de denuncia
     * - Enviar alertas de seguimiento
     * 
     * Nota:
     * - El email debe ser verificado en el Registro Civil
     * - NUNCA aceptar email del usuario (siempre usar el del Registro Civil)
     * - Si no existe email en Registro Civil, retornar null
     * 
     * @param citizenRef Referencia del ciudadano obtenida en verifyCitizen()
     * @return Email verificado del Registro Civil (ej: "usuario@ciudadano.gob.ec")
     *         null si no hay email registrado
     */
    String getEmailForCitizen(String citizenRef);
}
