package com.vozsegura.security;

/**
 * Interface para Rate Limiter (protección anti-brute-force y DDoS).
 * 
 * Propósito:
 * - Limitar cantidad de peticiones por usuario/IP en período de tiempo
 * - Proteger contra fuerza bruta (login, OTP)
 * - Proteger contra DDoS (amplificación de carga)
 * 
 * Estrategia:
 * - Clave: usuario (ej: cedula), IP, o combinación
 * - Límite: N peticiones por M segundos (ej: 3 intentos de login / 5 min)
 * - Acción: Rechazar, delay, o bloquear temporalmente
 * 
 * Implementaciones:
 * - InMemoryRateLimiter: HashMap en memoria (para DEV)
 * - RedisRateLimiter: Backend Redis (para PROD distribuido)
 * 
 * Métodos:
 * - tryConsume(key): Intentar consumir 1 token
 *   - true: Token disponible, proceder
 *   - false: Límite alcanzado, rechazar
 * 
 * Uso en código:
 * if (rateLimiter.tryConsume("login-cedula-" + cedula)) {
 *    // Proceder con login
 * } else {
 *    // Rechazar: "Demasiados intentos, espere X segundos"
 * }
 * 
 * @author Voz Segura Team
 * @since 2026-01
 * @see InMemoryRateLimiter - Implementación en memoria (DEV)
 */
public interface RateLimiter {

    /**
     * Intenta consumir 1 token del límite de la clave.
     * 
     * Lógica:
     * 1. Buscar contador para clave
     * 2. Si no existe: crear con límite inicial
     * 3. Si contador < límite: incrementar, retornar true
     * 4. Si contador >= límite: retornar false
     * 5. Si timeout expiró: resetear contador
     * 
     * @param key Clave única (ej: "login-cedula-1712345678", "ip-192.168.1.1")
     * @return true si token disponible (proceder), false si límite alcanzado (rechazar)
     */
    boolean tryConsume(String key);
}
