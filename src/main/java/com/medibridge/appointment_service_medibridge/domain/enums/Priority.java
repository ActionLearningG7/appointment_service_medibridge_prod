package com.medibridge.appointment_service_medibridge.domain.enums;

/**
 * Queue Entry Priority
 * 
 * Used for ordering patients in queue.
 * Emergency patients are prioritized.
 */
public enum Priority {
    /**
     * Normal priority - regular consultation
     */
    NORMAL,

    /**
     * Emergency priority - urgent consultation
     * These patients jump ahead in the queue
     */
    EMERGENCY
}
