package com.medibridge.appointment_service_medibridge.api.controller;

import com.medibridge.appointment_service_medibridge.api.dto.request.EndVideoConsultationRequest;
import com.medibridge.appointment_service_medibridge.api.dto.request.StartVideoConsultationRequest;
import com.medibridge.appointment_service_medibridge.api.dto.response.VideoSessionResponse;
import com.medibridge.appointment_service_medibridge.security.jwt.JwtUserPrincipal;
import com.medibridge.appointment_service_medibridge.service.VideoConsultationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Video Consultation REST API for Doctors
 *
 * Endpoints:
 * - Start video consultation
 * - End video consultation
 * - Get active session
 */
@RestController
@RequestMapping("/doctors/consultations")
@RequiredArgsConstructor
@Slf4j
public class DoctorVideoConsultationController {

    private final VideoConsultationService videoService;

    /**
     * Start video consultation for queue entry
     *
     * POST /api/v1/doctors/consultations/start-video
     */
    @PostMapping("/start-video")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<VideoSessionResponse> startVideoConsultation(
        @Valid @RequestBody StartVideoConsultationRequest request,
        Authentication authentication
    ) {
        UUID doctorId = extractUserId(authentication);

        log.info("Doctor starting video consultation: doctorId={}, queueEntryId={}",
            doctorId, request.getQueueEntryId());

        VideoSessionResponse response = videoService.startVideoSession(
            request.getQueueEntryId(),
            doctorId,
            request.getConsultationId()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get active video session for doctor
     *
     * GET /api/v1/doctors/consultations/active-video
     */
    @GetMapping("/active-video")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<VideoSessionResponse> getActiveSession(Authentication authentication) {
        UUID doctorId = extractUserId(authentication);

        log.debug("Getting active video session for doctor: {}", doctorId);

        VideoSessionResponse response = videoService.getActiveSession(doctorId, "DOCTOR");

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    /**
     * End video consultation
     *
     * POST /api/v1/doctors/consultations/sessions/{sessionId}/end
     */
    @PostMapping("/sessions/{sessionId}/end")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Void> endVideoConsultation(
        @PathVariable UUID sessionId,
        @Valid @RequestBody(required = false) EndVideoConsultationRequest request,
        Authentication authentication
    ) {
        UUID doctorId = extractUserId(authentication);

        log.info("Doctor ending video consultation: doctorId={}, sessionId={}, notes={}",
            doctorId, sessionId, request != null ? "provided" : "none");

        videoService.endSession(sessionId, doctorId, "DOCTOR");

        return ResponseEntity.ok().build();
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
