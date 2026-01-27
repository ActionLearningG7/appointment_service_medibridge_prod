package com.medibridge.appointment_service_medibridge.domain.enums;

/**
 * Video Provider
 * 
 * Abstraction for different video consultation providers.
 * Currently using STUB for development.
 */
public enum VideoProvider {
    /**
     * Stub implementation for development/testing
     */
    STUB,

    /**
     * Twilio Video
     */
    TWILIO,

    /**
     * Agora.io
     */
    AGORA,

    /**
     * Daily.co
     */
    DAILY,

    /**
     * Custom WebRTC implementation
     */
    WEBRTC
}
