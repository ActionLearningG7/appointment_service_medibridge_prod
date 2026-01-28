package com.medibridge.appointment_service_medibridge.api.dto.response;

import com.medibridge.appointment_service_medibridge.domain.enums.QueueStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Admin Queue Monitor Response
 * Extended queue information with statistics for admin monitoring
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminQueueMonitorResponse {

    private UUID id;
    private UUID doctorId;
    private String doctorName;
    private String specialization;
    private LocalDate queueDate;
    private QueueStatus status;
    private Integer currentToken;
    private Integer avgConsultationMinutes;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;

    // Statistics
    private Integer totalPatients;
    private Integer waitingCount;
    private Integer completedCount;
    private Integer calledCount;
    private Integer cancelledCount;
    private Integer noShowCount;
}
