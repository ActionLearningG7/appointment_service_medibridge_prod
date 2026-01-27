package com.medibridge.appointment_service_medibridge.domain.repository;

import com.medibridge.appointment_service_medibridge.domain.entity.QueueEntry;
import com.medibridge.appointment_service_medibridge.domain.enums.QueueEntryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueueEntryRepository extends JpaRepository<QueueEntry, UUID> {

        // Find entries for a queue
        List<QueueEntry> findByQueueIdOrderByTokenNumberAsc(UUID queueId);

        // Find waiting entries (ordered by priority then token)
        // Emergency comes first
        @Query("SELECT qe FROM QueueEntry qe WHERE qe.queueId = :queueId AND qe.status = 'WAITING' " +
                        "ORDER BY CASE WHEN qe.priority = 'EMERGENCY' THEN 0 ELSE 1 END ASC, qe.tokenNumber ASC")
        List<QueueEntry> findWaitingEntries(@Param("queueId") UUID queueId);

        // Get next patient to call
        @Query(value = "SELECT * FROM queue_entries qe WHERE qe.queue_id = :queueId AND qe.status = 'WAITING' " +
                        "ORDER BY CASE WHEN qe.priority = 'EMERGENCY' THEN 0 ELSE 1 END ASC, qe.token_number ASC LIMIT 1", nativeQuery = true)
        Optional<QueueEntry> findNextWaiting(@Param("queueId") byte[] queueId);

        // Check duplicate
        boolean existsByQueueIdAndPatientIdAndStatusNot(UUID queueId, UUID patientId, QueueEntryStatus status);

        // Find patient's active entry
        Optional<QueueEntry> findByPatientIdAndStatusIn(UUID patientId, List<QueueEntryStatus> statuses);

        // Count active patients ahead of a specific token
        @Query("SELECT COUNT(qe) FROM QueueEntry qe WHERE qe.queueId = :queueId AND qe.status = 'WAITING' AND " +
                        "(qe.priority = 'EMERGENCY' OR (qe.priority = 'NORMAL' AND qe.tokenNumber < :tokenNumber))")
        long countPatientsAhead(@Param("queueId") UUID queueId, @Param("tokenNumber") Integer tokenNumber);

        // Get max token number for generating next token
        @Query("SELECT COALESCE(MAX(qe.tokenNumber), 0) FROM QueueEntry qe WHERE qe.queueId = :queueId")
        Integer findMaxTokenNumber(@Param("queueId") UUID queueId);

        // Find stale entries (waiting or called but from previous days)
        @Query("SELECT qe FROM QueueEntry qe WHERE qe.status IN ('WAITING', 'CALLED') AND qe.joinedAt < :cutoff")
        List<QueueEntry> findStaleEntries(@Param("cutoff") java.time.LocalDateTime cutoff);
}
