package com.medibridge.appointment_service_medibridge.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${appointment.kafka.topics.user:medibridge.user.events}", groupId = "appointment-service-group")
    public void consumeUserEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String eventType = event.get("eventType").asText();
            String userId = event.get("aggregateId").asText();

            log.info("Received User Event: {} for User: {}", eventType, userId);

            switch (eventType) {
                case "DoctorSuspended":
                    handleDoctorSuspended(userId);
                    break;
                case "DoctorActivated":
                    handleDoctorActivated(userId);
                    break;
                default:
                    log.debug("Ignored event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process user event: {}", message, e);
        }
    }

    private void handleDoctorSuspended(String doctorId) {
        log.warn("TODO: Implement logic to close queue for suspended doctor: {}", doctorId);
        // queueService.forceCloseQueuesForDoctor(UUID.fromString(doctorId));
    }

    private void handleDoctorActivated(String doctorId) {
        log.info("Doctor activated: {}", doctorId);
    }
}
