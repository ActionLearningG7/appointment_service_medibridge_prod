package com.medibridge.appointment_service_medibridge.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medibridge.appointment_service_medibridge.domain.entity.OutboxEvent;
import com.medibridge.appointment_service_medibridge.domain.repository.OutboxEventRepository;
import com.medibridge.appointment_service_medibridge.kafka.event.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Event Producer (Transactional Outbox Pattern)
 * 
 * Instead of publishing directly to Kafka, this service persists events to the
 * 'outbox_events' table.
 * This guarantees that events are only published if the business transaction
 * succeeds.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DomainEventProducer {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Publish an event (persists to outbox)
     * Must be called within an existing transaction!
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String topic, String aggregateType, UUID aggregateId, BaseEvent event) {
        try {
            // Ensure event metadata is set
            if (event.getEventId() == null)
                event.setEventId(UUID.randomUUID());
            if (event.getTimestamp() == null)
                event.setTimestamp(LocalDateTime.now());
            if (event.getVersion() == null)
                event.setVersion("1.0");
            if (event.getAggregateId() == null)
                event.setAggregateId(aggregateId.toString());

            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(event.getEventType())
                    .topic(topic)
                    .payload(payload)
                    .published(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.debug("Event persisted to outbox: {} (ID: {})", event.getEventType(), aggregateId);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", event, e);
            throw new RuntimeException("Event serialization failed", e);
        }
    }
}
