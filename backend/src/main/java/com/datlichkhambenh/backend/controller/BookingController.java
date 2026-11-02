package com.datlichkhambenh.backend.controller;

import com.datlichkhambenh.backend.dto.request.CreateBookingDto;
import com.datlichkhambenh.backend.dto.response.ApiResponseDto;
import com.datlichkhambenh.backend.dto.response.AppointmentDto;
import com.datlichkhambenh.backend.pattern.facade.AppointmentBookingFacade;
import com.datlichkhambenh.backend.service.BookingService;
import com.datlichkhambenh.backend.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class BookingController {

    private final AppointmentBookingFacade bookingFacade;
    private final BookingService bookingService;
    private final CurrentUserService currentUserService;

    @PostMapping
    @PreAuthorize("hasAuthority('PATIENT')")
    public ResponseEntity<ApiResponseDto<AppointmentDto>> createBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateBookingDto request
    ) {
        Long patientUserId = currentUserService.getUserId(userDetails);
        AppointmentDto appointment = bookingFacade.bookAppointment(patientUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(appointment, "Đặt lịch khám thành công"));
    }

    @DeleteMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PATIENT')")
    public ResponseEntity<ApiResponseDto<Void>> cancelBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long appointmentId
    ) {
        Long patientUserId = currentUserService.getUserId(userDetails);
        bookingFacade.cancelAppointment(patientUserId, appointmentId);
        return ResponseEntity.ok(ApiResponseDto.success(null, "Hủy lịch hẹn thành công"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PATIENT', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponseDto<AppointmentDto>> getBookingDetails(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long appointmentId) {
        Long requesterId = currentUserService.getUserId(userDetails);
        String requesterRole = userDetails.getAuthorities().iterator().next().getAuthority();
        AppointmentDto appointment = bookingService.getBookingDetails(appointmentId, requesterId, requesterRole);
        return ResponseEntity.ok(ApiResponseDto.success(appointment));
    }

    @GetMapping("/my-appointments")
    @PreAuthorize("hasAuthority('PATIENT')")
    public ResponseEntity<ApiResponseDto<List<AppointmentDto>>> getMyPatientAppointments(@AuthenticationPrincipal UserDetails userDetails) {
        Long patientUserId = currentUserService.getUserId(userDetails);
        List<AppointmentDto> appointments = bookingService.getPatientAppointments(patientUserId);
        return ResponseEntity.ok(ApiResponseDto.success(appointments));
    }
}
