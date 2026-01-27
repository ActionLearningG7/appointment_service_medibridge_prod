package com.medibridge.appointment_service_medibridge.domain.entity;

import com.medibridge.appointment_service_medibridge.domain.enums.VideoProvider;
import com.medibridge.appointment_service_medibridge.domain.enums.VideoSessionStatus;
import com.medibridge.appointment_service_medibridge.util.UUIDConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Video Session Entity
 * 
 * Stores metadata for video consultation sessions.
 * Does NOT handle actual video streaming (delegated to provider).
 * 
 * Lifecycle:
 * CREATED → ACTIVE → ENDED
 * 
 * Indexes:
 * - (appointment_id) - Find session by appointment
 * - (queue_entry_id) - Find session by queue entry
 * 
 * Foreign Keys:
 * - appointment_id → appointments(id)
 */
@Entity
@Table(name = "video_sessions", indexes = {
        @Index(name = "idx_appointment", columnList = "appointment_id"),
        @Index(name = "idx_queue_entry", columnList = "queue_entry_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoSession extends BaseEntity {

    /**
     * Appointment ID (foreign key to appointments)
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "appointment_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID appointmentId;

    /**
     * Queue Entry ID (optional, if created from queue)
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "queue_entry_id", columnDefinition = "BINARY(16)")
    private UUID queueEntryId;

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
     * Video provider (STUB, TWILIO, AGORA, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 50)
    private VideoProvider provider;

    /**
     * Provider's session ID
     */
    @Column(name = "session_id")
    private String sessionId;

    /**
     * Provider's room ID
     */
    @Column(name = "room_id")
    private String roomId;

    /**
     * Doctor's join URL (secure, time-limited)
     */
    @Column(name = "doctor_join_url", columnDefinition = "TEXT")
    private String doctorJoinUrl;

    /**
     * Patient's join URL (secure, time-limited)
     */
    @Column(name = "patient_join_url", columnDefinition = "TEXT")
    private String patientJoinUrl;

    /**
     * When the access tokens expire
     */
    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    /**
     * Current session status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private VideoSessionStatus status;

    /**
     * When the session actually started (first participant joined)
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * When the session ended
     */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /**
     * Session duration in minutes (calculated)
     */
    @Transient
    private Integer durationMinutes;

    /**
     * Calculate duration when session ends
     */
    public void calculateDuration() {
        if (startedAt != null && endedAt != null) {
            durationMinutes = (int) java.time.Duration.between(startedAt, endedAt).toMinutes();
        }
    }
}
