package com.medibridge.appointment_service_medibridge.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class JoinQueueRequest {
    @NotNull
    private UUID appointmentId;

    private boolean emergency;
}
