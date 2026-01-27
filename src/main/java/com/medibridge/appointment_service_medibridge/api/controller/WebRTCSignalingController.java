package com.medibridge.appointment_service_medibridge.api.controller;

import com.medibridge.appointment_service_medibridge.api.dto.websocket.PresenceEvent;
import com.medibridge.appointment_service_medibridge.api.dto.websocket.SignalingMessage;
import com.medibridge.appointment_service_medibridge.domain.entity.ConsultationSession;
import com.medibridge.appointment_service_medibridge.domain.enums.ParticipantRole;
import com.medibridge.appointment_service_medibridge.domain.enums.SignalingType;
import com.medibridge.appointment_service_medibridge.service.VideoConsultationService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WebRTC Signaling Controller
 *
 * Handles secure WebRTC signaling for video consultations.
 *
 * Architecture:
 * - Clients send to: /app/webrtc/{roomId}/signal
 * - Server routes to recipient: /user/webrtc/{roomId}/signal (private)
 * - Presence updates: /topic/consultations/{roomId}/presence (public to room)
 *
 * Security:
 * - JWT validated on WebSocket handshake
 * - Room authorization checked before message delivery
 * - Payload size limited to 16KB
 *
 * Message Flow:
 * 1. Doctor sends OFFER (to PATIENT role)
 * 2. Server validates and forwards to patient's private queue
 * 3. Patient sends ANSWER (to DOCTOR role)
 * 4. Both exchange ICE candidates
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebRTCSignalingController {

    private final VideoConsultationService videoService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MeterRegistry meterRegistry;

    /**
     * Handle WebRTC Signaling Messages
     *
     * Clients send to: /app/webrtc/{roomId}/signal
     * Receives at: /user/webrtc/{roomId}/signal (private to recipient)
     *
     * @param roomId Room/Session ID
     * @param message Signaling message (OFFER, ANSWER, ICE, etc.)
     * @param principal Authenticated user (from JWT)
     */
    @MessageMapping("/webrtc/{roomId}/signal")
    public void handleSignal(
        @DestinationVariable String roomId,
        @Payload SignalingMessage message,
        Principal principal
    ) {
        try {
            log.info("=== WebRTC Signal Received ===");
            log.info("RoomId: {}", roomId);
            log.info("Principal: {}", principal != null ? principal.getName() : "NULL");
            log.info("Message type: {}", message != null ? message.getType() : "NULL");
            
            // Extract user ID from principal
            UUID senderId = extractUserId(principal);
            message.setSenderId(senderId);

            log.info("Signal received: roomId={}, type={}, sender={}, to={}",
                roomId, message.getType(), senderId, message.getTo());

            // Validate session and authorization
            ConsultationSession session = videoService.getSessionByRoomId(roomId, senderId);
            log.info("Session found: {}", session.getId());

            // Validate payload size
            if (message.getSdp() != null) {
                videoService.validatePayloadSize(message.getSdp());
            }
            if (message.getCandidate() != null) {
                videoService.validatePayloadSize(message.getCandidate());
            }

            // Handle different message types
            switch (message.getType()) {
                case JOIN:
                    log.info("Handling JOIN message");
                    handleJoin(roomId, senderId, session, principal);
                    break;

                case LEAVE:
                case BYE:
                    log.info("Handling LEAVE/BYE message");
                    handleLeave(roomId, senderId, session);
                    break;

                case OFFER:
                case ANSWER:
                case ICE:
                    log.info("Routing signaling message: {}", message.getType());
                    routeSignalingMessage(roomId, message, session);
                    break;

                default:
                    log.warn("Unknown signaling message type: {}", message.getType());
            }

            // Update session activity
            videoService.pingSession(roomId, senderId);

            // Increment metrics
            incrementCounter("signaling.messages.total", message.getType().name());

        } catch (SecurityException e) {
            log.error("❌ Security violation in signaling: roomId={}, error={}", roomId, e.getMessage());
            incrementCounter("signaling.errors.security");
            // Do not send error to client for security reasons
        } catch (Exception e) {
            log.error("Error handling signaling message: roomId={}, error={}", roomId, e.getMessage(), e);
            incrementCounter("signaling.errors.general");
        }
    }

    /**
     * Handle participant join
     */
    private void handleJoin(String roomId, UUID participantId, ConsultationSession session, Principal principal) {
        log.info("Participant joining: roomId={}, participantId={}", roomId, participantId);

        // Determine participant role
        ParticipantRole role = session.getDoctorId().equals(participantId)
            ? ParticipantRole.DOCTOR
            : ParticipantRole.PATIENT;

        // Extract user agent (optional)
        String userAgent = null; // Can be extracted from handshake attributes if needed

        // Register participant presence
        videoService.joinSession(roomId, participantId, role, userAgent);

        // Get connected count
        int connectedCount = videoService.getConnectedParticipantCount(session.getId());

        // Broadcast presence event to room
        PresenceEvent event = PresenceEvent.builder()
            .eventType("PARTICIPANT_JOINED")
            .participantId(participantId)
            .role(role)
            .roomId(roomId)
            .timestamp(LocalDateTime.now())
            .message(role + " joined the consultation")
            .connectedCount(connectedCount)
            .build();

        messagingTemplate.convertAndSend("/topic/consultations/" + roomId + "/presence", event);

        log.info("Participant joined successfully: participantId={}, role={}, connectedCount={}",
            participantId, role, connectedCount);

        incrementCounter("signaling.participants.joined", role.name());
    }

    /**
     * Handle participant leave
     */
    private void handleLeave(String roomId, UUID participantId, ConsultationSession session) {
        log.info("Participant leaving: roomId={}, participantId={}", roomId, participantId);

        // Determine participant role
        ParticipantRole role = session.getDoctorId().equals(participantId)
            ? ParticipantRole.DOCTOR
            : ParticipantRole.PATIENT;

        // Update participant presence
        videoService.leaveSession(roomId, participantId);

        // Get connected count
        int connectedCount = videoService.getConnectedParticipantCount(session.getId());

        // Broadcast presence event to room
        PresenceEvent event = PresenceEvent.builder()
            .eventType("PARTICIPANT_LEFT")
            .participantId(participantId)
            .role(role)
            .roomId(roomId)
            .timestamp(LocalDateTime.now())
            .message(role + " left the consultation")
            .connectedCount(connectedCount)
            .build();

        messagingTemplate.convertAndSend("/topic/consultations/" + roomId + "/presence", event);

        log.info("Participant left successfully: participantId={}, role={}, connectedCount={}",
            participantId, role, connectedCount);

        incrementCounter("signaling.participants.left", role.name());
    }

    /**
     * Route signaling message to specific recipient based on role
     */
    private void routeSignalingMessage(String roomId, SignalingMessage message, ConsultationSession session) {
        UUID recipientId;

        // Determine recipient based on target role
        if (message.getTo() == ParticipantRole.DOCTOR) {
            recipientId = session.getDoctorId();
        } else if (message.getTo() == ParticipantRole.PATIENT) {
            recipientId = session.getPatientId();
        } else {
            log.error("Invalid target role in signaling message: {}", message.getTo());
            return;
        }

        // Send to recipient's private queue
        String destination = "/webrtc/" + roomId + "/signal";
        messagingTemplate.convertAndSendToUser(recipientId.toString(), destination, message);

        log.debug("Signaling message routed: type={}, from={}, to={}, recipientId={}",
            message.getType(), message.getSenderId(), message.getTo(), recipientId);
    }

    /**
     * Extract user ID from principal
     */
    private UUID extractUserId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new SecurityException("User not authenticated");
        }

        // Principal.getName() returns email from WebSocket auth
        // Try to parse as UUID (for REST endpoints)
        try {
            return UUID.fromString(principal.getName());
        } catch (IllegalArgumentException e) {
            // If not UUID, it's probably the email from WebSocket
            throw new SecurityException("Invalid user ID in principal: " + principal.getName());
        }
    }

    /**
     * Increment metrics counter
     */
    private void incrementCounter(String metricName, String... tags) {
        try {
            Counter.builder(metricName)
                .tags(tags.length > 0 ? new String[]{"type", tags[0]} : new String[]{})
                .register(meterRegistry)
                .increment();
        } catch (Exception e) {
            log.warn("Failed to increment metric: {}", metricName, e);
        }
    }
}
