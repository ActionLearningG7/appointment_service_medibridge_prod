package com.medibridge.appointment_service_medibridge.api.controller;

import com.medibridge.appointment_service_medibridge.api.dto.request.JoinQueueRequest;
import com.medibridge.appointment_service_medibridge.api.dto.response.QueueEntryResponse;
import com.medibridge.appointment_service_medibridge.api.mapper.AppMapper;
import com.medibridge.appointment_service_medibridge.domain.entity.QueueEntry;
import com.medibridge.appointment_service_medibridge.service.QueueEntryService;
import com.medibridge.appointment_service_medibridge.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/") // Context path /api/v1 is already set in application.yml
@RequiredArgsConstructor
public class PatientController {

    private final QueueEntryService queueEntryService;
    private final AppMapper mapper;

    @PostMapping("/queues/join")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<QueueEntryResponse> joinQueue(@Valid @RequestBody JoinQueueRequest request) {
        UUID patientId = SecurityUtils.getCurrentUserId();

        var entry = queueEntryService.joinQueue(
                request.getAppointmentId(), patientId, request.isEmergency());
        return ResponseEntity.ok(mapper.toQueueEntryResponse(entry));
    }

    @PostMapping("/queues/{id}/leave")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Void> leaveQueue(@PathVariable UUID id) {
        UUID patientId = SecurityUtils.getCurrentUserId();
        queueEntryService.leaveQueue(id, patientId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/queues/me/active")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<QueueEntryResponse> getMyActiveQueue() {
        UUID patientId = SecurityUtils.getCurrentUserId();
        Optional<QueueEntry> activeEntry = queueEntryService.getPatientActiveQueue(patientId);

        if (activeEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(mapper.toQueueEntryResponse(activeEntry.get()));
    }
}
