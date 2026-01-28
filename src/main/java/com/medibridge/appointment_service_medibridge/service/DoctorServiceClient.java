package com.medibridge.appointment_service_medibridge.service;

import com.medibridge.appointment_service_medibridge.api.dto.response.DoctorInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * Doctor Service Client
 * Fetches doctor information from User Service via REST API
 * Uses actual doctor profile data instead of defaults
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorServiceClient {

    private final RestTemplate restTemplate;

    private static final String USER_SERVICE_URL = "http://localhost:8082/api/v1";
    // This can be configured in application.yml if needed
    // For Eureka discovery, use: http://user-service/api/v1

    /**
     * Get doctor information by ID
     * Caches the result for 1 hour
     * Fetches actual doctor profile from User Service
     *
     * @param doctorId The doctor user ID
     * @return DoctorInfoDTO with actual doctor details
     */
    @Cacheable(value = "doctorInfo", key = "#doctorId", unless = "#result == null")
    public DoctorInfoDTO getDoctorById(UUID doctorId) {
        try {
            String url = USER_SERVICE_URL + "/doctors/" + doctorId;
            log.debug("Fetching doctor information from User Service: {}", url);

            // Call User Service - it returns ApiResponse<DoctorProfileResponse>
            // We need to extract the data from the response wrapper
            var response = restTemplate.getForObject(url, ApiResponseWrapper.class);

            if (response != null && response.getData() != null) {
                DoctorProfile doctorProfile = response.getData();

                // Build DoctorInfoDTO from DoctorProfile
                DoctorInfoDTO doctor = DoctorInfoDTO.builder()
                        .id(doctorId)
                        .firstName(doctorProfile.getFirstName() != null ? doctorProfile.getFirstName() : "")
                        .lastName(doctorProfile.getLastName() != null ? doctorProfile.getLastName() : "")
                        .specialization(doctorProfile.getSpecialization() != null ?
                            doctorProfile.getSpecialization().toString() : "")
                        .email(doctorProfile.getEmail() != null ? doctorProfile.getEmail() : "")
//                        .phoneNumber(doctorProfile.getPhoneNumber() != null ? doctorProfile.getPhoneNumber() : "")
                        .build();

                log.info("Successfully fetched doctor from User Service: {} {} - {}",
                    doctor.getFirstName(), doctor.getLastName(), doctor.getSpecialization());
                return doctor;
            } else {
                log.warn("User Service returned null data for doctor {}", doctorId);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch doctor {} from User Service ({}): {}",
                doctorId, e.getClass().getSimpleName(), e.getMessage());
        }

        // Return empty DoctorInfoDTO if fetch fails (let AdminQueueMonitoringService handle defaults)
        log.info("Returning empty doctor info for: {} (will use fallback)", doctorId);
        return DoctorInfoDTO.builder()
                .id(doctorId)
                .firstName("")
                .lastName("")
                .specialization("")
                .build();
    }

    /**
     * Wrapper class for ApiResponse from User Service
     * Structure: { "success": true, "data": { ... }, "message": "..." }
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ApiResponseWrapper {
        private boolean success;
        private DoctorProfile data;
        private String message;
    }

    /**
     * Doctor Profile structure matching User Service response
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DoctorProfile {
        private UUID userId;
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private Object specialization;  // Can be enum or string
        private String bio;
        private String photoUrl;
        private Boolean active;
    }
}