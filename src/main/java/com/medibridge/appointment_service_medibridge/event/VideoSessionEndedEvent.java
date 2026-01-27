package com.medibridge.appointment_service_medibridge.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kafka Event: Video Session Ended
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoSessionEndedEvent {
    private UUID sessionId;
    private UUID queueEntryId;
    private UUID doctorId;
    private UUID patientId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Long durationSeconds;
    private String endReason;
    private String eventType = "VIDEO_SESSION_ENDED";
    private LocalDateTime eventTimestamp;
}
