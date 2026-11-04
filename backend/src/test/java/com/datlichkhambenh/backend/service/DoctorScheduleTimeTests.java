package com.datlichkhambenh.backend.service;

import com.datlichkhambenh.backend.dto.response.ScheduleDto;
import com.datlichkhambenh.backend.entity.*;
import com.datlichkhambenh.backend.repository.*;
import com.datlichkhambenh.backend.service.impl.DoctorScheduleServiceImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorScheduleTimeTests {
    @Mock ScheduleRepository schedules;
    @Mock DoctorRepository doctors;
    @Mock RedisCacheService cache;
    @Mock AppointmentRepository appointments;
    @InjectMocks DoctorScheduleServiceImpl service;

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void pastStartTimesAreExcludedFromAvailableSlotsIncludingCachedResults(boolean cached) {
        Doctor doctor = Doctor.builder().userId(2L).fullName("Doctor").isApproved(true)
                .user(User.builder().id(2L).isActive(true).build()).build();
        LocalDate today = LocalDate.now();
        // Midnight today is already in the past whenever this test runs.
        Schedule past = Schedule.builder().id(3L).doctor(doctor).workDate(today)
                .startTime(LocalTime.MIDNIGHT).endTime(LocalTime.of(0, 30)).isAvailable(true).build();
        when(doctors.findById(2L)).thenReturn(Optional.of(doctor));
        if (cached) {
            when(cache.getCachedAvailableSlots(2L, today)).thenReturn(List.of(ScheduleDto.builder()
                    .id(3L).doctorId(2L).workDate(today).startTime(LocalTime.MIDNIGHT)
                    .endTime(LocalTime.of(0, 30)).isAvailable(true).build()));
        } else {
            when(cache.getCachedAvailableSlots(2L, today)).thenReturn(null);
            when(schedules.findByDoctorUserIdAndWorkDateAndIsAvailableTrue(2L, today)).thenReturn(List.of(past));
        }
        assertTrue(service.getAvailableSlots(2L, today).isEmpty());
    }
}
