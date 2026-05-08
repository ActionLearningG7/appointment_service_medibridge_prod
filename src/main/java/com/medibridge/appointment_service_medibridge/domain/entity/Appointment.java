package com.medibridge.appointment_service_medibridge.domain.entity;

import com.medibridge.appointment_service_medibridge.domain.enums.AppointmentStatus;
import com.medibridge.appointment_service_medibridge.util.UUIDConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Appointment Entity
 * 
 * Represents a virtual consultation appointment request.
 * 
 * Lifecycle:
 * REQUESTED → QUEUED → CALLED → IN_PROGRESS → COMPLETED
 * ↓
 * CANCELLED / NO_SHOW
 * 
 * Indexes:
 * - (patient_id, appointment_date, status) - Patient's appointments
 * - (doctor_id, appointment_date) - Doctor's appointments
 * - (status) - Filter by status
 */
@Entity
@Table(name = "appointments", indexes = {
        @Index(name = "idx_patient_date_status", columnList = "patient_id, appointment_date, status"),
        @Index(name = "idx_doctor_date", columnList = "doctor_id, appointment_date"),
        @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment extends BaseEntity {

    /**
     * Patient ID (from User Service)
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "patient_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID patientId;

    /**
     * Doctor ID (from User Service)
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "doctor_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID doctorId;

    /**
     * Organization ID (for multi-org support)
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "organization_id", columnDefinition = "BINARY(16)")
    private UUID organizationId;

    /**
     * Appointment Type (always VIRTUAL for this service)
     */
    @Column(name = "appointment_type", length = 20, nullable = false)
    @Builder.Default
    private String appointmentType = "VIRTUAL";

    /**
     * Reason for visit (short description)
     */
    @Column(name = "reason_for_visit", length = 500)
    private String reasonForVisit;

    /**
     * Current status of the appointment
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private AppointmentStatus status;

    /**
     * Appointment date
     */
    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    /**
     * Who created this appointment (patient ID or admin ID)
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "created_by", columnDefinition = "BINARY(16)")
    private UUID createdBy;

    /**
     * Cancellation reason (if cancelled)
     */
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    /**
     * Cancelled by (user ID)
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "cancelled_by", columnDefinition = "BINARY(16)")
    private UUID cancelledBy;

    /**
     * Doctor name (cached for queue monitoring)
     * Denormalized for performance
     */
    @Column(name = "doctor_name", length = 255)
    private String doctorName;

    /**
     * Doctor specialization (cached for queue monitoring)
     * Denormalized for performance
     */
    @Column(name = "doctor_specialization", length = 100)
    private String doctorSpecialization;

    /**
     * Invoice ID (from Payment Service)
     */
    @Column(name = "invoice_id", length = 50)
    private String invoiceId;

    /**
     * Payment Status (PENDING, PAID, FAILED)
     */
    @Column(name = "payment_status", length = 20)
    private String paymentStatus;
}
