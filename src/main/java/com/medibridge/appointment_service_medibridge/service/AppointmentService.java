package com.medibridge.appointment_service_medibridge.service;

import com.medibridge.appointment_service_medibridge.api.dto.response.AppointmentResponse;
import com.medibridge.appointment_service_medibridge.api.mapper.AppMapper;

import com.medibridge.appointment_service_medibridge.domain.entity.Appointment;
import com.medibridge.appointment_service_medibridge.domain.enums.ActionType;
import com.medibridge.appointment_service_medibridge.domain.enums.AppointmentStatus;
import com.medibridge.appointment_service_medibridge.domain.repository.AppointmentRepository;
import com.medibridge.appointment_service_medibridge.dto.payment.CheckoutRequest;
import com.medibridge.appointment_service_medibridge.dto.payment.CheckoutResponse;
import com.medibridge.appointment_service_medibridge.dto.payment.CreateInvoiceRequest;
import com.medibridge.appointment_service_medibridge.dto.payment.InvoiceDTO;
import com.medibridge.appointment_service_medibridge.kafka.event.BaseEvent;
import com.medibridge.appointment_service_medibridge.kafka.producer.DomainEventProducer;
import com.medibridge.appointment_service_medibridge.service.client.PaymentServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
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
    private final PaymentServiceClient paymentServiceClient;
    private final AppMapper mapper;

    @Value("${appointment.kafka.topics.appointments}")
    private String appointmentsTopic;

    /**
     * Create a new appointment request
     */
    @Transactional
    public AppointmentResponse createAppointment(UUID patientId, UUID doctorId, LocalDate date, String reason) {
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
                List.of(AppointmentStatus.REQUESTED, AppointmentStatus.QUEUED, AppointmentStatus.IN_PROGRESS));

        if (appointmentExists) {
            throw new IllegalArgumentException("You already have an appointment with this doctor on this date");
        }

        // Create appointment with all required fields
        Appointment appointment = Appointment.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .appointmentDate(date)
                .reasonForVisit(reason)
                .appointmentType("VIRTUAL") // Always VIRTUAL for this service
                .status(AppointmentStatus.PAYMENT_PENDING)
                .paymentStatus("PENDING")
                .createdBy(patientId)
                .build();

        Appointment saved = appointmentRepository.save(appointment);

        String checkoutUrl = null;
        String invoiceId = null;

        // Create Invoice in Payment Service
        try {
            CreateInvoiceRequest invoiceRequest = CreateInvoiceRequest.builder()
                    .patientId(patientId.toString())
                    .description("Teleconsultation Appointment - " + date)
                    .dueDate(LocalDateTime.now().plusHours(24))
                    .serviceType("APPOINTMENT")
                    .serviceRefId(saved.getId().toString())
                    .items(List.of(CreateInvoiceRequest.CreateInvoiceItemRequest.builder()
                            .description("Doctor Consultation Fee")
                            .quantity(1)
                            .unitPrice(new BigDecimal("50.00")) // Default fee
                            .category("CONSULTATION")
                            .build()))
                    .build();

            InvoiceDTO invoice = paymentServiceClient.createInvoice(invoiceRequest);
            invoiceId = invoice.getId().toString();
            saved.setInvoiceId(invoiceId);
            appointmentRepository.save(saved);
            log.info("Linked invoice {} to appointment {}", invoice.getId(), saved.getId());

            // Get Checkout Link
            CheckoutRequest checkoutRequest = CheckoutRequest.builder()
                    .invoiceId(invoice.getId())
                    .successUrl("http://localhost:3000/payments/success?ref=APPOINTMENT&id=" + saved.getId())
                    .cancelUrl("http://localhost:3000/payments/cancel?ref=APPOINTMENT&id=" + saved.getId())
                    .build();

            CheckoutResponse checkoutResponse = paymentServiceClient.getCheckoutLink(checkoutRequest);
            checkoutUrl = checkoutResponse.getCheckoutUrl();

        } catch (Exception e) {
            log.error("Failed to create invoice or checkout link for appointment: {}", saved.getId(), e);
            // In a real system, you might want to rollback or handle this asynchronously
        }

        // Audit
        auditService.logAction(ActionType.APPOINTMENT_CREATED, "APPOINTMENT", saved.getId(),
                "Appointment requested via Service - Payment Pending", null);

        // Publish Event
        publishEvent(saved, "AppointmentCreated");

        // Prepare response
        AppointmentResponse response = mapper.toAppointmentResponse(saved);
        response.setPaymentRequired(true);
        response.setInvoiceId(invoiceId);
        response.setCheckoutUrl(checkoutUrl);

        return response;
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

    @Transactional
    public void processPaymentSuccess(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (appointment.getStatus() == AppointmentStatus.PAYMENT_PENDING) {
            appointment.setStatus(AppointmentStatus.CONFIRMED);
            appointment.setPaymentStatus("PAID");
            appointmentRepository.save(appointment);
            log.info("Appointment {} confirmed after successful payment", appointmentId);

            auditService.logAction(ActionType.APPOINTMENT_UPDATED, "APPOINTMENT", appointmentId,
                    "Marked as PAID and CONFIRMED via Payment Service", null);

            publishEvent(appointment, "AppointmentConfirmed");
        }
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
    public List<Appointment> getAppointmentsForDoctor(UUID doctorId, LocalDate date, String status) {
        if (date != null && status != null) {
            AppointmentStatus appointmentStatus = AppointmentStatus.valueOf(status.toUpperCase());
            return appointmentRepository.findDoctorAppointmentsForDay(doctorId, date, List.of(appointmentStatus));
        } else if (date != null) {
            // Find all for day regardless of status, but usually we care about
            // non-cancelled ones
            return appointmentRepository.findDoctorAppointmentsForDay(doctorId, date,
                    List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.QUEUED, AppointmentStatus.IN_PROGRESS,
                            AppointmentStatus.COMPLETED));
        } else {
            // Return all doctor appointments ordered by date
            // Note: findByDoctorId returns Page, so we might want a different method or
            // just use the page
            return appointmentRepository.findByDoctorIdOrderByAppointmentDateDesc(doctorId);
        }
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
