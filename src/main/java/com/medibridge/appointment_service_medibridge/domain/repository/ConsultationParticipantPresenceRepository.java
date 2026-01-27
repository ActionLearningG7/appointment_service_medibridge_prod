package com.medibridge.appointment_service_medibridge.domain.repository;

import com.medibridge.appointment_service_medibridge.domain.entity.ConsultationParticipantPresence;
import com.medibridge.appointment_service_medibridge.domain.enums.ConnectionState;
import com.medibridge.appointment_service_medibridge.domain.enums.ParticipantRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsultationParticipantPresenceRepository extends JpaRepository<ConsultationParticipantPresence, UUID> {

    /**
     * Find participant presence in session
     */
    Optional<ConsultationParticipantPresence> findBySessionIdAndParticipantId(UUID sessionId, UUID participantId);

    /**
     * Find all participants in session
     */
    List<ConsultationParticipantPresence> findBySessionId(UUID sessionId);

    /**
     * Find connected participants in session
     */
    List<ConsultationParticipantPresence> findBySessionIdAndConnectionState(UUID sessionId, ConnectionState state);

    /**
     * Find participant by role in session
     */
    Optional<ConsultationParticipantPresence> findBySessionIdAndRole(UUID sessionId, ParticipantRole role);

    /**
     * Count connected participants in session
     */
    @Query("SELECT COUNT(p) FROM ConsultationParticipantPresence p WHERE p.sessionId = :sessionId AND p.connectionState = 'CONNECTED' AND p.leftAt IS NULL")
    long countConnectedParticipants(@Param("sessionId") UUID sessionId);

    /**
     * Find stale connections (no ping for specified duration)
     */
    @Query("SELECT p FROM ConsultationParticipantPresence p WHERE p.connectionState = 'CONNECTED' AND p.lastPingAt < :threshold")
    List<ConsultationParticipantPresence> findStaleConnections(@Param("threshold") LocalDateTime threshold);

    /**
     * Delete all presence records for a session (cleanup)
     */
    void deleteBySessionId(UUID sessionId);
}
