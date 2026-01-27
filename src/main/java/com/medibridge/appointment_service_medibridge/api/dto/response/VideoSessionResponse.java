package com.medibridge.appointment_service_medibridge.api.dto.response;

import com.medibridge.appointment_service_medibridge.domain.enums.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Video Consultation Session Response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoSessionResponse {

    /**
     * Session ID
     */
    private UUID sessionId;

    /**
     * Room ID (secure, for WebSocket connection)
     */
    private String roomId;

    /**
     * Queue Entry ID
     */
    private UUID queueEntryId;

    /**
     * Consultation ID (if exists)
     */
    private UUID consultationId;

    /**
     * Session Status
     */
    private SessionStatus status;

    /**
     * Doctor ID
     */
    private UUID doctorId;

    /**
     * Patient ID
     */
    private UUID patientId;

    /**
     * When session was created
     */
    private LocalDateTime createdAt;

    /**
     * When session became active
     */
    private LocalDateTime startedAt;

    /**
     * When session ended
     */
    private LocalDateTime endedAt;

    /**
     * Token expiration time
     */
    private LocalDateTime expiresAt;

    /**
     * WebSocket topics for signaling
     */
    private WebSocketTopics wsTopics;

    /**
     * Is session currently active and not expired
     */
    private boolean active;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WebSocketTopics {
        /**
         * Topic to send signaling messages
         */
        private String sendTo;

        /**
         * Topic to receive signaling messages
         */
        private String receiveFrom;

        /**
         * Topic for presence/status updates
         */
        private String presenceTopic;
    }
}
