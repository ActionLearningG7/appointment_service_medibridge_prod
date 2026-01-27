package com.medibridge.appointment_service_medibridge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Idempotency Service
 * 
 * Prevents duplicate operations using Redis atomic/transactional capabilities.
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${appointment.redis.idempotency-ttl-hours:24}")
    private int ttlHours;

    /**
     * Try to acquire an idempotency lock
     * 
     * @param key Unique idempotency key
     * @return true if lock acquired (first time), false if already exists
     */
    public boolean acquireLock(String key) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
                "idem:" + key,
                "LOCKED",
                Duration.ofHours(ttlHours));
        return Boolean.TRUE.equals(success);
    }

    /**
     * Release lock (in case of failure)
     */
    public void releaseLock(String key) {
        redisTemplate.delete("idem:" + key);
    }
}
