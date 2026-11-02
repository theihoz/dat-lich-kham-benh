package com.datlichkhambenh.backend.pattern;

import com.datlichkhambenh.backend.dto.request.CreateBookingDto;
import com.datlichkhambenh.backend.dto.response.AppointmentDto;
import com.datlichkhambenh.backend.pattern.facade.AppointmentBookingFacade;
import com.datlichkhambenh.backend.pattern.facade.AppointmentBookingFacadeImpl;
import com.datlichkhambenh.backend.service.BookingService;
import com.datlichkhambenh.backend.service.DoctorScheduleService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FacadePatternTests {

    private final BookingService bookingService = mock(BookingService.class);
    private final DoctorScheduleService scheduleService = mock(DoctorScheduleService.class);
    private final AppointmentBookingFacade facade =
            new AppointmentBookingFacadeImpl(bookingService, scheduleService);

    @Test
    void facadeDelegatesToBookingServiceForCreate() {
        CreateBookingDto request = CreateBookingDto.builder()
                .scheduleId(10L).symptoms("Đau đầu").build();
        AppointmentDto expected = AppointmentDto.builder()
                .id(1L).bookingCode("BK-FACADE").status("CONFIRMED")
                .price(new BigDecimal("300000")).build();
        when(bookingService.createBooking(5L, request)).thenReturn(expected);

        AppointmentDto result = facade.bookAppointment(5L, request);

        assertSame(expected, result);
        verify(bookingService).createBooking(5L, request);
        verifyNoMoreInteractions(bookingService);
    }

    @Test
    void facadeDelegatesToBookingServiceForCancel() {
        facade.cancelAppointment(5L, 99L);

        verify(bookingService).cancelBooking(99L, 5L);
        verifyNoMoreInteractions(bookingService);
    }

    @Test
    void facadeExposesSimplifiedInterfaceHidingSubsystemComplexity() {
        assertEquals(2, AppointmentBookingFacade.class.getDeclaredMethods().length,
                "Facade chỉ lộ 2 phương thức: bookAppointment và cancelAppointment");
    }
}
