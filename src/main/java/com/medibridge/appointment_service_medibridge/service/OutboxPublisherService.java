package com.medibridge.appointment_service_medibridge.service;

import com.medibridge.appointment_service_medibridge.domain.entity.OutboxEvent;
import com.medibridge.appointment_service_medibridge.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox Publisher Service
 * 
 * Scheduled job to read pending events from 'outbox_events' and publish them to
 * Kafka.
 * Ensures "At-Least-Once" delivery.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Poll pending events every 5 seconds
     */
    @Scheduled(fixedDelay = 5000) // 5 seconds
    @Transactional
    public void publishPendingEvents() {
        // Fetch oldest 50 pending events
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc(
                PageRequest.of(0, 50));

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending events to publish", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                // Publish to Kafka
                // Key is aggregateId to ensure ordering per entity
                kafkaTemplate.send(event.getTopic(), event.getAggregateId().toString(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                markAsPublished(event);
                            } else {
                                log.error("Failed to publish event ID: {}", event.getId(), ex);
                                markAsFailed(event, ex.getMessage());
                            }
                        });

            } catch (Exception e) {
                log.error("Exception sending event ID: {}", event.getId(), e);
            }
        }
    }

    private void markAsPublished(OutboxEvent event) {
        event.setPublished(true);
        event.setPublishedAt(LocalDateTime.now());
        outboxEventRepository.save(event);
        log.debug("Event ID {} marked as published", event.getId());
    }

    private void markAsFailed(OutboxEvent event, String error) {
        event.setErrorMessage(error);
        outboxEventRepository.save(event);
    }
}
