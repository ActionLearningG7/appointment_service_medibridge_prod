package com.medibridge.appointment_service_medibridge.domain.enums;

/**
 * Queue Entry Status
 * 
 * Lifecycle:
 * WAITING → CALLED → (IN_PROGRESS via Appointment) → COMPLETED
 * ↓
 * SKIPPED / NO_SHOW / CANCELLED
 */
public enum QueueEntryStatus {
    /**
     * Patient is waiting in queue
     */
    WAITING,

    /**
     * Doctor has called this patient
     */
    CALLED,

    /**
     * Patient was skipped (with reason)
     */
    SKIPPED,

    /**
     * Patient did not respond when called
     */
    NO_SHOW,

    /**
     * Consultation completed
     */
    COMPLETED,

    /**
     * Queue entry cancelled (patient left queue)
     */
    CANCELLED
}
