package com.medibridge.appointment_service_medibridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enterprise Appointment & Virtual Queue Microservice
 * 
 * Responsibilities:
 * - Appointment lifecycle management (virtual consultations)
 * - Queue lifecycle management (doctor-specific daily queues)
 * - Queue entry ordering & token allocation
 * - Video session metadata orchestration
 * - Real-time UI updates via WebSocket
 * - Domain event publishing to Kafka
 * 
 * Architecture:
 * - MySQL: Source of truth for all data
 * - Redis: Fast path for queue state, locks, idempotency
 * - Kafka: Domain events for async integration
 * - WebSocket (STOMP): Real-time queue updates
 * 
 * @author MediBridge Platform Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableFeignClients
@EnableJpaAuditing
@EnableKafka
@EnableAsync
@EnableScheduling
public class AppointmentServiceMedibridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppointmentServiceMedibridgeApplication.class, args);
    }

}
