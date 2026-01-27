package com.medibridge.appointment_service_medibridge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueEventBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notify doctor of queue changes
     */
    public void convertAndSendToDoctor(UUID doctorId, String eventType, Object payload) {
        String dest = "/topic/queues/doctor/" + doctorId;
        log.debug("Sending {} to {}", eventType, dest);
        messagingTemplate.convertAndSend(dest, payload);
    }

    /**
     * Notify specific patient
     */
    public void convertAndSendToPatient(String userId, String eventType, Object payload) {
        String dest = "/queue/updates"; // Will become /user/{userId}/queue/updates
        log.debug("Sending {} to user {}", eventType, userId);
        messagingTemplate.convertAndSendToUser(userId, dest, payload);
    }
}
