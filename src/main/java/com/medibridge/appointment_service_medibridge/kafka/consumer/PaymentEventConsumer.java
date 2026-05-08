package com.medibridge.appointment_service_medibridge.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medibridge.appointment_service_medibridge.dto.payment.InvoicePaidEvent;
import com.medibridge.appointment_service_medibridge.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final ObjectMapper objectMapper;
    private final AppointmentService appointmentService;

    @KafkaListener(topics = "invoice.paid", groupId = "appointment-payment-group")
    public void consumePaymentEvent(String message) {
        try {
            log.info("Received Payment Paid Event: {}", message);
            InvoicePaidEvent event = objectMapper.readValue(message, InvoicePaidEvent.class);

            if ("APPOINTMENT".equals(event.getServiceType()) && event.getServiceRefId() != null) {
                log.info("Processing successful payment for appointment: {}", event.getServiceRefId());
                appointmentService.processPaymentSuccess(UUID.fromString(event.getServiceRefId()));
            }
        } catch (Exception e) {
            log.error("Failed to process payment event: {}", message, e);
        }
    }
}
