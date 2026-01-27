package com.medibridge.appointment_service_medibridge.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service", url = "${application.feign.user-service-url:http://localhost:8081/api/v1}")
public interface UserServiceClient {

    @GetMapping("/doctors/{id}/exists")
    boolean doesDoctorExist(@PathVariable("id") UUID id);

    @GetMapping("/patients/{id}/exists")
    boolean doesPatientExist(@PathVariable("id") UUID id);

    @GetMapping("/doctors/{id}/active")
    boolean isDoctorActive(@PathVariable("id") UUID id);
}
