package com.medibridge.appointment_service_medibridge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Distributed Lock Service
 * 
 * Uses Redis (Redisson) to provide distributed locking.
 * Prevents race conditions in critical operations like:
 * - Allocating unique token numbers
 * - Managing queue state transitions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {

    private final RedissonClient redissonClient;

    /**
     * Execute a task with a distributed lock
     * 
     * @param lockKey Unique key for the lock
     * @param task    Key to execute
     * @return Result of the task
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> task) {
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Try to acquire lock for 5 seconds, lease for 10 seconds
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);

            if (isLocked) {
                try {
                    return task.get();
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("Failed to acquire lock: {}", lockKey);
                throw new RuntimeException("System busy, please try again");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operation interrupted", e);
        }
    }

    /**
     * Execute a void task with a distributed lock
     */
    public void executeWithLock(String lockKey, Runnable task) {
        executeWithLock(lockKey, () -> {
            task.run();
            return null;
        });
    }
}
