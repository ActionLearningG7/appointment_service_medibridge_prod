package com.medibridge.appointment_service_medibridge.api.controller;

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
}
