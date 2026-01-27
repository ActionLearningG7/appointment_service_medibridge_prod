package com.medibridge.appointment_service_medibridge.api.dto.websocket;

import com.medibridge.appointment_service_medibridge.domain.enums.ParticipantRole;
import com.medibridge.appointment_service_medibridge.domain.enums.SignalingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * WebRTC Signaling Message
 * 
 * Used to exchange SDP (Session Description Protocol) and ICE Candidates.
 * 
 * Flow:
 * 1. Doctor sends OFFER (to PATIENT)
 * 2. Patient receives OFFER, sends ANSWER (to DOCTOR)
 * 3. Both exchange ICE_CANDIDATEs
 * 4. Either sends BYE to end call
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalingMessage {

    /**
     * Type of signal: OFFER, ANSWER, ICE, JOIN, LEAVE, BYE
     */
    private SignalingType type;

    /**
     * Target role (DOCTOR or PATIENT)
     * Used for role-based routing
     */
    private ParticipantRole to;

    /**
     * SDP offer/answer (for OFFER/ANSWER types)
     */
    private String sdp;

    /**
     * ICE candidate JSON string (for ICE type)
     */
    private String candidate;

    /**
     * Who sent this signal
     */
    private UUID senderId;

    /**
     * Client timestamp (for latency tracking)
     */
    private Long clientTs;

    /**
     * Additional payload (flexible JSON string, max 16KB)
     */
    private String payload;
}
