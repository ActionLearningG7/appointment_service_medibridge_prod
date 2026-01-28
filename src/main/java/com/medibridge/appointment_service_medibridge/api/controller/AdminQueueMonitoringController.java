package com.medibridge.appointment_service_medibridge.api.controller;

import com.medibridge.appointment_service_medibridge.api.dto.response.AdminQueueMonitorResponse;
import com.medibridge.appointment_service_medibridge.service.AdminQueueMonitoringService;
import com.medibridge.appointment_service_medibridge.service.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin Queue Monitoring Controller
 * REST endpoints for real-time queue monitoring in admin dashboard
 *
 * All endpoints require ADMIN role
 */
@RestController
@RequestMapping("/admin/queues")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminQueueMonitoringController {

    private final AdminQueueMonitoringService adminQueueMonitoringService;
    private final QueueService queueService;

    /**
     * GET /api/v1/admin/queues
     * Get all active queues for today with statistics
     *
     * @return List of AdminQueueMonitorResponse with queue statistics
     */
    @GetMapping
    public ResponseEntity<List<AdminQueueMonitorResponse>> getAllActiveQueues() {
        log.debug("Admin requesting all active queues");
        List<AdminQueueMonitorResponse> queues = adminQueueMonitoringService.getAllActiveQueuesWithStats();
        return ResponseEntity.ok(queues);
    }

    /**
     * GET /api/v1/admin/queues/{queueId}
     * Get a specific queue with detailed statistics
     *
     * @param queueId Queue ID
     * @return AdminQueueMonitorResponse with detailed queue information
     */
    @GetMapping("/{queueId}")
    public ResponseEntity<AdminQueueMonitorResponse> getQueueDetails(@PathVariable UUID queueId) {
        log.debug("Admin requesting details for queue: {}", queueId);
        AdminQueueMonitorResponse queue = adminQueueMonitoringService.getQueueWithStats(queueId);
        return ResponseEntity.ok(queue);
    }

    /**
     * GET /api/v1/admin/queues/statistics
     * Get aggregate statistics for all queues
     *
     * @return Map with summary statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getQueueStatistics() {
        log.debug("Admin requesting queue statistics summary");
        Map<String, Object> stats = adminQueueMonitoringService.getQueueStatisticsSummary();
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/v1/admin/queues/{queueId}/entries
     * Get all entries (patients) in a queue
     *
     * @param queueId Queue ID
     * @return List of queue entries with patient details
     */
    @GetMapping("/{queueId}/entries")
    public ResponseEntity<?> getQueueEntries(@PathVariable UUID queueId) {
        log.debug("Admin requesting entries for queue: {}", queueId);
        // This will be implemented when queue entries DTO is available
        return ResponseEntity.ok(adminQueueMonitoringService.getQueueEntries(queueId));
    }

    /**
     * GET /api/v1/admin/queues/{queueId}/waiting
     * Get waiting entries (patients currently in queue)
     *
     * @param queueId Queue ID
     * @return List of waiting queue entries
     */
    @GetMapping("/{queueId}/waiting")
    public ResponseEntity<?> getWaitingEntries(@PathVariable UUID queueId) {
        log.debug("Admin requesting waiting entries for queue: {}", queueId);
        return ResponseEntity.ok(adminQueueMonitoringService.getWaitingEntries(queueId));
    }

    /**
     * POST /api/v1/admin/queues/{queueId}/pause
     * Pause a queue (temporarily stop accepting patients)
     *
     * @param queueId Queue ID
     * @return Success response
     */
    @PostMapping("/{queueId}/pause")
    public ResponseEntity<Map<String, Object>> pauseQueue(@PathVariable UUID queueId) {
        log.info("Admin pausing queue: {}", queueId);
        queueService.pauseQueue(queueId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Queue paused successfully",
                "queueId", queueId
        ));
    }

    /**
     * POST /api/v1/admin/queues/{queueId}/resume
     * Resume a paused queue
     *
     * @param queueId Queue ID
     * @return Success response
     */
    @PostMapping("/{queueId}/resume")
    public ResponseEntity<Map<String, Object>> resumeQueue(@PathVariable UUID queueId) {
        log.info("Admin resuming queue: {}", queueId);
        queueService.resumeQueue(queueId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Queue resumed successfully",
                "queueId", queueId
        ));
    }
}