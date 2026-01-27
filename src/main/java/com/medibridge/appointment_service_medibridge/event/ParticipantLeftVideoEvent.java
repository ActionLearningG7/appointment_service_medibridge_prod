package com.medibridge.appointment_service_medibridge.event;

import com.medibridge.appointment_service_medibridge.domain.enums.ParticipantRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kafka Event: Participant Left Video
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipantLeftVideoEvent {
    private UUID sessionId;
    private UUID participantId;
    private ParticipantRole role;
    private LocalDateTime leftAt;
    private Long sessionDurationSeconds;
    private String eventType = "PARTICIPANT_LEFT_VIDEO";
    private LocalDateTime eventTimestamp;
}
