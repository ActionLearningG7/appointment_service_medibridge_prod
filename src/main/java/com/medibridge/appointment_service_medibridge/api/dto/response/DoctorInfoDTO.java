package com.medibridge.appointment_service_medibridge.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Doctor Information DTO
 * Used for fetching doctor details from User Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorInfoDTO {

    private UUID id;
    private String firstName;
    private String lastName;
    private String specialization;
    private String qualifications;
    private String email;
    private String phone;
    private String profileImage;

    /**
     * Get full doctor name
     */
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return "Dr. Unknown";
        }
        if (firstName == null) {
            return "Dr. " + lastName;
        }
        if (lastName == null) {
            return "Dr. " + firstName;
        }
        return "Dr. " + firstName + " " + lastName;
    }
}
