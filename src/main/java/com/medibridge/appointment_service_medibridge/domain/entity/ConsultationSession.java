package com.medibridge.appointment_service_medibridge.domain.entity;

import com.medibridge.appointment_service_medibridge.domain.enums.SessionStatus;
import com.medibridge.appointment_service_medibridge.util.UUIDConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consultation Session Entity
 *
 * Represents a video consultation session with WebRTC room details.
 *
 * Lifecycle:
 * CREATED → ACTIVE → ENDED
 *
 * Security:
 * - roomId is non-guessable UUID
 * - tokenExpiresAt enforces short-lived access (30 min default)
 * - only doctorId and patientId can join
 *
 * Constraints:
 * - UNIQUE(consultation_id) - One session per consultation
 * - UNIQUE(room_id) - Unique room identifier
 *
 * Indexes:
 * - (room_id) - Quick room lookup
 * - (doctor_id, status) - Find active doctor sessions
 * - (patient_id, status) - Find active patient sessions
 */
@Entity
@Table(name = "consultation_sessions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_consultation_session", columnNames = {"consultation_id"}),
    @UniqueConstraint(name = "uk_room_id", columnNames = {"room_id"})
}, indexes = {
    @Index(name = "idx_room_id", columnList = "room_id"),
    @Index(name = "idx_doctor_status", columnList = "doctor_id, status"),
    @Index(name = "idx_patient_status", columnList = "patient_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationSession extends BaseEntity {

    /**
     * Consultation ID (if consultation entity exists)
     * Can be null if session created before consultation entity
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "consultation_id", columnDefinition = "BINARY(16)")
    private UUID consultationId;

    /**
     * Queue Entry ID (link to queue)
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "queue_entry_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID queueEntryId;

    /**
     * Secure Room ID (non-guessable UUID)
     */
    @Column(name = "room_id", length = 36, nullable = false)
    private String roomId;

    /**
     * Session Status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SessionStatus status;

    /**
     * Doctor ID (from User Service)
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "doctor_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID doctorId;

    /**
     * Patient ID (from User Service)
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "patient_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID patientId;

    /**
     * When session was created
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * When first participant joined (session became active)
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * When session ended
     */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /**
     * Last activity timestamp (for timeout detection)
     */
    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt;

    /**
     * Token expiration (short-lived, e.g., 30 minutes)
     */
    @Column(name = "token_expires_at", nullable = false)
    private LocalDateTime tokenExpiresAt;

    /**
     * Optimistic locking version
     */
    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (lastActivityAt == null) {
            lastActivityAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        super.onUpdate();
        lastActivityAt = LocalDateTime.now();
    }

    /**
     * Check if session is expired
     */
    public boolean isExpired() {
        return tokenExpiresAt != null && LocalDateTime.now().isAfter(tokenExpiresAt);
    }

    /**
     * Check if session is active
     */
    public boolean isActive() {
        return status == SessionStatus.ACTIVE && !isExpired();
    }

    /**
     * Check if user is authorized to join
     */
    public boolean isAuthorizedParticipant(UUID userId) {
        return userId != null && (userId.equals(doctorId) || userId.equals(patientId));
    }
}
