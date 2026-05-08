package com.medibridge.appointment_service_medibridge.api.controller;

import com.medibridge.appointment_service_medibridge.api.dto.request.QueueSettingsRequest;
import com.medibridge.appointment_service_medibridge.api.dto.response.QueueEntryResponse;
import com.medibridge.appointment_service_medibridge.api.dto.response.QueueResponse;
import com.medibridge.appointment_service_medibridge.api.mapper.AppMapper;
import com.medibridge.appointment_service_medibridge.service.QueueEntryService;
import com.medibridge.appointment_service_medibridge.service.QueueService;
import com.medibridge.appointment_service_medibridge.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final QueueService queueService;
    private final QueueEntryService queueEntryService;
    private final AppMapper mapper;

    @PostMapping("/queues/open")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<QueueResponse> openQueue() {
        UUID doctorId = SecurityUtils.getCurrentUserId();
        var queue = queueService.openQueue(doctorId, LocalDate.now(), doctorId);
        return ResponseEntity.ok(mapper.toQueueResponse(queue));
    }

    @PostMapping("/queues/{queueId}/pause")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Void> pauseQueue(@PathVariable UUID queueId) {
        queueService.pauseQueue(queueId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/queues/{queueId}/resume")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Void> resumeQueue(@PathVariable UUID queueId) {
        queueService.resumeQueue(queueId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/queues/{queueId}/close")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Void> closeQueue(@PathVariable UUID queueId) {
        UUID doctorId = SecurityUtils.getCurrentUserId();
        queueService.closeQueue(queueId, doctorId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/queues/{queueId}/settings")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<QueueResponse> updateSettings(@PathVariable UUID queueId,
            @RequestBody QueueSettingsRequest request) {
        var queue = queueService.updateQueueSettings(queueId, request.getAvgConsultationMinutes());
        return ResponseEntity.ok(mapper.toQueueResponse(queue));
    }

    @GetMapping("/queues/today")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<QueueResponse> getTodayQueue() {
        UUID doctorId = SecurityUtils.getCurrentUserId();
        var queue = queueService.getQueueForToday(doctorId);
        return ResponseEntity.ok(mapper.toQueueResponse(queue));
    }

    @GetMapping("/queues/{queueId}/entries")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<QueueEntryResponse>> getQueueEntries(@PathVariable UUID queueId) {
        // TODO: Verify doctor owns this queue
        var entries = queueEntryService.getEntriesForQueue(queueId);
        return ResponseEntity.ok(mapper.toQueueEntryResponses(entries));
    }

    @PostMapping("/queues/{queueId}/call-next")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<QueueEntryResponse> callNext(@PathVariable UUID queueId) {
        // In a real app, verify that the doctor owns this queue!
        // For MVP, we rely on @PreAuthorize("hasRole('DOCTOR')")
        var entry = queueEntryService.callNextPatient(queueId);
        return ResponseEntity.ok(mapper.toQueueEntryResponse(entry));
    }

    @PostMapping("/queues/entries/{entryId}/complete")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Void> completeEntry(@PathVariable UUID entryId) {
        queueEntryService.completeQueueEntry(entryId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/queues/entries/{entryId}/no-show")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Void> markNoShow(@PathVariable UUID entryId) {
        queueEntryService.markAsNoShow(entryId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/queues/entries/{entryId}/skip")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Void> skipPatient(@PathVariable UUID entryId) {
        queueEntryService.skipPatient(entryId);
        return ResponseEntity.ok().build();
    }
}
