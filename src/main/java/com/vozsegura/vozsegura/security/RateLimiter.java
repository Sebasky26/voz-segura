package com.vozsegura.vozsegura.security;

public interface RateLimiter {

    boolean tryConsume(String key);
}
