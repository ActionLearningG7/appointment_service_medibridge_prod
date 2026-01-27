package com.medibridge.appointment_service_medibridge.scheduler;

import com.medibridge.appointment_service_medibridge.service.VideoConsultationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for video consultation cleanup
 *
 * Tasks:
 * - Cleanup expired sessions
 * - Cleanup inactive sessions
 * - Cleanup stale participant connections
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VideoConsultationCleanupScheduler {

    private final VideoConsultationService videoService;
    private final com.medibridge.appointment_service_medibridge.service.QueueEntryService queueEntryService;

    /**
     * Cleanup expired sessions every 5 minutes
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000) // 5 min delay, 1 min initial
    public void cleanupExpiredSessions() {
        log.debug("Running scheduled task: cleanup expired sessions");

        try {
            videoService.cleanupExpiredSessions();
        } catch (Exception e) {
            log.error("Error during expired session cleanup", e);
        }
    }

    /**
     * Cleanup inactive sessions every 10 minutes
     */
    @Scheduled(fixedDelay = 600000, initialDelay = 120000) // 10 min delay, 2 min initial
    public void cleanupInactiveSessions() {
        log.debug("Running scheduled task: cleanup inactive sessions");

        try {
            videoService.cleanupInactiveSessions();
        } catch (Exception e) {
            log.error("Error during inactive session cleanup", e);
        }
    }

    /**
     * Cleanup stale queue entries every hour
     */
    @Scheduled(fixedRate = 3600000, initialDelay = 300000) // 1h rate, 5 min initial
    public void cleanupStaleQueueEntries() {
        log.debug("Running scheduled task: cleanup stale queue entries");

        try {
            queueEntryService.cleanupStaleEntries();
        } catch (Exception e) {
            log.error("Error during stale queue cleanup", e);
        }
    }
}
