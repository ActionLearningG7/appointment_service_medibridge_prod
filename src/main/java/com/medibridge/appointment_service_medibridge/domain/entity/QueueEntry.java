package com.medibridge.appointment_service_medibridge.domain.entity;

import com.medibridge.appointment_service_medibridge.domain.enums.Priority;
import com.medibridge.appointment_service_medibridge.domain.enums.QueueEntryStatus;
import com.medibridge.appointment_service_medibridge.util.UUIDConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Queue Entry Entity
 * 
 * Represents a patient's position in a doctor's queue.
 * 
 * Lifecycle:
 * WAITING → CALLED → (IN_PROGRESS via Appointment) → COMPLETED
 * ↓
 * SKIPPED / NO_SHOW / CANCELLED
 * 
 * Constraints:
 * - UNIQUE(queue_id, token_number) - No duplicate tokens
 * 
 * Indexes:
 * - (queue_id, status) - Find waiting patients
 * - (patient_id, doctor_id, joined_at) - Patient's queue history
 * 
 * Foreign Keys:
 * - queue_id → virtual_queues(id)
 * - appointment_id → appointments(id)
 */
@Entity
@Table(name = "queue_entries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_queue_token", columnNames = { "queue_id", "token_number" })
}, indexes = {
        @Index(name = "idx_queue_status", columnList = "queue_id, status"),
        @Index(name = "idx_patient_doctor_date", columnList = "patient_id, doctor_id, joined_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueueEntry extends BaseEntity {

    /**
     * Queue ID (foreign key to virtual_queues)
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "queue_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID queueId;

    /**
     * Appointment ID (foreign key to appointments)
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "appointment_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID appointmentId;

    /**
     * Patient ID (from User Service)
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "patient_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID patientId;

    /**
     * Doctor ID (from User Service)
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "doctor_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID doctorId;

    /**
     * Token number (sequential per queue)
     * UNIQUE constraint with queue_id
     */
    @Column(name = "token_number", nullable = false)
    private Integer tokenNumber;

    /**
     * Priority (NORMAL or EMERGENCY)
     * Emergency patients jump ahead in queue
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20, nullable = false)
    private Priority priority = Priority.NORMAL;

    /**
     * Current status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private QueueEntryStatus status;

    /**
     * When patient joined the queue
     */
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    /**
     * When doctor called this patient
     */
    @Column(name = "called_at")
    private LocalDateTime calledAt;

    /**
     * When consultation started
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * When consultation ended
     */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /**
     * Estimated start time (calculated)
     * = now + (patients_ahead * avg_consultation_minutes)
     */
    @Column(name = "estimated_start_time")
    private LocalDateTime estimatedStartTime;

    /**
     * Position in queue (1-based)
     * Calculated dynamically based on priority and token
     */
    @Transient
    private Integer position;

    /**
     * Notes (doctor/admin can add notes)
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Skip reason (if status = SKIPPED)
     */
    @Column(name = "skip_reason", length = 500)
    private String skipReason;

    /**
     * Who skipped (doctor ID or admin ID)
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "skipped_by", columnDefinition = "BINARY(16)")
    private UUID skippedBy;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }
}
