package com.medibridge.appointment_service_medibridge.domain.entity;

import com.medibridge.appointment_service_medibridge.domain.enums.ConnectionState;
import com.medibridge.appointment_service_medibridge.domain.enums.ParticipantRole;
import com.medibridge.appointment_service_medibridge.util.UUIDConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consultation Participant Presence Entity
 *
 * Tracks participant connection state and presence in video sessions.
 * Used for:
 * - Determining when both parties are connected
 * - Detecting disconnections
 * - Audit trail of join/leave events
 *
 * Indexes:
 * - (session_id, participant_id) - Unique participant per session
 * - (session_id, connection_state) - Find connected participants
 */
@Entity
@Table(name = "consultation_participant_presence", uniqueConstraints = {
    @UniqueConstraint(name = "uk_session_participant", columnNames = {"session_id", "participant_id"})
}, indexes = {
    @Index(name = "idx_session_participant", columnList = "session_id, participant_id"),
    @Index(name = "idx_session_state", columnList = "session_id, connection_state")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationParticipantPresence extends BaseEntity {

    /**
     * Session ID
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "session_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID sessionId;

    /**
     * Participant User ID
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "participant_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID participantId;

    /**
     * Participant Role (DOCTOR/PATIENT)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private ParticipantRole role;

    /**
     * When participant joined
     */
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    /**
     * When participant left (null if still connected)
     */
    @Column(name = "left_at")
    private LocalDateTime leftAt;

    /**
     * Last ping/heartbeat timestamp
     */
    @Column(name = "last_ping_at", nullable = false)
    private LocalDateTime lastPingAt;

    /**
     * Connection State
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "connection_state", length = 20, nullable = false)
    private ConnectionState connectionState;

    /**
     * User Agent (optional, for debugging)
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    /**
     * On create, set joinedAt and lastPingAt if not set
     */
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
        if (lastPingAt == null) {
            lastPingAt = LocalDateTime.now();
        }
    }

    /**
     * Check if participant is currently connected
     */
    public boolean isConnected() {
        return connectionState == ConnectionState.CONNECTED && leftAt == null;
    }

    /**
     * Update last ping timestamp
     */
    public void ping() {
        this.lastPingAt = LocalDateTime.now();
    }

    /**
     * Mark participant as disconnected
     */
    public void disconnect() {
        this.connectionState = ConnectionState.DISCONNECTED;
        this.leftAt = LocalDateTime.now();
    }
}
