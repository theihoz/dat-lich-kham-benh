package com.datlichkhambenh.backend.service;

import com.datlichkhambenh.backend.dto.request.CreateBookingDto;
import com.datlichkhambenh.backend.entity.Appointment;
import com.datlichkhambenh.backend.entity.Doctor;
import com.datlichkhambenh.backend.entity.Patient;
import com.datlichkhambenh.backend.entity.Schedule;
import com.datlichkhambenh.backend.entity.User;
import com.datlichkhambenh.backend.exception.AppException;
import com.datlichkhambenh.backend.exception.ResourceNotFoundException;
import com.datlichkhambenh.backend.repository.AppointmentRepository;
import com.datlichkhambenh.backend.repository.DoctorRepository;
import com.datlichkhambenh.backend.repository.PatientRepository;
import com.datlichkhambenh.backend.repository.ScheduleRepository;
import com.datlichkhambenh.backend.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTests {
    @Mock ScheduleRepository scheduleRepository;
    @Mock AppointmentRepository appointmentRepository;
    @Mock PatientRepository patientRepository;
    @Mock DoctorRepository doctorRepository;
    @Mock AsyncEmailNotificationService emailNotificationService;
    @Mock RedisCacheService redisCacheService;
    @InjectMocks BookingServiceImpl bookingService;

    @Test
    void emptyHistoryStaysEmptyInsteadOfInventingAppointments() {
        when(appointmentRepository.findByPatientUserId(10L)).thenReturn(List.of());
        assertTrue(bookingService.getPatientAppointments(10L).isEmpty());
    }

    @Test
    void unrelatedPatientCannotReadAppointment() {
        Appointment appointment = appointment(10L, 20L);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        assertThrows(AppException.class, () -> bookingService.getBookingDetails(1L, 99L, "PATIENT"));
    }

    @Test
    void cancellationKeepsHistoryAndCreatesReplacementSlot() {
        Appointment appointment = appointment(10L, 20L);
        when(appointmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(appointment));

        bookingService.cancelBooking(1L, 10L);

        assertEquals("CANCELLED", appointment.getStatus());
        ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository, times(2)).save(captor.capture());
        List<Schedule> saved = captor.getAllValues();
        assertTrue(saved.stream().anyMatch(slot -> slot.getId() == null && Boolean.TRUE.equals(slot.getIsAvailable())));
        verify(emailNotificationService).sendCancellationNotice(appointment);
    }

    @Test
    void bookingWithNonexistentPatientThrowsResourceNotFound() {
        User doctorUser = User.builder().id(20L).isActive(true).build();
        Doctor doctor = Doctor.builder().userId(20L).user(doctorUser).fullName("Doctor")
                .isApproved(true).build();
        Schedule schedule = Schedule.builder().id(30L).doctor(doctor)
                .workDate(LocalDate.now().plusDays(3)).startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(9, 30)).isAvailable(true).build();
        when(scheduleRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(schedule));
        when(appointmentRepository.existsByScheduleId(30L)).thenReturn(false);
        when(patientRepository.findById(999L)).thenReturn(Optional.empty());

        CreateBookingDto request = CreateBookingDto.builder().scheduleId(30L).symptoms("Test").build();
        assertThrows(ResourceNotFoundException.class, () -> bookingService.createBooking(999L, request));
    }

    private Appointment appointment(Long patientId, Long doctorId) {
        Patient patient = Patient.builder().userId(patientId).fullName("Patient").build();
        Doctor doctor = Doctor.builder().userId(doctorId).fullName("Doctor").build();
        Schedule schedule = Schedule.builder().id(30L).doctor(doctor)
                .workDate(LocalDate.now().plusDays(3)).startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(9, 30)).isAvailable(false).build();
        return Appointment.builder().id(1L).patient(patient).doctor(doctor).schedule(schedule)
                .bookingCode("BK-TEST").status("CONFIRMED").build();
    }
}
