package com.medibridge.appointment_service_medibridge.domain.repository;

import com.medibridge.appointment_service_medibridge.domain.entity.VirtualQueue;
import com.medibridge.appointment_service_medibridge.domain.enums.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VirtualQueueRepository extends JpaRepository<VirtualQueue, UUID> {

    Optional<VirtualQueue> findByDoctorIdAndQueueDate(UUID doctorId, LocalDate queueDate);

    @Query("SELECT q FROM VirtualQueue q WHERE q.organizationId = :orgId AND q.queueDate = :date")
    List<VirtualQueue> findAllByOrganizationAndDate(@Param("orgId") UUID orgId, @Param("date") LocalDate date);

    @Query("SELECT q FROM VirtualQueue q WHERE q.doctorId = :doctorId AND q.status = :status")
    List<VirtualQueue> findByDoctorIdAndStatus(@Param("doctorId") UUID doctorId, @Param("status") QueueStatus status);

    boolean existsByDoctorIdAndQueueDate(UUID doctorId, LocalDate queueDate);

    // Find active queues for today
    @Query("SELECT q FROM VirtualQueue q WHERE q.queueDate = CURRENT_DATE AND q.status IN ('OPEN', 'PAUSED')")
    List<VirtualQueue> findActiveQueuesForToday();
}
