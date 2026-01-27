package com.medibridge.appointment_service_medibridge.service;

import com.medibridge.appointment_service_medibridge.domain.enums.QueueStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis Queue Cache Service
 * 
 * Manages the "Fast Path" for queue operations using Redis.
 * 
 * Key Patterns:
 * 1. Queue Status: key-value
 * 2. Active Patients: Sorted Set (ZSET)
 * 3. Patient Position: Hash
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisQueueCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${appointment.redis.queue-cache-ttl-hours:24}")
    private int cacheTtlHours;

    private static final String KEY_PREFIX = "queue:";
    private static final String ZSET_WAITING_PREFIX = "queue:waiting:";

    /**
     * Cache queue status
     */
    public void cacheQueueStatus(UUID doctorId, LocalDate date, QueueStatus status) {
        String key = getQueueStatusKey(doctorId, date);
        redisTemplate.opsForValue().set(key, status.name(), cacheTtlHours, TimeUnit.HOURS);
    }

    /**
     * Get cached queue status
     */
    public QueueStatus getCachedQueueStatus(UUID doctorId, LocalDate date) {
        String key = getQueueStatusKey(doctorId, date);
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            return QueueStatus.valueOf(value.toString());
        }
        return null;
    }

    /**
     * Add patient to waiting list (ZSET)
     * Score = priorityWeight + tokenNumber
     * Emergency priority gets a lower score (jumps ahead)
     */
    public void addPatientToWaitingList(UUID queueId, UUID queueEntryId, int tokenNumber, boolean isEmergency) {
        String key = ZSET_WAITING_PREFIX + queueId;

        // Priority weight: Emergency = 0, Normal = 1,000,000
        // Result: Emergency token 1 = 1, Normal token 1 = 1,000,001
        double score = (isEmergency ? 0 : 1_000_000) + tokenNumber;

        redisTemplate.opsForZSet().add(key, queueEntryId.toString(), score);
        redisTemplate.expire(key, cacheTtlHours, TimeUnit.HOURS);
    }

    /**
     * Remove patient from waiting list
     */
    public void removePatientFromWaitingList(UUID queueId, UUID queueEntryId) {
        String key = ZSET_WAITING_PREFIX + queueId;
        redisTemplate.opsForZSet().remove(key, queueEntryId.toString());
    }

    /**
     * Get next patient ID from waiting list
     */
    public UUID getNextPatient(UUID queueId) {
        String key = ZSET_WAITING_PREFIX + queueId;
        Set<Object> range = redisTemplate.opsForZSet().range(key, 0, 0);

        if (range != null && !range.isEmpty()) {
            return UUID.fromString(range.iterator().next().toString());
        }
        return null;
    }

    /**
     * Get patient's position in queue
     */
    public Long getPosition(UUID queueId, UUID queueEntryId) {
        String key = ZSET_WAITING_PREFIX + queueId;
        return redisTemplate.opsForZSet().rank(key, queueEntryId.toString());
    }

    private String getQueueStatusKey(UUID doctorId, LocalDate date) {
        return KEY_PREFIX + doctorId + ":" + date + ":status";
    }
}
