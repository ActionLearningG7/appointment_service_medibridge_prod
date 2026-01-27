package com.medibridge.appointment_service_medibridge.domain.enums;

/**
 * Virtual Queue Status
 * 
 * Lifecycle:
 * OPEN → PAUSED → OPEN → CLOSED
 */
public enum QueueStatus {
    /**
     * Queue is open and accepting patients
     */
    OPEN,

    /**
     * Queue is temporarily paused (e.g., doctor break)
     */
    PAUSED,

    /**
     * Queue is closed for the day
     */
    CLOSED
}
