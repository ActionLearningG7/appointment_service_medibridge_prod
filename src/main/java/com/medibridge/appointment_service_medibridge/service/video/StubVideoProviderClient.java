package com.medibridge.appointment_service_medibridge.service.video;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class StubVideoProviderClient implements VideoProviderClient {

    @Override
    public Map<String, String> createSession(String roomId) {
        Map<String, String> session = new HashMap<>();
        session.put("sessionId", UUID.randomUUID().toString());
        session.put("doctorUrl", "https://video.medibridge.com/meet/" + roomId + "?role=doctor");
        session.put("patientUrl", "https://video.medibridge.com/meet/" + roomId + "?role=patient");
        return session;
    }
}
