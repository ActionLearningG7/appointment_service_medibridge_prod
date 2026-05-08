package com.medibridge.appointment_service_medibridge.api.dto.response;

import com.medibridge.appointment_service_medibridge.domain.enums.AppointmentStatus;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class AppointmentResponse {
    private UUID id;
    private UUID patientId;
    private UUID doctorId;
    private LocalDate appointmentDate;
    private AppointmentStatus status;
    private String reasonForVisit;
    private boolean paymentRequired;
    private String invoiceId;
    private String checkoutUrl;
}
