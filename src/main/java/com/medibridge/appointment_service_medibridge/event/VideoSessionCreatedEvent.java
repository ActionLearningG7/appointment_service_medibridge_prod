package com.medibridge.appointment_service_medibridge.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kafka Event: Video Session Created
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoSessionCreatedEvent {
    private UUID sessionId;
    private UUID queueEntryId;
    private UUID consultationId;
    private String roomId;
    private UUID doctorId;
    private UUID patientId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String eventType = "VIDEO_SESSION_CREATED";
    private LocalDateTime eventTimestamp;
}
