package com.medibridge.appointment_service_medibridge.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka Configuration
 * 
 * Defines topics and configuration.
 */
@Configuration
public class KafkaConfig {

    @Value("${appointment.kafka.topics.appointments}")
    private String appointmentsTopic;

    @Value("${appointment.kafka.topics.queue}")
    private String queueTopic;

    @Value("${appointment.kafka.topics.video}")
    private String videoTopic;

    @Bean
    public NewTopic appointmentsTopic() {
        return TopicBuilder.name(appointmentsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic queueTopic() {
        return TopicBuilder.name(queueTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic videoTopic() {
        return TopicBuilder.name(videoTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
