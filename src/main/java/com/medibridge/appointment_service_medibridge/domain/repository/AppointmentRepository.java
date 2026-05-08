package com.medibridge.appointment_service_medibridge.domain.repository;

import com.medibridge.appointment_service_medibridge.domain.entity.Appointment;
import com.medibridge.appointment_service_medibridge.domain.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

        // Patient Queries
        Page<Appointment> findByPatientId(UUID patientId, Pageable pageable);

        Page<Appointment> findByPatientIdAndStatus(UUID patientId, AppointmentStatus status, Pageable pageable);

        List<Appointment> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

        // Doctor Queries
        Page<Appointment> findByDoctorId(UUID doctorId, Pageable pageable);

        List<Appointment> findByDoctorIdOrderByAppointmentDateDesc(UUID doctorId);

        Page<Appointment> findByDoctorIdAndAppointmentDate(UUID doctorId, LocalDate date, Pageable pageable);

        @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND a.appointmentDate = :date AND a.status IN :statuses")
        List<Appointment> findDoctorAppointmentsForDay(
                        @Param("doctorId") UUID doctorId,
                        @Param("date") LocalDate date,
                        @Param("statuses") List<AppointmentStatus> statuses);

        // Status Checks
        boolean existsByPatientIdAndDoctorIdAndAppointmentDateAndStatusIn(
                        UUID patientId,
                        UUID doctorId,
                        LocalDate date,
                        List<AppointmentStatus> statuses);

        // Organization Queries (Admin)
        Page<Appointment> findByOrganizationId(UUID organizationId, Pageable pageable);

        @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctorId = :doctorId AND a.appointmentDate = :date AND a.status = 'COMPLETED'")
        long countCompletedAppointments(@Param("doctorId") UUID doctorId, @Param("date") LocalDate date);

        // Queue Monitoring (Admin)
        Optional<Appointment> findByDoctorIdAndAppointmentDate(UUID doctorId, LocalDate date);
}
