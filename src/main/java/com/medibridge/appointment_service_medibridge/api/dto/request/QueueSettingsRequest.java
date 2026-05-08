package com.medibridge.appointment_service_medibridge.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueSettingsRequest {
    private int avgConsultationMinutes;
}
