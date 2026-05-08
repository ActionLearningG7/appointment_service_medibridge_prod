package com.medibridge.appointment_service_medibridge.service;

import com.medibridge.appointment_service_medibridge.domain.entity.Appointment;
import com.medibridge.appointment_service_medibridge.domain.entity.QueueEntry;
import com.medibridge.appointment_service_medibridge.domain.entity.VirtualQueue;
import com.medibridge.appointment_service_medibridge.domain.enums.*;
import com.medibridge.appointment_service_medibridge.domain.repository.QueueEntryRepository;
import com.medibridge.appointment_service_medibridge.kafka.event.BaseEvent;
import com.medibridge.appointment_service_medibridge.kafka.producer.DomainEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.medibridge.appointment_service_medibridge.util.UUIDConverter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueEntryService {

    private final QueueEntryRepository queueEntryRepository;
    private final QueueService queueService;
    private final AppointmentService appointmentService;
    private final DistributedLockService distributedLockService;
    private final RedisQueueCacheService redisQueueCacheService;
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;
    private final DomainEventProducer domainEventProducer;
    private final WebSocketQueueService webSocketQueueService;

    @Value("${appointment.kafka.topics.queue}")
    private String queueTopic;

    /**
     * Get all entries for a queue (ordered by priority and join time)
     */
    @Transactional(readOnly = true)
    public List<QueueEntry> getEntriesForQueue(UUID queueId) {
        return queueEntryRepository.findByQueueIdOrderByTokenNumberAsc(queueId);
    }

    /**
     * Join a queue
     */
    @Transactional
    public QueueEntry joinQueue(UUID appointmentId, UUID patientId, boolean isEmergency) {
        Appointment appointment = appointmentService.getAppointment(appointmentId);
        VirtualQueue queue = queueService.getQueueForToday(appointment.getDoctorId());

        if (queue.getStatus() == QueueStatus.CLOSED) {
            throw new IllegalStateException("Queue is closed");
        }

        // Idempotency check
        String idemKey = "join:" + patientId + ":" + queue.getId();
        if (!idempotencyService.acquireLock(idemKey)) {
            // Already processing or joined
            return queueEntryRepository
                    .findByPatientIdAndStatusIn(patientId, java.util.List.of(QueueEntryStatus.WAITING))
                    .orElseThrow(() -> new IllegalStateException("Duplicate join request detected"));
        }

        // Lock queue for token generation
        String lockKey = "lock:queue:join:" + queue.getId();

        return distributedLockService.executeWithLock(lockKey, () -> {
            try {
                // Generate secure sequential token
                Integer nextToken = queueEntryRepository.findMaxTokenNumber(queue.getId()) + 1;

                QueueEntry entry = QueueEntry.builder()
                        .queueId(queue.getId())
                        .appointmentId(appointmentId)
                        .patientId(patientId)
                        .doctorId(queue.getDoctorId())
                        .tokenNumber(nextToken)
                        .priority(isEmergency ? Priority.EMERGENCY : Priority.NORMAL)
                        .status(QueueEntryStatus.WAITING)
                        .joinedAt(LocalDateTime.now())
                        .build();

                // Calculate ETA
                long patientsAhead = queueEntryRepository.countPatientsAhead(queue.getId(), nextToken);
                int waitTimeMinutes = (int) patientsAhead * queue.getAvgConsultationMinutes();
                entry.setEstimatedStartTime(LocalDateTime.now().plusMinutes(waitTimeMinutes));

                QueueEntry saved = queueEntryRepository.save(entry);

                // Update Appointment
                appointmentService.updateStatus(appointmentId, AppointmentStatus.QUEUED);

                // Update Redis ZSET
                redisQueueCacheService.addPatientToWaitingList(queue.getId(), saved.getId(), nextToken, isEmergency);

                // Audit
                auditService.logAction(ActionType.QUEUE_JOINED, "QUEUE_ENTRY", saved.getId(),
                        "Patient joined queue. Token: " + nextToken, null);

                // Publish Event
                publishEvent(saved, "QueueJoined");

                // Broadcast WebSocket update
                webSocketQueueService.broadcastQueueEntryUpdate(queue.getId());

                return saved;

            } catch (Exception e) {
                idempotencyService.releaseLock(idemKey);
                throw e;
            }
        });
    }

    /**
     * Call next patient
     */
    @Transactional
    public QueueEntry callNextPatient(UUID queueId) {
        // Lock to prevent race condition on calling next
        String lockKey = "lock:queue:call:" + queueId;

        return distributedLockService.executeWithLock(lockKey, () -> {
            // 1. Get next from DB (Source of Truth) using priority logic
            QueueEntry nextEntry = queueEntryRepository.findNextWaiting(UUIDConverter.convertUUIDToBytes(queueId))
                    .orElseThrow(() -> new RuntimeException("No patients waiting in queue"));

            // 2. Update status
            nextEntry.setStatus(QueueEntryStatus.CALLED);
            nextEntry.setCalledAt(LocalDateTime.now());
            QueueEntry saved = queueEntryRepository.save(nextEntry);

            // 3. Update Appointment
            appointmentService.updateStatus(saved.getAppointmentId(), AppointmentStatus.CALLED);

            // 4. Remove from Redis Waiting ZSET
            redisQueueCacheService.removePatientFromWaitingList(queueId, saved.getId());

            // 5. Audit
            auditService.logAction(ActionType.PATIENT_CALLED, "QUEUE_ENTRY", saved.getId(),
                    "Doctor called token " + saved.getTokenNumber(), null);

            // 6. Publish Event
            publishEvent(saved, "PatientCalled");

            return saved;
        });
    }

    /**
     * Call next patient
     */
    @Transactional(readOnly = true)
    public java.util.Optional<QueueEntry> getPatientActiveQueue(UUID patientId) {
        return queueEntryRepository.findByPatientIdAndStatusIn(
                patientId,
                java.util.List.of(QueueEntryStatus.WAITING, QueueEntryStatus.CALLED));
    }

    private void publishEvent(QueueEntry entry, String eventType) {
        BaseEvent event = BaseEvent.builder()
                .eventType(eventType)
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .aggregateId(entry.getId().toString())
                .build();

        domainEventProducer.publish(queueTopic, "QUEUE_ENTRY", entry.getId(), event);
    }

    /**
     * Leave queue (Cancel entry)
     */
    @Transactional
    public void leaveQueue(UUID queueEntryId, UUID patientId) {
        QueueEntry entry = queueEntryRepository.findById(queueEntryId)
                .orElseThrow(() -> new IllegalArgumentException("Queue entry not found"));

        if (!entry.getPatientId().equals(patientId)) {
            throw new SecurityException("Unauthorized access to queue entry");
        }

        if (entry.getStatus() != QueueEntryStatus.WAITING) {
            throw new IllegalStateException("Cannot leave queue provided status: " + entry.getStatus());
        }

        // Update Status
        entry.setStatus(QueueEntryStatus.CANCELLED);
        queueEntryRepository.save(entry);

        // Update Appointment (Revert to CONFIRMED so they can join again if they want)
        appointmentService.updateStatus(entry.getAppointmentId(), AppointmentStatus.CONFIRMED);

        // Remove from Redis
        redisQueueCacheService.removePatientFromWaitingList(entry.getQueueId(), entry.getId());

        // Log
        auditService.logAction(ActionType.QUEUE_LEFT, "QUEUE_ENTRY", entry.getId(),
                "Patient left queue", null);

        // Event
        publishEvent(entry, "QueueLeft");

        // Broadcast WebSocket update
        webSocketQueueService.broadcastQueueEntryUpdate(entry.getQueueId());
    }

    /**
     * Mark patient as No-Show
     */
    @Transactional
    public void markAsNoShow(UUID queueEntryId) {
        QueueEntry entry = queueEntryRepository.findById(queueEntryId)
                .orElseThrow(() -> new IllegalArgumentException("Queue entry not found"));

        entry.setStatus(QueueEntryStatus.NO_SHOW);
        queueEntryRepository.save(entry);

        // Update Appointment
        appointmentService.updateStatus(entry.getAppointmentId(), AppointmentStatus.NO_SHOW);

        // Remove from Redis
        redisQueueCacheService.removePatientFromWaitingList(entry.getQueueId(), entry.getId());

        auditService.logAction(ActionType.NO_SHOW_MARKED, "QUEUE_ENTRY", entry.getId(),
                "Patient marked as no-show", null);

        publishEvent(entry, "PatientNoShow");

        // Broadcast WebSocket update
        webSocketQueueService.broadcastQueueEntryUpdate(entry.getQueueId());
    }

    /**
     * Skip patient (Move to skipped status)
     */
    @Transactional
    public void skipPatient(UUID queueEntryId) {
        QueueEntry entry = queueEntryRepository.findById(queueEntryId)
                .orElseThrow(() -> new IllegalArgumentException("Queue entry not found"));

        entry.setStatus(QueueEntryStatus.SKIPPED);
        queueEntryRepository.save(entry);

        // Update Appointment (back to CONFIRMED so they can join again)
        appointmentService.updateStatus(entry.getAppointmentId(), AppointmentStatus.CONFIRMED);

        auditService.logAction(ActionType.PATIENT_SKIPPED, "QUEUE_ENTRY", entry.getId(),
                "Patient skipped by doctor", null);

        publishEvent(entry, "PatientSkipped");

        // Broadcast WebSocket update
        webSocketQueueService.broadcastQueueEntryUpdate(entry.getQueueId());
    }

    /**
     * Complete a queue entry (Called when consultation ends)
     */
    @Transactional
    public void completeQueueEntry(UUID queueEntryId) {
        log.info("Completing queue entry: {}", queueEntryId);

        QueueEntry entry = queueEntryRepository.findById(queueEntryId)
                .orElseThrow(() -> new IllegalArgumentException("Queue entry not found"));

        // Only complete if currently CALLED or IN_PROGRESS (though our status only
        // tracks WAITING/CALLED/COMPLETED)
        // If status is already COMPLETED, do nothing (idempotency)
        if (entry.getStatus() == QueueEntryStatus.COMPLETED) {
            log.warn("Queue entry already completed: {}", queueEntryId);
            return;
        }

        entry.setStatus(QueueEntryStatus.COMPLETED);
        entry.setEndedAt(LocalDateTime.now());
        queueEntryRepository.save(entry);

        // Update Appointment Status
        appointmentService.updateStatus(entry.getAppointmentId(), AppointmentStatus.COMPLETED);

        // Audit
        auditService.logAction(ActionType.CONSULTATION_COMPLETED, "QUEUE_ENTRY", entry.getId(),
                "Consultation completed", null);

        // Event
        publishEvent(entry, "QueueEntryCompleted");

        // Broadcast WebSocket update
        webSocketQueueService.broadcastQueueEntryUpdate(entry.getQueueId());
    }

    /**
     * Stale Queue Entry Cleanup (Scheduled)
     * Automatically completes/cancels entries from previous days
     */
    @Transactional
    public void cleanupStaleEntries() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(12); // Consider entries > 12h old as stale
        List<QueueEntry> staleEntries = queueEntryRepository.findStaleEntries(cutoff);

        log.info("Found {} stale queue entries to clean up", staleEntries.size());

        for (QueueEntry entry : staleEntries) {
            try {
                // If CALLED, mark as COMPLETED (assume doctor forgot to end session)
                // If WAITING, mark as CANCELLED (queue closed/expired)

                if (entry.getStatus() == QueueEntryStatus.CALLED) {
                    entry.setStatus(QueueEntryStatus.COMPLETED);
                    entry.setEndedAt(LocalDateTime.now());
                    appointmentService.updateStatus(entry.getAppointmentId(), AppointmentStatus.COMPLETED);
                    auditService.logAction(ActionType.CONSULTATION_COMPLETED, "QUEUE_ENTRY", entry.getId(),
                            "Auto-completed stale called entry", "System");
                } else {
                    entry.setStatus(QueueEntryStatus.CANCELLED);
                    appointmentService.updateStatus(entry.getAppointmentId(), AppointmentStatus.CANCELLED);
                    auditService.logAction(ActionType.QUEUE_LEFT, "QUEUE_ENTRY", entry.getId(),
                            "Auto-cancelled stale waiting entry", "System");
                }

                queueEntryRepository.save(entry);
                redisQueueCacheService.removePatientFromWaitingList(entry.getQueueId(), entry.getId());

            } catch (Exception e) {
                log.error("Failed to cleanup stale entry {}", entry.getId(), e);
            }
        }
    }
}
