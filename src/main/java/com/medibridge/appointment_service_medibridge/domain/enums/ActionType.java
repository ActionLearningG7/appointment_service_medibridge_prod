package com.medibridge.appointment_service_medibridge.domain.enums;

/**
 * Action Type for Audit Logging
 * 
 * Tracks all significant actions in the system
 */
public enum ActionType {
    // Appointment Actions
    APPOINTMENT_CREATED,
    APPOINTMENT_CANCELLED,
    APPOINTMENT_UPDATED,

    // Queue Actions
    QUEUE_OPENED,
    QUEUE_PAUSED,
    QUEUE_CLOSED,
    QUEUE_SETTINGS_UPDATED,

    // Queue Entry Actions
    QUEUE_JOINED,
    QUEUE_LEFT,
    PATIENT_CALLED,
    PATIENT_SKIPPED,
    NO_SHOW_MARKED,
    CONSULTATION_STARTED,
    CONSULTATION_COMPLETED,

    // Video Session Actions
    VIDEO_SESSION_CREATED,
    VIDEO_SESSION_STARTED,
    VIDEO_SESSION_ENDED,

    // Admin Actions
    ADMIN_QUEUE_OVERRIDE,
    ADMIN_QUEUE_REORDER,
    ADMIN_APPOINTMENT_CANCEL,

    // Emergency Actions
    EMERGENCY_PRIORITY_ASSIGNED
}
