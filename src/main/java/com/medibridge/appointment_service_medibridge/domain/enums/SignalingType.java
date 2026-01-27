package com.medibridge.appointment_service_medibridge.domain.enums;

/**
 * WebRTC Signaling Message Type
 */
public enum SignalingType {
    /**
     * SDP Offer from caller
     */
    OFFER,

    /**
     * SDP Answer from callee
     */
    ANSWER,

    /**
     * ICE Candidate exchange
     */
    ICE,

    /**
     * Participant joining room
     */
    JOIN,

    /**
     * Participant leaving room
     */
    LEAVE,

    /**
     * End call signal
     */
    BYE
}
