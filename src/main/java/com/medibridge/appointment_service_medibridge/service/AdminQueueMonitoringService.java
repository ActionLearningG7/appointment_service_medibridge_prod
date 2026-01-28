package com.medibridge.appointment_service_medibridge.service;

import com.medibridge.appointment_service_medibridge.api.dto.response.AdminQueueMonitorResponse;
import com.medibridge.appointment_service_medibridge.api.dto.response.DoctorInfoDTO;
import com.medibridge.appointment_service_medibridge.domain.entity.Appointment;
import com.medibridge.appointment_service_medibridge.domain.entity.QueueEntry;
import com.medibridge.appointment_service_medibridge.domain.entity.VirtualQueue;
import com.medibridge.appointment_service_medibridge.domain.enums.QueueEntryStatus;
import com.medibridge.appointment_service_medibridge.domain.repository.AppointmentRepository;
import com.medibridge.appointment_service_medibridge.domain.repository.QueueEntryRepository;
import com.medibridge.appointment_service_medibridge.domain.repository.VirtualQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin Queue Monitoring Service
 * Provides real-time queue monitoring data with statistics for admin dashboard
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminQueueMonitoringService {

    private final VirtualQueueRepository queueRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorServiceClient doctorServiceClient;

    /**
     * Get all active queues for today with statistics
     * Used for admin dashboard monitoring
     */
    @Transactional(readOnly = true)
    public List<AdminQueueMonitorResponse> getAllActiveQueuesWithStats() {
        log.debug("Fetching all active queues for today with statistics");

        List<VirtualQueue> activeQueues = queueRepository.findActiveQueuesForToday();


        return activeQueues.stream()
                .map(this::buildQueueMonitorResponse)
                .sorted(Comparator.comparing(AdminQueueMonitorResponse::getOpenedAt).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get a specific queue with detailed statistics
     */
    @Transactional(readOnly = true)
    public AdminQueueMonitorResponse getQueueWithStats(UUID queueId) {
        log.debug("Fetching queue {} with statistics", queueId);

        VirtualQueue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new RuntimeException("Queue not found: " + queueId));

        return buildQueueMonitorResponse(queue);
    }

    /**
     * Get queue entries for a specific queue
     */
    @Transactional(readOnly = true)
    public List<QueueEntry> getQueueEntries(UUID queueId) {
        log.debug("Fetching entries for queue {}", queueId);
        return queueEntryRepository.findByQueueIdOrderByTokenNumberAsc(queueId);
    }

    /**
     * Get waiting queue entries (patients waiting in queue)
     */
    @Transactional(readOnly = true)
    public List<QueueEntry> getWaitingEntries(UUID queueId) {
        log.debug("Fetching waiting entries for queue {}", queueId);
        return queueEntryRepository.findWaitingEntries(queueId);
    }

    /**
     * Build AdminQueueMonitorResponse with statistics
     */
    private AdminQueueMonitorResponse buildQueueMonitorResponse(VirtualQueue queue) {
        List<QueueEntry> entries = queueEntryRepository.findByQueueIdOrderByTokenNumberAsc(queue.getId());

        // Calculate statistics
        long totalPatients = entries.size();
        long waitingCount = entries.stream()
                .filter(e -> e.getStatus() == QueueEntryStatus.WAITING)
                .count();
        long completedCount = entries.stream()
                .filter(e -> e.getStatus() == QueueEntryStatus.COMPLETED)
                .count();
        long calledCount = entries.stream()
                .filter(e -> e.getStatus() == QueueEntryStatus.CALLED)
                .count();
        long cancelledCount = entries.stream()
                .filter(e -> e.getStatus() == QueueEntryStatus.CANCELLED)
                .count();
        long noShowCount = entries.stream()
                .filter(e -> e.getStatus() == QueueEntryStatus.NO_SHOW)
                .count();

        // Fetch doctor details from User Service
        String doctorName = "Dr. Unknown";
        String specialization = "Specialist";

        try {
            DoctorInfoDTO doctorInfo = doctorServiceClient.getDoctorById(queue.getDoctorId());
            if (doctorInfo != null) {
                // Format doctor name properly
                String firstName = doctorInfo.getFirstName() != null ? doctorInfo.getFirstName().trim() : "";
                String lastName = doctorInfo.getLastName() != null ? doctorInfo.getLastName().trim() : "";

                if (!firstName.isEmpty() && !lastName.isEmpty()) {
                    doctorName = "Dr. " + firstName + " " + lastName;
                } else if (!firstName.isEmpty()) {
                    doctorName = "Dr. " + firstName;
                } else if (!lastName.isEmpty()) {
                    doctorName = "Dr. " + lastName;
                }

                // Set specialization if available
                if (doctorInfo.getSpecialization() != null && !doctorInfo.getSpecialization().trim().isEmpty()) {
                    specialization = doctorInfo.getSpecialization().trim();
                }

                log.debug("Fetched doctor info: {} - {}", doctorName, specialization);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch doctor details for {}: {}", queue.getDoctorId(), e.getMessage());
            // Fallback to appointment-based lookup if service call fails
            try {
                Optional<Appointment> appointment = appointmentRepository.findByDoctorIdAndAppointmentDate(
                        queue.getDoctorId(),
                        queue.getQueueDate()
                );
                if (appointment.isPresent()) {
                    Appointment apt = appointment.get();
                    doctorName = apt.getDoctorName();
                    specialization = apt.getDoctorSpecialization();
                    log.debug("Fell back to appointment data for doctor");
                }
            } catch (Exception ex) {
                log.warn("Fallback appointment lookup also failed: {}", ex.getMessage());
            }
        }

        return AdminQueueMonitorResponse.builder()
                .id(queue.getId())
                .doctorId(queue.getDoctorId())
                .doctorName(doctorName)
                .specialization(specialization)
                .queueDate(queue.getQueueDate())
                .status(queue.getStatus())
                .currentToken(queue.getCurrentToken())
                .avgConsultationMinutes(queue.getAvgConsultationMinutes())
                .openedAt(queue.getOpenedAt())
                .closedAt(queue.getClosedAt())
                .totalPatients((int) totalPatients)
                .waitingCount((int) waitingCount)
                .completedCount((int) completedCount)
                .calledCount((int) calledCount)
                .cancelledCount((int) cancelledCount)
                .noShowCount((int) noShowCount)
                .build();
    }

    /**
     * Get queue statistics summary
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getQueueStatisticsSummary() {
        log.debug("Fetching queue statistics summary");

        List<AdminQueueMonitorResponse> allQueues = getAllActiveQueuesWithStats();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalQueues", allQueues.size());
        stats.put("activeQueues", allQueues.stream()
                .filter(q -> q.getStatus().toString().equals("OPEN"))
                .count());
        stats.put("pausedQueues", allQueues.stream()
                .filter(q -> q.getStatus().toString().equals("PAUSED"))
                .count());
        stats.put("totalPatients", allQueues.stream()
                .mapToInt(AdminQueueMonitorResponse::getTotalPatients)
                .sum());
        stats.put("totalWaiting", allQueues.stream()
                .mapToInt(AdminQueueMonitorResponse::getWaitingCount)
                .sum());
        stats.put("totalCompleted", allQueues.stream()
                .mapToInt(AdminQueueMonitorResponse::getCompletedCount)
                .sum());

        return stats;
    }
}
