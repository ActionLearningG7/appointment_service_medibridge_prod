package com.medibridge.appointment_service_medibridge.service.video;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * WebRTC Video Provider Implementation
 * 
 * Generates session details for the internal WebRTC system.
 * Replaces the Stub implementation.
 */
@Service
@Primary // Make this the default implementation
public class WebRTCVideoProviderClient implements VideoProviderClient {

    private static final String FRONTEND_BASE_URL = "https://app.medibridge.com"; // Configure in properties

    @Override
    public Map<String, String> createSession(String roomId) {
        // Session ID matches the Room ID for WebRTC simplicity
        String sessionId = roomId;

        // Generate Secure Tokens (JWT could be appended here for video-specific auth)
        // For now, we rely on the Join URL having the roomId

        Map<String, String> session = new HashMap<>();
        session.put("sessionId", sessionId);

        // Frontend Route: /video-consultation/{roomId}
        session.put("doctorUrl", FRONTEND_BASE_URL + "/video-consultation/" + roomId + "?role=DOCTOR");
        session.put("patientUrl", FRONTEND_BASE_URL + "/video-consultation/" + roomId + "?role=PATIENT");

        return session;
    }
}
