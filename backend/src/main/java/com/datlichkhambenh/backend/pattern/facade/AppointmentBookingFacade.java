package com.datlichkhambenh.backend.pattern.facade;

import com.datlichkhambenh.backend.dto.request.CreateBookingDto;
import com.datlichkhambenh.backend.dto.response.AppointmentDto;

public interface AppointmentBookingFacade {
    AppointmentDto bookAppointment(Long patientUserId, CreateBookingDto request);
    void cancelAppointment(Long patientUserId, Long appointmentId);
}
