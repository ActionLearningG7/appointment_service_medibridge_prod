package com.medibridge.appointment_service_medibridge.domain.enums;

/**
 * Consultation Session Status
 *
 * Lifecycle:
 * CREATED → ACTIVE → ENDED
 *     ↓
 * CANCELLED (if ended before starting)
 */
public enum SessionStatus {
    /**
     * Session created, waiting for participants to join
     */
    CREATED,

    /**
     * At least one participant joined, video call active
     */
    ACTIVE,

    /**
     * Session ended normally
     */
    ENDED,

    /**
     * Session cancelled before starting
     */
    CANCELLED
}
