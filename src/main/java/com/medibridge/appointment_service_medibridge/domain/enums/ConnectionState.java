package com.medibridge.appointment_service_medibridge.domain.enums;

/**
 * Participant Connection State
 */
public enum ConnectionState {
    /**
     * Participant is attempting to connect
     */
    CONNECTING,

    /**
     * Participant successfully connected to room
     */
    CONNECTED,

    /**
     * Participant disconnected from room
     */
    DISCONNECTED
}
