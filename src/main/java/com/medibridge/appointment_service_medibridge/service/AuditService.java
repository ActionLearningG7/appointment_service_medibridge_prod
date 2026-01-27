package com.medibridge.appointment_service_medibridge.service;

import com.medibridge.appointment_service_medibridge.domain.entity.AuditLog;
import com.medibridge.appointment_service_medibridge.domain.enums.ActionType;
import com.medibridge.appointment_service_medibridge.domain.repository.AuditLogRepository;
import com.medibridge.appointment_service_medibridge.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Audit Service
 * 
 * Asynchronously logs significant actions.
 * Never fails the main transaction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(ActionType actionType, String targetType, UUID targetId, String summary, String details) {
        try {
            UUID actorId = getCurrentUserId();
            String actorRole = getCurrentUserRole();
            String ipAddress = "0.0.0.0"; // Requires RequestContextHolder logic if needed

            AuditLog auditLog = AuditLog.builder()
                    .actorId(actorId)
                    .actorRole(actorRole)
                    .actionType(actionType)
                    .targetType(targetType)
                    .targetId(targetId)
                    .summary(summary)
                    .details(details)
                    .ipAddress(ipAddress)
                    .build();

            auditLogRepository.save(auditLog);
            log.info("Audit Logged: {} on {} {}", actionType, targetType, targetId);

        } catch (Exception e) {
            log.error("Failed to save audit log", e);
            // Do not throw exception to avoid impacting main flow
        }
    }

    private UUID getCurrentUserId() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            return null; // System action or unauthenticated
        }
    }

    private String getCurrentUserRole() {
        try {
            return SecurityUtils.getCurrentUserRole();
        } catch (Exception e) {
            return "SYSTEM";
        }
    }
}
