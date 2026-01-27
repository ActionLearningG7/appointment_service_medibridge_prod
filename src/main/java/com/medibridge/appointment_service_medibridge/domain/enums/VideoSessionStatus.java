package com.medibridge.appointment_service_medibridge.domain.enums;

/**
 * Video Session Status
 * 
 * Lifecycle:
 * CREATED → ACTIVE → ENDED
 */
public enum VideoSessionStatus {
    /**
     * Session created, URLs generated
     */
    CREATED,

    /**
     * At least one participant has joined
     */
    ACTIVE,

    /**
     * Session has ended
     */
    ENDED
}
