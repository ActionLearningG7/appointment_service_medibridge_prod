package com.medibridge.appointment_service_medibridge.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request to start video consultation session
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartVideoConsultationRequest {

    /**
     * Queue Entry ID
     */
    @NotNull(message = "Queue entry ID is required")
    private UUID queueEntryId;

    /**
     * Optional: Consultation ID if already created
     */
    private UUID consultationId;
}
