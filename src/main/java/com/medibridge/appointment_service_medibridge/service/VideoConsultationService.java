package com.medibridge.appointment_service_medibridge.service;

import com.medibridge.appointment_service_medibridge.api.dto.response.VideoSessionResponse;
import com.medibridge.appointment_service_medibridge.domain.entity.ConsultationParticipantPresence;
import com.medibridge.appointment_service_medibridge.domain.entity.ConsultationSession;
import com.medibridge.appointment_service_medibridge.domain.entity.QueueEntry;
import com.medibridge.appointment_service_medibridge.domain.enums.*;
import com.medibridge.appointment_service_medibridge.domain.repository.ConsultationParticipantPresenceRepository;
import com.medibridge.appointment_service_medibridge.domain.repository.ConsultationSessionRepository;
import com.medibridge.appointment_service_medibridge.domain.repository.QueueEntryRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Video Consultation Session Service
 *
 * Handles:
 * - Session creation and lifecycle
 * - Participant authorization
 * - Presence tracking
 * - Security validation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoConsultationService {

    private final ConsultationSessionRepository sessionRepository;
    private final ConsultationParticipantPresenceRepository presenceRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final QueueEntryService queueEntryService;
    private final AppointmentService appointmentService;

    private static final int SESSION_TOKEN_VALIDITY_MINUTES = 30;
    private static final int SESSION_INACTIVITY_TIMEOUT_MINUTES = 10;
    private static final int MAX_PAYLOAD_SIZE = 16 * 1024; // 16KB

    /**
     * Start video consultation session
     *
     * @param queueEntryId   Queue entry ID
     * @param doctorId       Doctor user ID
     * @param consultationId Optional consultation ID
     * @return Created session response
     */
    @Transactional
    @Retryable(value = OptimisticLockException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public VideoSessionResponse startVideoSession(UUID queueEntryId, UUID doctorId, UUID consultationId) {
        log.info("Starting video session for queue entry: {}, doctor: {}", queueEntryId, doctorId);

        // Verify queue entry exists and doctor is authorized
        QueueEntry queueEntry = queueEntryRepository.findById(queueEntryId)
                .orElseThrow(() -> new IllegalArgumentException("Queue entry not found: " + queueEntryId));

        if (!queueEntry.getDoctorId().equals(doctorId)) {
            throw new SecurityException("Doctor not authorized for this queue entry");
        }

        // Check if session already exists
        if (sessionRepository.findByQueueEntryId(queueEntryId).isPresent()) {
            throw new IllegalStateException("Video session already exists for this queue entry");
        }

        // Check if doctor already has an active session
        if (sessionRepository.countByDoctorIdAndStatus(doctorId, SessionStatus.ACTIVE) > 0) {
            throw new IllegalStateException("Doctor already has an active video session");
        }

        // Create session
        String roomId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        ConsultationSession session = ConsultationSession.builder()
                .consultationId(consultationId)
                .queueEntryId(queueEntryId)
                .roomId(roomId)
                .status(SessionStatus.ACTIVE) // Changed from CREATED to ACTIVE so patient can immediately see it
                .doctorId(doctorId)
                .patientId(queueEntry.getPatientId())
                .createdAt(now)
                .startedAt(now) // Set started time immediately
                .lastActivityAt(now)
                .tokenExpiresAt(now.plusMinutes(SESSION_TOKEN_VALIDITY_MINUTES))
                .build();

        session = sessionRepository.save(session);

        // Update Appointment Status to IN_PROGRESS
        appointmentService.updateStatus(queueEntry.getAppointmentId(), AppointmentStatus.IN_PROGRESS);

        log.info("Video session created: sessionId={}, roomId={}", session.getId(), roomId);

        return buildSessionResponse(session);
    }

    /**
     * Get active video session for user
     */
    @Transactional(readOnly = true)
    public VideoSessionResponse getActiveSession(UUID userId, String role) {
        log.info("=== Getting active session for user: {}, role: {} ===", userId, role);

        if (userId == null) {
            log.error("userId is NULL!");
            return null;
        }

        if (role == null || role.trim().isEmpty()) {
            log.error("role is NULL or empty!");
            return null;
        }

        ConsultationSession session = null;

        if ("DOCTOR".equals(role)) {
            log.info("BRANCH: Querying for DOCTOR with userId: {}", userId);
            var result = sessionRepository.findByDoctorIdAndStatus(userId, SessionStatus.ACTIVE);
            log.info("Query result present: {}", result.isPresent());
            session = result.orElse(null);
            log.info("Session after query: {}", session != null ? session.getId() : "NULL");
        } else if ("PATIENT".equals(role)) {
            log.info("BRANCH: Querying for PATIENT with userId: {}", userId);
            var result = sessionRepository.findByPatientIdAndStatus(userId, SessionStatus.ACTIVE);
            log.info("Query result present: {}", result.isPresent());
            session = result.orElse(null);
            log.info("Session after query: {}", session != null ? session.getId() : "NULL");
        } else {
            log.error("BRANCH: Invalid role provided: '{}' (length: {})", role, role.length());
            throw new IllegalArgumentException("Invalid role: " + role);
        }

        if (session == null) {
            log.info("=== No active session found for user: {}, role: {} ===", userId, role);
            return null;
        }

        log.info("Found session: {}", session.getId());

        // Check if expired
        if (session.isExpired()) {
            log.warn("Session expired: sessionId={}", session.getId());
            return null;
        }

        log.info("=== Returning active session: {} ===", session.getId());
        return buildSessionResponse(session);
    }

    /**
     * Get session by room ID with authorization check
     */
    @Transactional(readOnly = true)
    public ConsultationSession getSessionByRoomId(String roomId, UUID userId) {
        ConsultationSession session = sessionRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found for room: " + roomId));

        if (!session.isAuthorizedParticipant(userId)) {
            throw new SecurityException("User not authorized for this session");
        }

        if (session.isExpired()) {
            throw new IllegalStateException("Session expired");
        }

        return session;
    }

    /**
     * Join video session (participant connects)
     */
    @Transactional
    public void joinSession(String roomId, UUID participantId, ParticipantRole role, String userAgent) {
        log.info("Participant joining session: roomId={}, participantId={}, role={}", roomId, participantId, role);

        ConsultationSession session = sessionRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found for room: " + roomId));

        // Verify authorization
        if (!session.isAuthorizedParticipant(participantId)) {
            throw new SecurityException("Participant not authorized for this session");
        }

        if (session.isExpired()) {
            throw new IllegalStateException("Session expired");
        }

        // Create or update presence record
        ConsultationParticipantPresence presence = presenceRepository
                .findBySessionIdAndParticipantId(session.getId(), participantId)
                .orElse(ConsultationParticipantPresence.builder()
                        .sessionId(session.getId())
                        .participantId(participantId)
                        .role(role)
                        .connectionState(ConnectionState.CONNECTING)
                        .userAgent(userAgent)
                        .build());

        presence.setConnectionState(ConnectionState.CONNECTED);
        presence.ping();
        presenceRepository.save(presence);

        // Activate session if not already active
        if (session.getStatus() == SessionStatus.CREATED) {
            session.setStatus(SessionStatus.ACTIVE);
            session.setStartedAt(LocalDateTime.now());
            sessionRepository.save(session);
            log.info("Session activated: sessionId={}", session.getId());
        }

        // Update last activity
        session.setLastActivityAt(LocalDateTime.now());
        sessionRepository.save(session);

        log.info("Participant joined successfully: participantId={}, sessionId={}", participantId, session.getId());
    }

    /**
     * Leave video session (participant disconnects)
     */
    @Transactional
    public void leaveSession(String roomId, UUID participantId) {
        log.info("Participant leaving session: roomId={}, participantId={}", roomId, participantId);

        ConsultationSession session = sessionRepository.findByRoomId(roomId)
                .orElse(null);

        if (session == null) {
            log.warn("Session not found for room: {}", roomId);
            return;
        }

        // Update presence
        presenceRepository.findBySessionIdAndParticipantId(session.getId(), participantId)
                .ifPresent(presence -> {
                    presence.disconnect();
                    presenceRepository.save(presence);
                    log.info("Participant presence updated: participantId={}, state=DISCONNECTED", participantId);
                });

        // Update last activity
        session.setLastActivityAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    /**
     * End video session
     */
    @Transactional
    public void endSession(UUID sessionId, UUID requesterId, String role) {
        log.info("Ending session: sessionId={}, requesterId={}, role={}", sessionId, requesterId, role);

        ConsultationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        // Only doctor can end session (or patient can request if policy allows)
        if ("DOCTOR".equals(role) && !session.getDoctorId().equals(requesterId)) {
            throw new SecurityException("Only the assigned doctor can end this session");
        }

        if (session.getStatus() == SessionStatus.ENDED) {
            log.warn("Session already ended: sessionId={}", sessionId);
            return;
        }

        // Mark session as ended
        session.setStatus(SessionStatus.ENDED);
        session.setEndedAt(LocalDateTime.now());
        sessionRepository.save(session);

        // Disconnect all participants
        presenceRepository.findBySessionId(sessionId).forEach(presence -> {
            if (presence.isConnected()) {
                presence.disconnect();
                presenceRepository.save(presence);
            }
        });

        // Complete Queue Entry
        queueEntryService.completeQueueEntry(session.getQueueEntryId());

        log.info("Session ended successfully: sessionId={}", sessionId);
    }

    /**
     * Validate signaling message payload size
     */
    public void validatePayloadSize(String payload) {
        if (payload != null && payload.length() > MAX_PAYLOAD_SIZE) {
            throw new IllegalArgumentException("Payload size exceeds maximum allowed: " + MAX_PAYLOAD_SIZE + " bytes");
        }
    }

    /**
     * Ping session (update last activity)
     */
    @Transactional
    public void pingSession(String roomId, UUID participantId) {
        sessionRepository.findByRoomId(roomId).ifPresent(session -> {
            session.setLastActivityAt(LocalDateTime.now());
            sessionRepository.save(session);

            presenceRepository.findBySessionIdAndParticipantId(session.getId(), participantId)
                    .ifPresent(presence -> {
                        presence.ping();
                        presenceRepository.save(presence);
                    });
        });
    }

    /**
     * Get connected participant count
     */
    @Transactional(readOnly = true)
    public int getConnectedParticipantCount(UUID sessionId) {
        return (int) presenceRepository.countConnectedParticipants(sessionId);
    }

    /**
     * Build session response DTO
     */
    private VideoSessionResponse buildSessionResponse(ConsultationSession session) {
        UUID appointmentId = null;
        if (session.getQueueEntryId() != null) {
            appointmentId = queueEntryRepository.findById(session.getQueueEntryId())
                    .map(QueueEntry::getAppointmentId)
                    .orElse(null);
        }

        return VideoSessionResponse.builder()
                .sessionId(session.getId())
                .roomId(session.getRoomId())
                .queueEntryId(session.getQueueEntryId())
                .consultationId(session.getConsultationId())
                .appointmentId(appointmentId)
                .status(session.getStatus())
                .doctorId(session.getDoctorId())
                .patientId(session.getPatientId())
                .createdAt(session.getCreatedAt())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .expiresAt(session.getTokenExpiresAt())
                .active(session.isActive())
                .wsTopics(VideoSessionResponse.WebSocketTopics.builder()
                        .sendTo("/app/webrtc/" + session.getRoomId() + "/signal")
                        .receiveFrom("/user/webrtc/" + session.getRoomId() + "/signal")
                        .presenceTopic("/topic/consultations/" + session.getRoomId() + "/presence")
                        .build())
                .build();
    }

    /**
     * Cleanup expired sessions (scheduled task)
     */
    @Transactional
    public void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        List<ConsultationSession> expiredSessions = sessionRepository.findExpiredSessions(SessionStatus.ACTIVE, now);

        for (ConsultationSession session : expiredSessions) {
            log.info("Cleaning up expired session: sessionId={}", session.getId());
            session.setStatus(SessionStatus.ENDED);
            session.setEndedAt(now);
            sessionRepository.save(session);
        }

        log.info("Expired sessions cleaned up: count={}", expiredSessions.size());
    }

    /**
     * Cleanup inactive sessions (scheduled task)
     */
    @Transactional
    public void cleanupInactiveSessions() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(SESSION_INACTIVITY_TIMEOUT_MINUTES);
        List<ConsultationSession> inactiveSessions = sessionRepository.findInactiveSessions(SessionStatus.ACTIVE,
                threshold);

        for (ConsultationSession session : inactiveSessions) {
            log.info("Cleaning up inactive session: sessionId={}, lastActivity={}", session.getId(),
                    session.getLastActivityAt());
            session.setStatus(SessionStatus.ENDED);
            session.setEndedAt(LocalDateTime.now());
            sessionRepository.save(session);
        }

        log.info("Inactive sessions cleaned up: count={}", inactiveSessions.size());
    }
}
