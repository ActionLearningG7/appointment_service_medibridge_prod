package com.medibridge.appointment_service_medibridge.domain.enums;

/**
 * Appointment Status Lifecycle
 * 
 * Flow:
 * REQUESTED → QUEUED → CALLED → IN_PROGRESS → COMPLETED
 * ↓
 * CANCELLED / NO_SHOW
 */
public enum AppointmentStatus {
    /**
     * Initial state when patient creates appointment
     */
    REQUESTED,

    /**
     * Patient has joined the queue
     */
    QUEUED,

    /**
     * Doctor has called the patient
     */
    CALLED,

    /**
     * Consultation is in progress
     */
    IN_PROGRESS,

    /**
     * Consultation completed successfully
     */
    COMPLETED,

    /**
     * Appointment cancelled (by patient, doctor, or admin)
     */
    CANCELLED,

    /**
     * Patient did not show up when called
     */
    NO_SHOW
}
