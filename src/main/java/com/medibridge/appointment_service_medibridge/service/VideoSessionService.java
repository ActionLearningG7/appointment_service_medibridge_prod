package com.medibridge.appointment_service_medibridge.service;

import com.medibridge.appointment_service_medibridge.domain.entity.VideoSession;
import com.medibridge.appointment_service_medibridge.domain.enums.ActionType;
import com.medibridge.appointment_service_medibridge.domain.enums.VideoProvider;
import com.medibridge.appointment_service_medibridge.domain.enums.VideoSessionStatus;
import com.medibridge.appointment_service_medibridge.domain.repository.VideoSessionRepository;
import com.medibridge.appointment_service_medibridge.kafka.event.BaseEvent;
import com.medibridge.appointment_service_medibridge.kafka.producer.DomainEventProducer;
import com.medibridge.appointment_service_medibridge.service.video.VideoProviderClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoSessionService {

    private final VideoSessionRepository videoSessionRepository;
    private final VideoProviderClient videoProviderClient; // Inject abstract interface
    private final DomainEventProducer domainEventProducer;
    private final AuditService auditService;

    @Value("${appointment.kafka.topics.video}")
    private String videoTopic;

    /**
     * Create Video Session
     * Called when Doctor accepts the call or manually starts session
     */
    @Transactional
    public VideoSession createSession(UUID appointmentId, UUID doctorId, UUID patientId, UUID queueEntryId) {
        // Check existing
        if (videoSessionRepository.findByAppointmentId(appointmentId).isPresent()) {
            return videoSessionRepository.findByAppointmentId(appointmentId).get();
        }

        String roomId = UUID.randomUUID().toString();

        // Call Provider (Stub or Real)
        Map<String, String> providerSession = videoProviderClient.createSession(roomId);

        VideoSession session = VideoSession.builder()
                .appointmentId(appointmentId)
                .queueEntryId(queueEntryId)
                .doctorId(doctorId)
                .patientId(patientId)
                .provider(VideoProvider.STUB)
                .roomId(roomId)
                .sessionId(providerSession.get("sessionId"))
                .doctorJoinUrl(providerSession.get("doctorUrl"))
                .patientJoinUrl(providerSession.get("patientUrl"))
                .tokenExpiresAt(LocalDateTime.now().plusMinutes(60))
                .status(VideoSessionStatus.CREATED)
                .build();

        VideoSession saved = videoSessionRepository.save(session);

        auditService.logAction(ActionType.VIDEO_SESSION_CREATED, "VIDEO_SESSION", saved.getId(),
                "Session created for appointment " + appointmentId, null);

        publishEvent(saved, "VideoSessionCreated");

        return saved;
    }

    private void publishEvent(VideoSession session, String eventType) {
        BaseEvent event = BaseEvent.builder()
                .eventType(eventType)
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .aggregateId(session.getId().toString())
                .build();

        domainEventProducer.publish(videoTopic, "VIDEO_SESSION", session.getId(), event);
    }
}
