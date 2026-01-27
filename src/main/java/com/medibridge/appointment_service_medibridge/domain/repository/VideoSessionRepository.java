package com.medibridge.appointment_service_medibridge.domain.repository;

import com.medibridge.appointment_service_medibridge.domain.entity.VideoSession;
import com.medibridge.appointment_service_medibridge.domain.enums.VideoSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoSessionRepository extends JpaRepository<VideoSession, UUID> {

    Optional<VideoSession> findByAppointmentId(UUID appointmentId);

    Optional<VideoSession> findByQueueEntryId(UUID queueEntryId);

    Optional<VideoSession> findBySessionId(String sessionId);

    List<VideoSession> findByDoctorIdAndStatus(UUID doctorId, VideoSessionStatus status);
}
