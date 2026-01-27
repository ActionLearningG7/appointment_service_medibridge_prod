package com.medibridge.appointment_service_medibridge.domain.repository;

import com.medibridge.appointment_service_medibridge.domain.entity.ConsultationSession;
import com.medibridge.appointment_service_medibridge.domain.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsultationSessionRepository extends JpaRepository<ConsultationSession, UUID> {

    /**
     * Find session by room ID
     */
    Optional<ConsultationSession> findByRoomId(String roomId);

    /**
     * Find session by queue entry ID
     */
    Optional<ConsultationSession> findByQueueEntryId(UUID queueEntryId);

    /**
     * Find session by consultation ID
     */
    Optional<ConsultationSession> findByConsultationId(UUID consultationId);

    /**
     * Find active session for doctor
     */
    @Query("SELECT s FROM ConsultationSession s WHERE s.doctorId = :doctorId AND s.status = :status")
    Optional<ConsultationSession> findByDoctorIdAndStatus(@Param("doctorId") UUID doctorId, @Param("status") SessionStatus status);

    /**
     * Find active session for patient
     */
    @Query("SELECT s FROM ConsultationSession s WHERE s.patientId = :patientId AND s.status = :status")
    Optional<ConsultationSession> findByPatientIdAndStatus(@Param("patientId") UUID patientId, @Param("status") SessionStatus status);

    /**
     * Find all active sessions
     */
    List<ConsultationSession> findByStatus(SessionStatus status);

    /**
     * Find expired sessions that are still marked as active
     */
    @Query("SELECT s FROM ConsultationSession s WHERE s.status = :status AND s.tokenExpiresAt < :now")
    List<ConsultationSession> findExpiredSessions(@Param("status") SessionStatus status, @Param("now") LocalDateTime now);

    /**
     * Find inactive sessions (no activity for specified duration)
     */
    @Query("SELECT s FROM ConsultationSession s WHERE s.status = :status AND s.lastActivityAt < :threshold")
    List<ConsultationSession> findInactiveSessions(@Param("status") SessionStatus status, @Param("threshold") LocalDateTime threshold);

    /**
     * Count active sessions for doctor
     */
    long countByDoctorIdAndStatus(UUID doctorId, SessionStatus status);

    /**
     * Count active sessions for patient
     */
    long countByPatientIdAndStatus(UUID patientId, SessionStatus status);
}
