package com.medibridge.appointment_service_medibridge.api.dto.response;

import com.medibridge.appointment_service_medibridge.domain.enums.QueueStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueResponse {

    private UUID id;
    private UUID doctorId;
    private LocalDate queueDate;
    private QueueStatus status;
    private Integer currentToken;
    private Integer avgConsultationMinutes;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private UUID openedBy;
    private UUID closedBy;
}
