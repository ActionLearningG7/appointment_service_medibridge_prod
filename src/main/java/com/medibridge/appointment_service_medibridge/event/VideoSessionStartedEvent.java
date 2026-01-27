package com.medibridge.appointment_service_medibridge.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kafka Event: Video Session Started
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoSessionStartedEvent {
    private UUID sessionId;
    private UUID queueEntryId;
    private UUID doctorId;
    private UUID patientId;
    private LocalDateTime startedAt;
    private String eventType = "VIDEO_SESSION_STARTED";
    private LocalDateTime eventTimestamp;
}
