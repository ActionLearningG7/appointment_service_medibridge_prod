package com.medibridge.appointment_service_medibridge.api.controller;

import com.medibridge.appointment_service_medibridge.api.dto.request.CreateAppointmentRequest;
import com.medibridge.appointment_service_medibridge.api.dto.response.AppointmentResponse;
import com.medibridge.appointment_service_medibridge.api.mapper.AppMapper;
import com.medibridge.appointment_service_medibridge.service.AppointmentService;
import com.medibridge.appointment_service_medibridge.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AppMapper mapper;

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<AppointmentResponse> createAppointment(@Valid @RequestBody CreateAppointmentRequest request) {
        UUID patientId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(appointmentService.createAppointment(
                patientId,
                request.getDoctorId(),
                request.getDate(),
                request.getReason()));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments() {
        UUID patientId = SecurityUtils.getCurrentUserId();
        var appointments = appointmentService.getAppointmentsForPatient(patientId);
        return ResponseEntity.ok(mapper.toAppointmentResponses(appointments));
    }

    @GetMapping("/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<AppointmentResponse>> getDoctorAppointments(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status) {
        UUID doctorId = SecurityUtils.getCurrentUserId();
        LocalDate localDate = (date != null) ? LocalDate.parse(date) : null;
        var appointments = appointmentService.getAppointmentsForDoctor(doctorId, localDate, status);
        return ResponseEntity.ok(mapper.toAppointmentResponses(appointments));
    }

    @GetMapping("/{appointmentId}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<AppointmentResponse> getAppointment(@PathVariable UUID appointmentId) {
        UUID patientId = SecurityUtils.getCurrentUserId();
        var appointment = appointmentService.getAppointmentForPatient(appointmentId, patientId);
        return ResponseEntity.ok(mapper.toAppointmentResponse(appointment));
    }
}
