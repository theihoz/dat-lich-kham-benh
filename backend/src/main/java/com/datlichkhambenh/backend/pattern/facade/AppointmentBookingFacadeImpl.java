package com.datlichkhambenh.backend.pattern.facade;

import com.datlichkhambenh.backend.dto.request.CreateBookingDto;
import com.datlichkhambenh.backend.dto.response.AppointmentDto;
import com.datlichkhambenh.backend.service.BookingService;
import com.datlichkhambenh.backend.service.DoctorScheduleService;
import com.datlichkhambenh.backend.dto.response.ScheduleDto;
import com.datlichkhambenh.backend.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentBookingFacadeImpl implements AppointmentBookingFacade {

    private final BookingService bookingService;
    private final DoctorScheduleService doctorScheduleService;

    @Override
    public AppointmentDto bookAppointment(Long patientUserId, CreateBookingDto request) {
        log.info("Facade: bắt đầu quy trình đặt lịch cho bệnh nhân {}", patientUserId);
        return bookingService.createBooking(patientUserId, request);
    }

    @Override
    public void cancelAppointment(Long patientUserId, Long appointmentId) {
        log.info("Facade: bắt đầu quy trình hủy lịch hẹn {} cho bệnh nhân {}", appointmentId, patientUserId);
        bookingService.cancelBooking(appointmentId, patientUserId);
    }
}
