package com.medibridge.appointment_service_medibridge.event;

import com.medibridge.appointment_service_medibridge.domain.enums.ParticipantRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kafka Event: Participant Joined Video
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipantJoinedVideoEvent {
    private UUID sessionId;
    private UUID participantId;
    private ParticipantRole role;
    private LocalDateTime joinedAt;
    private String eventType = "PARTICIPANT_JOINED_VIDEO";
    private LocalDateTime eventTimestamp;
}
