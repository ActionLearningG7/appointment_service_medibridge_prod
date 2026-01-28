package com.medibridge.appointment_service_medibridge.service;

import com.medibridge.appointment_service_medibridge.api.dto.response.AdminQueueMonitorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket Message Service
 * Handles real-time push notifications for queue updates via STOMP
 *
 * Topics:
 * - /topic/admin/queues - Broadcasts all queue updates
 * - /topic/admin/queues/{queueId} - Broadcasts specific queue updates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketQueueService {

    private final SimpMessagingTemplate messagingTemplate;
    private final AdminQueueMonitoringService monitoringService;

    /**
     * Broadcast all active queues to admin subscribers
     */
    public void broadcastAllQueues() {
        log.debug("Broadcasting all active queues to admin subscribers");
        try {
            List<AdminQueueMonitorResponse> queues = monitoringService.getAllActiveQueuesWithStats();
            messagingTemplate.convertAndSend("/topic/admin/queues", queues);
        } catch (Exception e) {
            log.error("Error broadcasting all queues", e);
        }
    }

    /**
     * Broadcast specific queue update
     */
    public void broadcastQueueUpdate(UUID queueId) {
        log.debug("Broadcasting update for queue: {}", queueId);
        try {
            AdminQueueMonitorResponse queue = monitoringService.getQueueWithStats(queueId);
            messagingTemplate.convertAndSend("/topic/admin/queues/" + queueId, queue);
            // Also broadcast to the general queues topic
            broadcastAllQueues();
        } catch (Exception e) {
            log.error("Error broadcasting queue update for {}", queueId, e);
        }
    }

    /**
     * Broadcast queue statistics summary
     */
    public void broadcastQueueStatistics() {
        log.debug("Broadcasting queue statistics summary");
        try {
            Map<String, Object> stats = monitoringService.getQueueStatisticsSummary();
            messagingTemplate.convertAndSend("/topic/admin/queues/statistics", stats);
        } catch (Exception e) {
            log.error("Error broadcasting queue statistics", e);
        }
    }

    /**
     * Broadcast when a patient joins the queue
     */
    public void broadcastQueueEntryUpdate(UUID queueId) {
        log.debug("Broadcasting queue entry update for queue: {}", queueId);
        broadcastQueueUpdate(queueId);
    }

    /**
     * Broadcast when queue status changes (open/pause/close)
     */
    public void broadcastQueueStatusChange(UUID queueId) {
        log.debug("Broadcasting queue status change for queue: {}", queueId);
        broadcastQueueUpdate(queueId);
    }
}
