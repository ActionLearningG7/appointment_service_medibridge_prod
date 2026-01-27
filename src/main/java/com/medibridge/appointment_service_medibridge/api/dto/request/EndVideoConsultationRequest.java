package com.medibridge.appointment_service_medibridge.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to end video consultation session
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EndVideoConsultationRequest {

    /**
     * Reason for ending (optional)
     */
    private String reason;

    /**
     * Doctor notes (optional, for consultation record)
     */
    private String notes;
}
