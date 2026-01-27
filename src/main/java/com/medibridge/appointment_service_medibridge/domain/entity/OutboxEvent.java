package com.medibridge.appointment_service_medibridge.domain.entity;

import com.medibridge.appointment_service_medibridge.util.UUIDConverter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbox Event Entity (Transactional Outbox Pattern)
 * 
 * Ensures reliable event publishing to Kafka.
 * 1. Business logic updates entity (e.g., Appointment)
 * 2. Event is inserted into this table IN THE SAME TRANSACTION
 * 3. Background job publishes to Kafka and updates 'published' status
 * 
 * Indexes:
 * - (published, created_at) - Find pending events
 */
@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_published", columnList = "published, created_at"),
        @Index(name = "idx_aggregate", columnList = "aggregate_type, aggregate_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Aggregate Type (APPOINTMENT, QUEUE, etc.)
     */
    @Column(name = "aggregate_type", length = 50, nullable = false)
    private String aggregateType;

    /**
     * Aggregate ID
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "aggregate_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID aggregateId;

    /**
     * Event Type (AppointmentCreated, QueueJoined, etc.)
     */
    @Column(name = "event_type", length = 100, nullable = false)
    private String eventType;

    /**
     * Event Payload (JSON)
     */
    @Column(name = "payload", columnDefinition = "JSON", nullable = false)
    private String payload;

    /**
     * Kafka Topic
     */
    @Column(name = "topic", length = 100, nullable = false)
    private String topic;

    /**
     * Whether the event has been published to Kafka
     */
    @Column(name = "published", nullable = false)
    private boolean published = false;

    /**
     * When it was published
     */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /**
     * Error message if publishing failed
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
