package com.medibridge.appointment_service_medibridge.api.controller;

import com.medibridge.appointment_service_medibridge.api.dto.response.AppointmentResponse;
import com.medibridge.appointment_service_medibridge.api.mapper.AppMapper;
import com.medibridge.appointment_service_medibridge.service.AppointmentService;
import com.medibridge.appointment_service_medibridge.service.QueueService;
import com.medibridge.appointment_service_medibridge.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final QueueService queueService;
    private final AppointmentService appointmentService;
    private final AppMapper mapper;

    @PostMapping("/queues/{queueId}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> forceCloseQueue(@PathVariable UUID queueId) {
        UUID adminId = SecurityUtils.getCurrentUserId();
        queueService.closeQueue(queueId, adminId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/appointments/{appointmentId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cancelAppointment(@PathVariable UUID appointmentId, @RequestParam String reason) {
        UUID adminId = SecurityUtils.getCurrentUserId();
        appointmentService.cancelAppointment(appointmentId, adminId, reason);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/appointments/{appointmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponse> getAppointment(@PathVariable UUID appointmentId) {
        var appointment = appointmentService.getAppointment(appointmentId);
        return ResponseEntity.ok(mapper.toAppointmentResponse(appointment));
    }
}
