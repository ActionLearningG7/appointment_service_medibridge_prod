package com.medibridge.appointment_service_medibridge.api.dto.websocket;

import com.medibridge.appointment_service_medibridge.domain.enums.ParticipantRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Presence Event Message
 *
 * Sent to /topic/consultations/{roomId}/presence
 * Notifies participants about join/leave events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresenceEvent {

    /**
     * Event type: PARTICIPANT_JOINED, PARTICIPANT_LEFT, SESSION_ENDED
     */
    private String eventType;

    /**
     * Participant ID (masked for privacy)
     */
    private UUID participantId;

    /**
     * Participant Role
     */
    private ParticipantRole role;

    /**
     * Session/Room ID
     */
    private String roomId;

    /**
     * Event timestamp
     */
    private LocalDateTime timestamp;

    /**
     * Additional context (optional)
     */
    private String message;

    /**
     * Number of currently connected participants
     */
    private Integer connectedCount;
}
