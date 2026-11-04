package com.datlichkhambenh.backend.controller;

import com.datlichkhambenh.backend.dto.request.SpecialtyRequestDto;
import com.datlichkhambenh.backend.dto.response.ApiResponseDto;
import com.datlichkhambenh.backend.dto.response.DoctorDto;
import com.datlichkhambenh.backend.dto.response.SpecialtyDto;
import com.datlichkhambenh.backend.dto.response.UserDto;
import com.datlichkhambenh.backend.dto.response.PatientDto;
import com.datlichkhambenh.backend.dto.request.AdminCreateDoctorDto;
import com.datlichkhambenh.backend.dto.request.AdminCreatePatientDto;
import com.datlichkhambenh.backend.dto.request.UpdateDoctorProfileDto;
import com.datlichkhambenh.backend.dto.request.UpdatePatientProfileDto;
import com.datlichkhambenh.backend.service.AdminManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.time.Year;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    private final AdminManagementService adminManagementService;
    private final com.datlichkhambenh.backend.service.query.AdminReadService queries;

    @GetMapping("/doctors/pending")
    public ResponseEntity<ApiResponseDto<List<DoctorDto>>> getPendingDoctors() {
        List<DoctorDto> doctors = adminManagementService.getPendingDoctors();
        return ResponseEntity.ok(ApiResponseDto.success(doctors));
    }

    @PutMapping("/doctors/{id}/approve")
    public ResponseEntity<ApiResponseDto<Void>> approveDoctor(@PathVariable("id") Long doctorUserId) {
        adminManagementService.approveDoctor(doctorUserId);
        return ResponseEntity.ok(ApiResponseDto.success(null, "Phê duyệt bác sĩ thành công"));
    }

    @PutMapping("/doctors/{id}/reject")
    public ResponseEntity<ApiResponseDto<Void>> rejectDoctor(@PathVariable("id") Long doctorUserId) {
        adminManagementService.rejectDoctor(doctorUserId);
        return ResponseEntity.ok(ApiResponseDto.success(null, "Từ chối bác sĩ thành công"));
    }

    @PostMapping("/specialties")
    public ResponseEntity<ApiResponseDto<SpecialtyDto>> createSpecialty(@Valid @RequestBody SpecialtyRequestDto request) {
        SpecialtyDto specialty = adminManagementService.createSpecialty(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(specialty, "Tạo chuyên khoa mới thành công"));
    }

    @GetMapping("/specialties")
    public ResponseEntity<ApiResponseDto<List<SpecialtyDto>>> getAllSpecialties() {
        List<SpecialtyDto> specialties = adminManagementService.getAllSpecialties();
        return ResponseEntity.ok(ApiResponseDto.success(specialties));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> getSystemStats() {
        Map<String, Object> stats = queries.stats();
        return ResponseEntity.ok(ApiResponseDto.success(stats));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponseDto<List<UserDto>>> getUsers(@RequestParam(required = false) String role,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        var result=queries.users(role,page,size);
        return ResponseEntity.ok().header("X-Total-Count",Long.toString(result.total())).body(ApiResponseDto.success(result.content()));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponseDto<Void>> setUserStatus(@PathVariable Long id, @RequestParam boolean active) {
        adminManagementService.setUserActive(id, active);
        return ResponseEntity.ok(ApiResponseDto.success(null, active ? "Đã kích hoạt tài khoản" : "Đã khóa tài khoản"));
    }

    @PutMapping("/specialties/{id}")
    public ResponseEntity<ApiResponseDto<SpecialtyDto>> updateSpecialty(@PathVariable Long id, @Valid @RequestBody SpecialtyRequestDto request) {
        return ResponseEntity.ok(ApiResponseDto.success(adminManagementService.updateSpecialty(id, request)));
    }

    @DeleteMapping("/specialties/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deleteSpecialty(@PathVariable Long id) {
        adminManagementService.deleteSpecialty(id);
        return ResponseEntity.ok(ApiResponseDto.success(null, "Đã xóa chuyên khoa"));
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> analytics(
            @RequestParam(required = false) Integer year) {
        int selectedYear = year != null ? year : Year.now().getValue();
        return ResponseEntity.ok(ApiResponseDto.success(queries.analytics(selectedYear)));
    }

    @GetMapping("/doctors")
    public ResponseEntity<ApiResponseDto<List<DoctorDto>>> getDoctors(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        var result=queries.doctors(page,size);
        return ResponseEntity.ok().header("X-Total-Count",Long.toString(result.total())).body(ApiResponseDto.success(result.content()));
    }

    @PostMapping("/doctors")
    public ResponseEntity<ApiResponseDto<DoctorDto>> createDoctor(@Valid @RequestBody AdminCreateDoctorDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(adminManagementService.createDoctor(request), "Đã tạo bác sĩ"));
    }

    @PutMapping("/doctors/{id}")
    public ResponseEntity<ApiResponseDto<DoctorDto>> updateDoctor(
            @PathVariable Long id, @Valid @RequestBody UpdateDoctorProfileDto request) {
        return ResponseEntity.ok(ApiResponseDto.success(adminManagementService.updateDoctor(id, request), "Đã cập nhật bác sĩ"));
    }

    @DeleteMapping("/doctors/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deleteDoctor(@PathVariable Long id) {
        adminManagementService.deleteDoctor(id);
        return ResponseEntity.ok(ApiResponseDto.success(null, "Đã xóa bác sĩ"));
    }

    @GetMapping("/patients")
    public ResponseEntity<ApiResponseDto<List<PatientDto>>> getPatients(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        var result=queries.patients(page,size);
        return ResponseEntity.ok().header("X-Total-Count",Long.toString(result.total())).body(ApiResponseDto.success(result.content()));
    }

    @PostMapping("/patients")
    public ResponseEntity<ApiResponseDto<PatientDto>> createPatient(@Valid @RequestBody AdminCreatePatientDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(adminManagementService.createPatient(request), "Đã tạo bệnh nhân"));
    }

    @PutMapping("/patients/{id}")
    public ResponseEntity<ApiResponseDto<PatientDto>> updatePatient(
            @PathVariable Long id, @Valid @RequestBody UpdatePatientProfileDto request) {
        return ResponseEntity.ok(ApiResponseDto.success(adminManagementService.updatePatient(id, request), "Đã cập nhật bệnh nhân"));
    }

    @DeleteMapping("/patients/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deletePatient(@PathVariable Long id) {
        adminManagementService.deletePatient(id);
        return ResponseEntity.ok(ApiResponseDto.success(null, "Đã xóa bệnh nhân"));
    }
    @GetMapping("/appointments")
    public ResponseEntity<ApiResponseDto<List<com.datlichkhambenh.backend.dto.response.AppointmentDto>>> appointments(
            @RequestParam(required=false) Long doctorId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        var result=queries.appointments(doctorId,page,size);
        return ResponseEntity.ok().header("X-Total-Count",Long.toString(result.total())).body(ApiResponseDto.success(result.content()));
    }

}
