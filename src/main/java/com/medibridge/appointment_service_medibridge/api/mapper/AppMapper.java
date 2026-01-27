package com.medibridge.appointment_service_medibridge.api.mapper;

import com.medibridge.appointment_service_medibridge.api.dto.response.AppointmentResponse;
import com.medibridge.appointment_service_medibridge.api.dto.response.QueueEntryResponse;
import com.medibridge.appointment_service_medibridge.api.dto.response.QueueResponse;
import com.medibridge.appointment_service_medibridge.domain.entity.Appointment;
import com.medibridge.appointment_service_medibridge.domain.entity.QueueEntry;
import com.medibridge.appointment_service_medibridge.domain.entity.VirtualQueue;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AppMapper {
    AppointmentResponse toAppointmentResponse(Appointment appointment);

    List<AppointmentResponse> toAppointmentResponses(List<Appointment> appointments);

    QueueEntryResponse toQueueEntryResponse(QueueEntry queueEntry);

    List<QueueEntryResponse> toQueueEntryResponses(List<QueueEntry> queueEntries);

    QueueResponse toQueueResponse(VirtualQueue queue);
}
