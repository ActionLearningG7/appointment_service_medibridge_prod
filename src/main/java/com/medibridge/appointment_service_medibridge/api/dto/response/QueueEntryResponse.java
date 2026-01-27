package com.medibridge.appointment_service_medibridge.api.dto.response;

import com.medibridge.appointment_service_medibridge.domain.enums.Priority;
import com.medibridge.appointment_service_medibridge.domain.enums.QueueEntryStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class QueueEntryResponse {
    private UUID id;
    private UUID queueId;
    private UUID appointmentId;
    private UUID patientId;
    private UUID doctorId;
    private Integer tokenNumber;
    private Priority priority;
    private QueueEntryStatus status;
    private LocalDateTime joinedAt;
    private LocalDateTime calledAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime estimatedStartTime;
    private Integer position; // Dynamic
    private String notes;
}
