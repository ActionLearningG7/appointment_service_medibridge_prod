package com.medibridge.appointment_service_medibridge.service.video;

import java.util.Map;

public interface VideoProviderClient {
    /**
     * Create a video session
     * 
     * @return Map containing sessionId, doctorUrl, patientUrl
     */
    Map<String, String> createSession(String roomId);
}
