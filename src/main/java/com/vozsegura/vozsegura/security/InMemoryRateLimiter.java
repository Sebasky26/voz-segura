package com.vozsegura.vozsegura.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementación simple en memoria.
 * No registra IPs de forma persistente; solo contadores efímeros.
 */
@Component
public class InMemoryRateLimiter implements RateLimiter {

    private static final int WINDOW_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 30;

    private static class Counter {
        AtomicInteger count = new AtomicInteger();
        long windowStart;
    }

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    @Override
    public boolean tryConsume(String key) {
        long now = Instant.now().getEpochSecond();
        Counter counter = counters.computeIfAbsent(key, k -> {
            Counter c = new Counter();
            c.windowStart = now;
            return c;
        });
        synchronized (counter) {
            if (now - counter.windowStart > WINDOW_SECONDS) {
                counter.windowStart = now;
                counter.count.set(0);
            }
            return counter.count.incrementAndGet() <= MAX_ATTEMPTS;
        }
    }
}
