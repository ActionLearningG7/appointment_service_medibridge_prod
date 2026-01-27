package com.medibridge.appointment_service_medibridge.domain.entity;

import com.medibridge.appointment_service_medibridge.domain.enums.QueueStatus;
import com.medibridge.appointment_service_medibridge.util.UUIDConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Virtual Queue Entity
 * 
 * Represents a doctor's daily virtual consultation queue.
 * One queue per doctor per day.
 * 
 * Lifecycle:
 * OPEN → PAUSED → OPEN → CLOSED
 * 
 * Constraints:
 * - UNIQUE(doctor_id, queue_date) - One queue per doctor per day
 * 
 * Indexes:
 * - (doctor_id, queue_date, status) - Find active queues
 */
@Entity
@Table(name = "virtual_queues", uniqueConstraints = {
        @UniqueConstraint(name = "uk_doctor_date", columnNames = { "doctor_id", "queue_date" })
}, indexes = {
        @Index(name = "idx_doctor_date_status", columnList = "doctor_id, queue_date, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VirtualQueue extends BaseEntity {

    /**
     * Doctor ID (from User Service)
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "doctor_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID doctorId;

    /**
     * Organization ID (for multi-org support)
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "organization_id", columnDefinition = "BINARY(16)")
    private UUID organizationId;

    /**
     * Queue date
     */
    @Column(name = "queue_date", nullable = false)
    private LocalDate queueDate;

    /**
     * Current queue status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private QueueStatus status;

    /**
     * Current token number (last assigned)
     */
    @Column(name = "current_token", nullable = false)
    private Integer currentToken = 0;

    /**
     * Average consultation time in minutes
     * Used for estimating wait times
     */
    @Column(name = "avg_consultation_minutes", nullable = false)
    private Integer avgConsultationMinutes = 15;

    /**
     * Maximum patients allowed in queue
     */
    @Column(name = "max_patients")
    private Integer maxPatients = 100;

    /**
     * When the queue was opened
     */
    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    /**
     * When the queue was closed
     */
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    /**
     * Who opened the queue (doctor ID or admin ID)
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "opened_by", columnDefinition = "BINARY(16)")
    private UUID openedBy;

    /**
     * Who closed the queue (doctor ID or admin ID)
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "closed_by", columnDefinition = "BINARY(16)")
    private UUID closedBy;
}
