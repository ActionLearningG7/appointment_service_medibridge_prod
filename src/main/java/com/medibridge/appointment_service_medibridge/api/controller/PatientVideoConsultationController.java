package com.medibridge.appointment_service_medibridge.api.controller;

import com.medibridge.appointment_service_medibridge.api.dto.response.VideoSessionResponse;
import com.medibridge.appointment_service_medibridge.security.jwt.JwtUserPrincipal;
import com.medibridge.appointment_service_medibridge.service.VideoConsultationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Video Consultation REST API for Patients
 *
 * Endpoints:
 * - Get active video session
 * - Get session details
 */
@RestController
@RequestMapping("/patients/consultations")
@RequiredArgsConstructor
@Slf4j
public class PatientVideoConsultationController {

    private final VideoConsultationService videoService;

    /**
     * Get active video session for patient
     *
     * GET /api/v1/patients/consultations/active-video
     */
    @GetMapping("/active-video")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<VideoSessionResponse> getActiveSession(Authentication authentication) {
        try {
            log.info("=== Patient requesting active video session ===");
            
            if (authentication == null) {
                log.error("Authentication is NULL!");
                throw new SecurityException("User not authenticated");
            }
            
            UUID patientId = extractUserId(authentication);
            log.info("Patient ID extracted: {}", patientId);
            
            if (patientId == null) {
                log.error("Patient ID is NULL after extraction!");
                return ResponseEntity.badRequest().build();
            }

            log.info("Calling videoService.getActiveSession with patientId: {}", patientId);
            VideoSessionResponse response = videoService.getActiveSession(patientId, "PATIENT");
            log.info("Response received: {}", response);

            if (response == null) {
                log.info("No active session found for patient: {}", patientId);
                return ResponseEntity.noContent().build();
            }

            log.info("=== Returning active session for patient: {} ===", patientId);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            log.error("Security error: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting active video session: {}", e.getMessage());
            log.error("Full stack trace:", e);
            throw new RuntimeException("Failed to get active session: " + e.getMessage(), e);
        }
    }

    /**
     * Extract user ID from authentication
     */
    private UUID extractUserId(Authentication authentication) {
        if (authentication == null) {
            throw new SecurityException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();

        // Handle JwtUserPrincipal
        if (principal instanceof JwtUserPrincipal) {
            return ((JwtUserPrincipal) principal).getId();
        }

        // Fallback to parsing from name
        if (authentication.getName() == null) {
            throw new SecurityException("User ID not found in authentication");
        }

        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            throw new SecurityException("Invalid user ID in authentication: " + authentication.getName());
        }
    }
}
