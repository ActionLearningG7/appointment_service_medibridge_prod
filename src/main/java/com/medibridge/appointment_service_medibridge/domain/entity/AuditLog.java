package com.medibridge.appointment_service_medibridge.domain.entity;

import com.medibridge.appointment_service_medibridge.domain.enums.ActionType;
import com.medibridge.appointment_service_medibridge.util.UUIDConverter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit Log Entity
 * 
 * Immutable record of all significant actions in the system.
 * Mandatory for hospital compliance.
 * 
 * Indexes:
 * - (actor_id, created_at) - User's activity history
 * - (target_type, target_id) - Entity's history
 * - (action_type, created_at) - Activity type analysis
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_actor", columnList = "actor_id, created_at"),
        @Index(name = "idx_target", columnList = "target_type, target_id"),
        @Index(name = "idx_action", columnList = "action_type, created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Who performed the action (User ID)
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "actor_id", columnDefinition = "BINARY(16)")
    private UUID actorId;

    /**
     * Role of the actor (DOCTOR, PATIENT, ADMIN)
     */
    @Column(name = "actor_role", length = 20)
    private String actorRole;

    /**
     * What action was performed
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", length = 50, nullable = false)
    private ActionType actionType;

    /**
     * Type of entity affected (APPOINTMENT, QUEUE, etc.)
     */
    @Column(name = "target_type", length = 50)
    private String targetType;

    /**
     * ID of the entity affected
     */
    @Convert(converter = UUIDConverter.class)
    @Column(name = "target_id", columnDefinition = "BINARY(16)")
    private UUID targetId;

    /**
     * Human readable summary of the action
     */
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    /**
     * Detailed changes (JSON or text) - Optional
     */
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    /**
     * IP Address of the request
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
