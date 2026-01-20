package com.vozsegura.vozsegura.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementacion en memoria de rate limiter (anti-brute-force, anti-DDoS).
 * 
 * Proposito:
 * - Limitar el numero de intentos por IP/usuario en un periodo de tiempo
 * - Prevenir ataques de fuerza bruta (credenciales fallidas)
 * - Prevenir DDoS (demasiadas solicitudes por segundo)
 * - Proteger endpoints sensibles: login, OTP, admin panel
 * 
 * Caracteristicas:
 * - En memoria (no persistente, se pierde al reiniciar)
 * - Thread-safe (usa ConcurrentHashMap y sincronizacion)
 * - Ventanas deslizantes (rolling window)
 * - No registra IPs explicitamente (solo contadores efimeros)
 * 
 * Limites (configurables):
 * - Ventana: 60 segundos (WINDOW_SECONDS)
 * - Maximo de intentos: 30 por ventana (MAX_ATTEMPTS)
 * - Si se alcanzan 30 intentos en 60 segundos: bloquear siguiente
 * 
 * Cuando usar InMemoryRateLimiter:
 * - DESARROLLO: Perfecto para testing local
 * - PRODUCCION: NO recomendado si hay multiples instancias
 *               (cada instancia tiene su propio mapa en memoria)
 *   Usar RedisRateLimiter en produccion (distribuido)
 * 
 * Flujo:
 * 1. UnifiedAuthController.login() es atacado con fuerza bruta
 * 2. Cada intento POST /api/v1/auth/login llama a tryConsume(\"IP:192.168.1.1\")
 * 3. InMemoryRateLimiter incrementa contador de esa IP
 * 4. Si contador > 30 en 60 segundos: retorna false (rechazar)
 * 5. Ventana se reinicia cuando pasa 60 segundos
 * 
 * Sincronizacion:
 * - Map<String, Counter> es ConcurrentHashMap (thread-safe)
 * - Pero cada Counter tiene lock sincronizado (synchronized block)
 * - Evita race conditions al resetear ventana
 * 
 * @see com.vozsegura.vozsegura.security.RateLimiter
 * @see com.vozsegura.vozsegura.controller.UnifiedAuthController
 */
@Component
public class InMemoryRateLimiter implements RateLimiter {

    /**
     * Duracion de ventana de tiempo (en segundos).
     * 
     * Proposito:
     * - Define el periodo en que se cuentan los intentos
     * - Ejemplo: Si es 60, los intentos se resetean cada 60 segundos
     * - Valor: 60 segundos (1 minuto)
     * 
     * Nota: Es una constante, se asume que no cambia en runtime.
     *       Para cambiar, necesitaria usar configuracion externa (SystemConfig)
     */
    private static final int WINDOW_SECONDS = 60;

    /**
     * Numero maximo de intentos permitidos por ventana.
     * 
     * Proposito:
     * - Maximo de intentos dentro de WINDOW_SECONDS
     * - Si se alcanzan MAX_ATTEMPTS intentos: false (rechazar siguiente)
     * - Valor: 30 intentos por minuto
     * 
     * Logica:
     * - Intento 1-30: Aceptados (true)
     * - Intento 31+: Rechazados (false) hasta que pase ventana
     * 
     * Nota: Puede ser demasiado generoso (30/min = 0.5/segundo).
     *       Considerar bajar a 10-15 si hay ataques frecuentes.
     */
    private static final int MAX_ATTEMPTS = 30;

    /**
     * Clase interna para mantener contador y ventana de tiempo.
     * 
     * Proposito:
     * - Agrupar (contador + timestamp de ventana) para cada clave
     * - Permitir sincronizacion independiente por clave
     * 
     * Campos:
     * - count: AtomicInteger (thread-safe, se incrementa sin locks)
     * - windowStart: long (timestamp del inicio de ventana actual)
     * 
     * Nota: No es thread-safe internamente; se sincroniza externamente
     *       en InMemoryRateLimiter.tryConsume()
     */
    private static class Counter {
        /**
         * Contador de intentos en ventana actual.
         * 
         * AtomicInteger es thread-safe para incrementos.
         * Se reinicia a 0 cuando se pasa la ventana.
         */
        AtomicInteger count = new AtomicInteger();

        /**
         * Timestamp del inicio de ventana actual (en segundos).
         * 
         * Se obtiene de Instant.now().getEpochSecond()
         * Cuando (ahora - windowStart) > WINDOW_SECONDS: reiniciar
         */
        long windowStart;
    }

    /**
     * Mapa global de contadores por clave (IP, usuario, email, etc).
     * 
     * Proposito:
     * - Almacenar un Counter independiente por clave
     * - ConcurrentHashMap: acceso thread-safe
     * - Si bien no hay bloqueos en lectura, los writes son sincronizados
     * 
     * Claves tipicas:
     * - "192.168.1.100" (IP)
     * - "1234567890@email.com" (email)
     * - "cedula:1234567890" (cedula de ciudadano)
     * 
     * Nota: En memoria = se pierde cuando app reinicia.
     *       Para persistencia, usar Redis.
     */
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    /**
     * Intenta consumir un intento para una clave determinada.
     * 
     * Proposito:
     * - Verificar si una clave (IP, email, usuario) puede hacer un intento mas
     * - Si aun tiene intentos disponibles en ventana: retorna true
     * - Si ya alcanzo maximo: retorna false (rechazar)
     * 
     * Algoritmo:
     * 1. Obtener timestamp actual (now)
     * 2. Buscar o crear Counter para clave
     * 3. Sincronizar en Counter (para evitar race conditions)
     * 4. Si ventana expiro (now - windowStart > 60): resetear contador
     * 5. Incrementar contador
     * 6. Retornar true si <= 30, false si > 30
     * 
     * @param key Identificador unico (IP, email, usuario) - nunca nulo
     * @return true si aceptado (intento dentro del limite)
     *         false si rechazado (limite alcanzado)
     * 
     * Ejemplo:
     * // Primeros 30 intentos de 192.168.1.1 en 60 segundos
     * rateLimiter.tryConsume(\"192.168.1.1\") // true (intento 1)
     * rateLimiter.tryConsume(\"192.168.1.1\") // true (intento 2)
     * ... (continua true hasta intento 30)
     * rateLimiter.tryConsume(\"192.168.1.1\") // false (intento 31, rechazado)
     * 
     * // Despues de 60 segundos, ventana se resetea
     * rateLimiter.tryConsume(\"192.168.1.1\") // true (nueva ventana, intento 1)
     * 
     * Thread Safety:
     * - ConcurrentHashMap.computeIfAbsent() es thread-safe
     * - synchronized (counter) previene race conditions al resetear ventana
     * - AtomicInteger.incrementAndGet() es atomico
     */
    @Override
    public boolean tryConsume(String key) {
        // Obtener timestamp actual en segundos (desde epoch)
        long now = Instant.now().getEpochSecond();

        // Obtener o crear Counter para esta clave
        // computeIfAbsent es atomico en ConcurrentHashMap
        Counter counter = counters.computeIfAbsent(key, k -> {
            Counter c = new Counter();
            c.windowStart = now;  // Iniciar ventana
            return c;
        });

        // Sincronizar en este Counter para evitar race conditions
        // (multiples threads pueden acceder a diferentes keys, pero mismo Counter)
        synchronized (counter) {
            // Verificar si la ventana expiro
            if (now - counter.windowStart > WINDOW_SECONDS) {
                // Ventana expiro: resetear
                counter.windowStart = now;
                counter.count.set(0);
            }

            // Incrementar contador y retornar true si <= limite
            return counter.count.incrementAndGet() <= MAX_ATTEMPTS;
        }
    }
}
