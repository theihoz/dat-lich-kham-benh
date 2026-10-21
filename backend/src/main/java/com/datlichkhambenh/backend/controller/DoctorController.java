package com.datlichkhambenh.backend.controller;

import com.datlichkhambenh.backend.dto.request.CreateScheduleDto;
import com.datlichkhambenh.backend.dto.request.UpdateMedicalRecordDto;
import com.datlichkhambenh.backend.dto.request.UpdateDoctorProfileDto;
import com.datlichkhambenh.backend.dto.response.ApiResponseDto;
import com.datlichkhambenh.backend.dto.response.AppointmentDto;
import com.datlichkhambenh.backend.dto.response.DoctorDto;
import com.datlichkhambenh.backend.dto.response.ScheduleDto;
import com.datlichkhambenh.backend.dto.response.SpecialtyDto;
import com.datlichkhambenh.backend.service.BookingService;
import com.datlichkhambenh.backend.service.CurrentUserService;
import com.datlichkhambenh.backend.service.DoctorService;
import com.datlichkhambenh.backend.service.DoctorScheduleService;
import com.datlichkhambenh.backend.service.MedicalRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorScheduleService doctorScheduleService;
    private final DoctorService doctorService;
    private final CurrentUserService currentUserService;
    private final BookingService bookingService;
    private final MedicalRecordService medicalRecordService;

    @GetMapping
    public ResponseEntity<ApiResponseDto<List<DoctorDto>>> getApprovedDoctors(
            @RequestParam(value = "specialtyId", required = false) Long specialtyId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minFee,
            @RequestParam(required = false) BigDecimal maxFee,
            @RequestParam(required = false) Integer minExperience,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<DoctorDto> result = doctorService.searchApprovedDoctors(
                specialtyId, keyword, minFee, maxFee, minExperience, sort, page, size);
        return ResponseEntity.ok(ApiResponseDto.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<DoctorDto>> getDoctorById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponseDto.success(doctorService.getApprovedDoctor(id)));
    }

    @GetMapping("/specialties")
    public ResponseEntity<ApiResponseDto<List<SpecialtyDto>>> getSpecialties() {
        return ResponseEntity.ok(ApiResponseDto.success(doctorService.getSpecialties()));
    }

    @GetMapping("/{id}/slots")
    public ResponseEntity<ApiResponseDto<List<ScheduleDto>>> getAvailableSlots(
            @PathVariable("id") Long doctorId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<ScheduleDto> slots = doctorScheduleService.getAvailableSlots(doctorId, date);
        return ResponseEntity.ok(ApiResponseDto.success(slots));
    }

    @PostMapping("/slots")
    @PreAuthorize("hasAuthority('DOCTOR')")
    public ResponseEntity<ApiResponseDto<ScheduleDto>> createSlot(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateScheduleDto request
    ) {
        Long doctorUserId = currentUserService.getUserId(userDetails);
        ScheduleDto slot = doctorScheduleService.createSlot(doctorUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(slot, "Tạo ca khám thành công"));
    }

    @PutMapping("/slots/{id}/toggle")
    @PreAuthorize("hasAuthority('DOCTOR')")
    public ResponseEntity<ApiResponseDto<Void>> toggleSlot(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long scheduleId,
            @RequestParam("available") boolean available
    ) {
        doctorScheduleService.toggleSlotAvailability(currentUserService.getUserId(userDetails), scheduleId, available);
        return ResponseEntity.ok(ApiResponseDto.success(null, "Cập nhật trạng thái ca khám thành công"));
    }

    @PutMapping("/slots/{id}")
    @PreAuthorize("hasAuthority('DOCTOR')")
    public ResponseEntity<ApiResponseDto<ScheduleDto>> updateSlot(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long scheduleId,
            @Valid @RequestBody CreateScheduleDto request
    ) {
        Long doctorUserId = currentUserService.getUserId(userDetails);
        ScheduleDto slot = doctorScheduleService.updateSlot(doctorUserId, scheduleId, request);
        return ResponseEntity.ok(ApiResponseDto.success(slot, "Cập nhật ca khám thành công"));
    }

    @DeleteMapping("/slots/{id}")
    @PreAuthorize("hasAuthority('DOCTOR')")
    public ResponseEntity<ApiResponseDto<Void>> deleteSlot(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long scheduleId
    ) {
        Long doctorUserId = currentUserService.getUserId(userDetails);
        doctorScheduleService.deleteSlot(doctorUserId, scheduleId);
        return ResponseEntity.ok(ApiResponseDto.success(null, "Xóa ca khám thành công"));
    }

    @GetMapping("/my-appointments")
    @PreAuthorize("hasAuthority('DOCTOR')")
    public ResponseEntity<ApiResponseDto<List<AppointmentDto>>> getMyDoctorAppointments(@AuthenticationPrincipal UserDetails userDetails) {
        Long doctorUserId = currentUserService.getUserId(userDetails);
        List<AppointmentDto> appointments = bookingService.getDoctorAppointments(doctorUserId);
        return ResponseEntity.ok(ApiResponseDto.success(appointments));
    }

    @GetMapping("/my-appointments/range")
    @PreAuthorize("hasAuthority('DOCTOR')")
    public ResponseEntity<ApiResponseDto<List<AppointmentDto>>> getAppointmentsByRange(
            @AuthenticationPrincipal UserDetails userDetails, @RequestParam LocalDate from, @RequestParam LocalDate to) {
        Long id = currentUserService.getUserId(userDetails);
        List<AppointmentDto> result = bookingService.getDoctorAppointments(id, from, to);
        return ResponseEntity.ok(ApiResponseDto.success(result));
    }

    @PutMapping("/appointments/{id}/status")
    @PreAuthorize("hasAuthority('DOCTOR')")
    public ResponseEntity<ApiResponseDto<Void>> updateAppointmentStatus(@AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id, @RequestParam String status) {
        bookingService.updateAppointmentStatus(id, currentUserService.getUserId(userDetails), status.toUpperCase());
        return ResponseEntity.ok(ApiResponseDto.success(null, "Đã cập nhật trạng thái lịch hẹn"));
    }

    @PostMapping("/medical-records")
    @PreAuthorize("hasAuthority('DOCTOR')")
    public ResponseEntity<ApiResponseDto<Void>> updateMedicalRecord(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateMedicalRecordDto request) {
        medicalRecordService.updateMedicalRecord(currentUserService.getUserId(userDetails), request);
        return ResponseEntity.ok(ApiResponseDto.success(null, "Cập nhật kết quả chẩn đoán bệnh thành công"));
    }

    @GetMapping("/appointments/{id}/medical-record")
    @PreAuthorize("hasAuthority('DOCTOR')")
    public ResponseEntity<ApiResponseDto<com.datlichkhambenh.backend.dto.response.MedicalRecordDto>> getMedicalRecord(
            @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.success(
                medicalRecordService.getMedicalRecord(currentUserService.getUserId(userDetails), id)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('DOCTOR')")
    public ResponseEntity<ApiResponseDto<DoctorDto>> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponseDto.success(doctorService.getProfile(currentUserService.getUserId(userDetails))));
    }

    @PutMapping("/me")
    @PreAuthorize("hasAuthority('DOCTOR')")
    public ResponseEntity<ApiResponseDto<DoctorDto>> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateDoctorProfileDto request) {
        DoctorDto updated = doctorService.updateProfile(currentUserService.getUserId(userDetails), request);
        return ResponseEntity.ok(ApiResponseDto.success(updated, "Cập nhật hồ sơ bác sĩ thành công"));
    }

    @GetMapping("/my-slots")
    @PreAuthorize("hasAuthority('DOCTOR')")
    public ResponseEntity<ApiResponseDto<List<ScheduleDto>>> getMySlots(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponseDto.success(
                doctorScheduleService.getDoctorSlots(currentUserService.getUserId(userDetails))));
    }

    @GetMapping("/my-patients/completed")
    @PreAuthorize("hasAuthority('DOCTOR')")
    public ResponseEntity<ApiResponseDto<List<AppointmentDto>>> getCompletedPatients(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponseDto.success(
                bookingService.getCompletedDoctorAppointments(currentUserService.getUserId(userDetails))));
    }
}
