package com.datlichkhambenh.backend.service;

import com.datlichkhambenh.backend.dto.request.UpdateMedicalRecordDto;
import com.datlichkhambenh.backend.entity.Appointment;
import com.datlichkhambenh.backend.entity.Doctor;
import com.datlichkhambenh.backend.entity.Schedule;
import com.datlichkhambenh.backend.exception.AppException;
import com.datlichkhambenh.backend.repository.AppointmentRepository;
import com.datlichkhambenh.backend.repository.DoctorRepository;
import com.datlichkhambenh.backend.repository.MedicalRecordRepository;
import com.datlichkhambenh.backend.repository.ScheduleRepository;
import com.datlichkhambenh.backend.service.impl.DoctorScheduleServiceImpl;
import com.datlichkhambenh.backend.service.impl.MedicalRecordServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.springframework.http.HttpStatus;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnershipServiceTests {
    @Mock ScheduleRepository scheduleRepository;
    @Mock DoctorRepository doctorRepository;
    @Mock RedisCacheService redisCacheService;
    @Mock AppointmentRepository appointmentRepository;
    @Mock MedicalRecordRepository medicalRecordRepository;
    @Mock com.datlichkhambenh.backend.audit.MedicalAuditService audit;
    @InjectMocks DoctorScheduleServiceImpl doctorScheduleService;
    @InjectMocks MedicalRecordServiceImpl medicalRecordService;

    @Test
    void doctorCannotToggleAnotherDoctorsSlot() {
        Doctor owner = Doctor.builder().userId(2L).build();
        Schedule schedule = Schedule.builder().id(5L).doctor(owner).build();
        when(scheduleRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(schedule));
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(AppException.class,
                () -> doctorScheduleService.toggleSlotAvailability(1L, 5L, false)).getStatus());
    }

    @Test
    void doctorCannotReadAnotherDoctorsMedicalRecord() {
        Doctor owner = Doctor.builder().userId(2L).build();
        Appointment appointment = Appointment.builder().id(8L).doctor(owner).status("COMPLETED").build();
        when(appointmentRepository.findById(8L)).thenReturn(Optional.of(appointment));
        assertThrows(AppException.class, () -> medicalRecordService.getMedicalRecord(1L, 8L));
    }

    @Test
    void doctorCannotWriteAnotherDoctorsMedicalRecord() {
        Doctor owner = Doctor.builder().userId(2L).build();
        Appointment appointment = Appointment.builder().id(8L).doctor(owner).status("CONFIRMED").build();
        when(appointmentRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(appointment));
        UpdateMedicalRecordDto request = UpdateMedicalRecordDto.builder().appointmentId(8L)
                .diagnosis("Diagnosis").build();
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(AppException.class,
                () -> medicalRecordService.updateMedicalRecord(1L, request)).getStatus());
    }
}
