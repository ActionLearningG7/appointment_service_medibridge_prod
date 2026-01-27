package com.medibridge.appointment_service_medibridge.service;

import com.medibridge.appointment_service_medibridge.domain.entity.Appointment;
import com.medibridge.appointment_service_medibridge.domain.enums.ActionType;
import com.medibridge.appointment_service_medibridge.domain.enums.AppointmentStatus;
import com.medibridge.appointment_service_medibridge.domain.repository.AppointmentRepository;
import com.medibridge.appointment_service_medibridge.kafka.event.BaseEvent;
import com.medibridge.appointment_service_medibridge.kafka.producer.DomainEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DomainEventProducer domainEventProducer;
    private final AuditService auditService;

    @Value("${appointment.kafka.topics.appointments}")
    private String appointmentsTopic;

    /**
     * Create a new appointment request
     */
    @Transactional
    public Appointment createAppointment(UUID patientId, UUID doctorId, LocalDate date, String reason) {
        // Validation
        if (patientId == null) {
            throw new IllegalArgumentException("Patient ID cannot be null");
        }
        if (doctorId == null) {
            throw new IllegalArgumentException("Doctor ID cannot be null");
        }
        if (date == null) {
            throw new IllegalArgumentException("Appointment date cannot be null");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Appointment date must be today or in the future");
        }

        // Check for duplicate appointment on same day with same doctor
        boolean appointmentExists = appointmentRepository.existsByPatientIdAndDoctorIdAndAppointmentDateAndStatusIn(
                patientId,
                doctorId,
                date,
                List.of(AppointmentStatus.REQUESTED, AppointmentStatus.QUEUED, AppointmentStatus.IN_PROGRESS)
        );

        if (appointmentExists) {
            throw new IllegalArgumentException("You already have an appointment with this doctor on this date");
        }

        // Create appointment with all required fields
        Appointment appointment = Appointment.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .appointmentDate(date)
                .reasonForVisit(reason)
                .appointmentType("VIRTUAL")  // Always VIRTUAL for this service
                .status(AppointmentStatus.REQUESTED)
                .createdBy(patientId)
                .build();

        Appointment saved = appointmentRepository.save(appointment);

        // Audit
        auditService.logAction(ActionType.APPOINTMENT_CREATED, "APPOINTMENT", saved.getId(),
                "Appointment requested via Service", null);

        // Publish Event
        publishEvent(saved, "AppointmentCreated");

        return saved;
    }

    /**
     * Cancel an appointment
     */
    @Transactional
    public void cancelAppointment(UUID appointmentId, UUID cancelledBy, String reason) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel completed appointment");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledBy(cancelledBy);
        appointment.setCancellationReason(reason);

        appointmentRepository.save(appointment);

        auditService.logAction(ActionType.APPOINTMENT_CANCELLED, "APPOINTMENT", appointmentId,
                "Cancelled by " + cancelledBy, reason);

        publishEvent(appointment, "AppointmentCancelled");
    }

    /**
     * Update status
     */
    @Transactional
    public void updateStatus(UUID appointmentId, AppointmentStatus status) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        appointment.setStatus(status);
        appointmentRepository.save(appointment);
    }

    public Appointment getAppointment(UUID id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
    }

    @Transactional(readOnly = true)
    public List<Appointment> getAppointmentsForPatient(UUID patientId) {
        return appointmentRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    @Transactional(readOnly = true)
    public Appointment getAppointmentForPatient(UUID appointmentId, UUID patientId) {
        Appointment appointment = getAppointment(appointmentId);
        if (!appointment.getPatientId().equals(patientId)) {
            throw new AccessDeniedException("You are not allowed to view this appointment");
        }
        return appointment;
    }

    private void publishEvent(Appointment appointment, String eventType) {
        BaseEvent event = BaseEvent.builder()
                .eventType(eventType)
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .aggregateId(appointment.getId().toString())
                .build();

        domainEventProducer.publish(appointmentsTopic, "APPOINTMENT", appointment.getId(), event);
    }
}
