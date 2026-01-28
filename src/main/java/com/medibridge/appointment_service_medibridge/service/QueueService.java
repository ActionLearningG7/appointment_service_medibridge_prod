package com.medibridge.appointment_service_medibridge.service;

import com.medibridge.appointment_service_medibridge.domain.entity.VirtualQueue;
import com.medibridge.appointment_service_medibridge.domain.enums.ActionType;
import com.medibridge.appointment_service_medibridge.domain.enums.QueueStatus;
import com.medibridge.appointment_service_medibridge.domain.repository.VirtualQueueRepository;
import com.medibridge.appointment_service_medibridge.kafka.event.BaseEvent;
import com.medibridge.appointment_service_medibridge.kafka.producer.DomainEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {

    private final VirtualQueueRepository virtualQueueRepository;
    private final RedisQueueCacheService redisQueueCacheService;
    private final DomainEventProducer domainEventProducer;
    private final AuditService auditService;
    private final DistributedLockService distributedLockService;
    private final WebSocketQueueService webSocketQueueService;

    @Value("${appointment.queue.default-avg-consultation-minutes:15}")
    private int defaultAvgConsultationMinutes;

    @Value("${appointment.kafka.topics.queue}")
    private String queueTopic;

    /**
     * Open a queue for a doctor (today or future)
     */
    @Transactional
    public VirtualQueue openQueue(UUID doctorId, LocalDate date, UUID openedBy) {
        // Use lock to prevent duplicate queue creation
        String lockKey = "lock:queue:create:" + doctorId + ":" + date;

        return distributedLockService.executeWithLock(lockKey, () -> {
            Optional<VirtualQueue> existing = virtualQueueRepository.findByDoctorIdAndQueueDate(doctorId, date);

            if (existing.isPresent()) {
                VirtualQueue queue = existing.get();
                if (queue.getStatus() == QueueStatus.CLOSED) {
                    throw new IllegalStateException("Cannot reopen a closed queue");
                }
                return queue;
            }

            VirtualQueue queue = VirtualQueue.builder()
                    .doctorId(doctorId)
                    .queueDate(date)
                    .status(QueueStatus.OPEN)
                    .avgConsultationMinutes(defaultAvgConsultationMinutes)
                    .currentToken(0)
                    .openedAt(LocalDateTime.now())
                    .openedBy(openedBy)
                    .build();

            VirtualQueue savedQueue = virtualQueueRepository.save(queue);

            // Update Redis cache
            redisQueueCacheService.cacheQueueStatus(doctorId, date, QueueStatus.OPEN);

            // Audit
            auditService.logAction(ActionType.QUEUE_OPENED, "QUEUE", savedQueue.getId(),
                    "Queue opened for doctor " + doctorId + " on " + date, null);

            // Publish Event
            publishQueueEvent(savedQueue, "QueueOpened");

            // Broadcast WebSocket update
            webSocketQueueService.broadcastQueueStatusChange(savedQueue.getId());

            return savedQueue;
        });
    }

    /**
     * Get or create active queue for today
     */
    @Transactional(readOnly = true)
    public VirtualQueue getQueueForToday(UUID doctorId) {
        return virtualQueueRepository.findByDoctorIdAndQueueDate(doctorId, LocalDate.now())
                .orElseThrow(() -> new RuntimeException("No queue found for doctor today"));
    }

    /**
     * Pause Queue
     */
    @Transactional
    public void pauseQueue(UUID queueId) {
        VirtualQueue queue = getQueue(queueId);
        queue.setStatus(QueueStatus.PAUSED);
        virtualQueueRepository.save(queue);

        redisQueueCacheService.cacheQueueStatus(queue.getDoctorId(), queue.getQueueDate(), QueueStatus.PAUSED);
        auditService.logAction(ActionType.QUEUE_PAUSED, "QUEUE", queueId, "Queue paused", null);
        publishQueueEvent(queue, "QueuePaused");

        // Broadcast WebSocket update
        webSocketQueueService.broadcastQueueStatusChange(queueId);
    }

    /**
     * Resume Queue
     */
    @Transactional
    public void resumeQueue(UUID queueId) {
        VirtualQueue queue = getQueue(queueId);
        queue.setStatus(QueueStatus.OPEN);
        virtualQueueRepository.save(queue);

        redisQueueCacheService.cacheQueueStatus(queue.getDoctorId(), queue.getQueueDate(), QueueStatus.OPEN);
        auditService.logAction(ActionType.QUEUE_OPENED, "QUEUE", queueId, "Queue resumed", null);
        publishQueueEvent(queue, "QueueResumed");

        // Broadcast WebSocket update
        webSocketQueueService.broadcastQueueStatusChange(queueId);
    }

    /**
     * Close Queue
     */
    @Transactional
    public void closeQueue(UUID queueId, UUID closedBy) {
        VirtualQueue queue = getQueue(queueId);
        queue.setStatus(QueueStatus.CLOSED);
        queue.setClosedAt(LocalDateTime.now());
        queue.setClosedBy(closedBy);
        virtualQueueRepository.save(queue);

        redisQueueCacheService.cacheQueueStatus(queue.getDoctorId(), queue.getQueueDate(), QueueStatus.CLOSED);
        auditService.logAction(ActionType.QUEUE_CLOSED, "QUEUE", queueId, "Queue closed", null);
        publishQueueEvent(queue, "QueueClosed");

        // Broadcast WebSocket update
        webSocketQueueService.broadcastQueueStatusChange(queueId);
    }

    public VirtualQueue getQueue(UUID queueId) {
        return virtualQueueRepository.findById(queueId)
                .orElseThrow(() -> new RuntimeException("Queue not found: " + queueId));
    }

    /**
     * Get all active queues for today (for admin monitoring)
     */
    @Transactional(readOnly = true)
    public List<VirtualQueue> getAllActiveQueuesForToday() {
        return virtualQueueRepository.findActiveQueuesForToday();
    }

    /**
     * Get all queues for a specific date (for admin monitoring)
     */
    @Transactional(readOnly = true)
    public List<VirtualQueue> getAllQueuesForDate(LocalDate date) {
        return virtualQueueRepository.findAll().stream()
                .filter(q -> q.getQueueDate().equals(date))
                .toList();
    }

    private void publishQueueEvent(VirtualQueue queue, String eventType) {
        BaseEvent event = BaseEvent.builder()
                .eventType(eventType)
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .aggregateId(queue.getId().toString())
                .build();

        domainEventProducer.publish(queueTopic, "QUEUE", queue.getId(), event);
    }
}
