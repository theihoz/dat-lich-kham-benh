package com.datlichkhambenh.backend.service.impl;

import com.datlichkhambenh.backend.dto.request.SpecialtyRequestDto;
import com.datlichkhambenh.backend.dto.response.DoctorDto;
import com.datlichkhambenh.backend.dto.response.SpecialtyDto;
import com.datlichkhambenh.backend.dto.response.UserDto;
import com.datlichkhambenh.backend.dto.response.PatientDto;
import com.datlichkhambenh.backend.dto.request.AdminCreateDoctorDto;
import com.datlichkhambenh.backend.dto.request.AdminCreatePatientDto;
import com.datlichkhambenh.backend.dto.request.UpdateDoctorProfileDto;
import com.datlichkhambenh.backend.dto.request.UpdatePatientProfileDto;
import com.datlichkhambenh.backend.entity.Doctor;
import com.datlichkhambenh.backend.entity.Patient;
import com.datlichkhambenh.backend.entity.User;
import com.datlichkhambenh.backend.entity.Specialty;
import com.datlichkhambenh.backend.exception.ResourceNotFoundException;
import com.datlichkhambenh.backend.repository.*;
import com.datlichkhambenh.backend.service.AdminManagementService;
import com.datlichkhambenh.backend.service.RedisCacheService;
import com.datlichkhambenh.backend.service.DoctorService;
import com.datlichkhambenh.backend.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.datlichkhambenh.backend.entity.Appointment;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Locale;
import java.time.LocalDate;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class AdminManagementServiceImpl implements AdminManagementService {

    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final RedisCacheService redisCacheService;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;
    private final PasswordEncoder passwordEncoder;
    private final MedicalServiceRepository medicalServiceRepository;

    @Override
    @Transactional
    public void approveDoctor(Long doctorUserId) {
        Doctor doctor = doctorRepository.findById(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bác sĩ"));
        doctor.setIsApproved(true);
        doctor.getUser().setIsActive(true);
        userRepository.save(doctor.getUser());
        doctorRepository.save(doctor);
        redisCacheService.evictDoctorCache(doctor.getSpecialty() != null ? doctor.getSpecialty().getId() : null);
    }

    @Override
    @Transactional
    public void rejectDoctor(Long doctorUserId) {
        Doctor doctor = doctorRepository.findById(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bác sĩ"));
        doctor.setIsApproved(false);
        doctor.getUser().setIsActive(false);
        userRepository.save(doctor.getUser());
        doctorRepository.save(doctor);
        redisCacheService.evictDoctorCache(doctor.getSpecialty() != null ? doctor.getSpecialty().getId() : null);
    }

    @Override
    @Transactional
    public SpecialtyDto createSpecialty(SpecialtyRequestDto request) {
        Specialty specialty = Specialty.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        Specialty saved = specialtyRepository.save(specialty);
        return SpecialtyDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpecialtyDto> getAllSpecialties() {
        return specialtyRepository.findAll().stream()
                .map(s -> SpecialtyDto.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .description(s.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorDto> getPendingDoctors() {
        return doctorRepository.findAll().stream()
                .filter(d -> !Boolean.TRUE.equals(d.getIsApproved()) && Boolean.TRUE.equals(d.getUser().getIsActive()))
                .map(this::mapDoctor)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDoctors", doctorRepository.count());
        stats.put("totalPatients", patientRepository.count());
        stats.put("totalAppointments", appointmentRepository.count());
        stats.put("totalSpecialties", specialtyRepository.count());
        long cancelled = appointmentRepository.countByStatus("CANCELLED");
        long total = appointmentRepository.count();
        stats.put("cancelledAppointments", cancelled);
        stats.put("cancellationRate", total == 0 ? 0 : Math.round(cancelled * 10000.0 / total) / 100.0);
        java.math.BigDecimal revenue = appointmentRepository.findAll().stream()
                .filter(a -> "COMPLETED".equals(a.getStatus()) && a.getPrice() != null)
                .map(com.datlichkhambenh.backend.entity.Appointment::getPrice)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        stats.put("totalRevenue", revenue);
        stats.put("topDoctors", appointmentRepository.findTopDoctors().stream().limit(5)
                .map(row -> Map.of("doctorName", row[0], "appointments", row[1])).toList());
        return stats;
    }

    @Override
    public List<UserDto> getUsers(String role) {
        return userRepository.findAll().stream().filter(u -> role == null || role.equalsIgnoreCase(u.getRole()))
                .map(u -> UserDto.builder().id(u.getId()).email(u.getEmail()).role(u.getRole())
                        .isActive(u.getIsActive()).createdAt(u.getCreatedAt()).updatedAt(u.getUpdatedAt()).build()).toList();
    }

    @Override @Transactional
    public void setUserActive(Long userId, boolean active) {
        com.datlichkhambenh.backend.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        if (!active && "ADMIN".equals(user.getRole())
                && userRepository.countByRoleAndIsActiveTrue("ADMIN") <= 1) {
            throw new com.datlichkhambenh.backend.exception.AppException("Không thể khóa quản trị viên hoạt động cuối cùng");
        }
        user.setIsActive(active); userRepository.save(user);
        if ("DOCTOR".equals(user.getRole())) {
            doctorRepository.findById(userId).ifPresent(d ->
                    redisCacheService.evictDoctorCache(d.getSpecialty().getId()));
        }
    }

    @Override @Transactional
    public SpecialtyDto updateSpecialty(Long id, SpecialtyRequestDto request) {
        Specialty s = specialtyRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyên khoa"));
        s.setName(request.getName()); s.setDescription(request.getDescription()); specialtyRepository.save(s);
        redisCacheService.evictDoctorCache(id);
        return SpecialtyDto.builder().id(s.getId()).name(s.getName()).description(s.getDescription()).build();
    }

    @Override @Transactional
    public void deleteSpecialty(Long id) {
        Specialty s = specialtyRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyên khoa"));
        if (!doctorRepository.findBySpecialtyId(id).isEmpty() || medicalServiceRepository.existsBySpecialtyId(id)) {
            throw new com.datlichkhambenh.backend.exception.AppException("Không thể xóa chuyên khoa đang được sử dụng");
        }
        specialtyRepository.delete(s); redisCacheService.evictDoctorCache(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAnalytics(int year) {
        // Group earned consultation fees by the scheduled consultation date.
        // These amounts do not verify payment; this system has no payment ledger.
        List<Appointment> yearly = appointmentRepository.findAll().stream()
                .filter(a -> a.getSchedule() != null && a.getSchedule().getWorkDate() != null
                        && a.getSchedule().getWorkDate().getYear() == year).toList();
        List<Appointment> completed = yearly.stream()
                .filter(a -> "COMPLETED".equals(a.getStatus())).toList();
        BigDecimal yearRevenue = earnedFees(completed);
        List<Map<String, Object>> months = new ArrayList<>();
        BigDecimal previousRevenue = BigDecimal.ZERO;

        for (int month = 1; month <= 12; month++) {
            final int selectedMonth = month;
            List<Appointment> entries = yearly.stream()
                    .filter(a -> a.getSchedule().getWorkDate().getMonthValue() == selectedMonth).toList();
            BigDecimal revenue = earnedFees(entries);
            Map<String, Object> summary = new HashMap<>();
            summary.put("month", month);
            summary.put("appointments", entries.size());
            summary.put("completed", entries.stream().filter(a -> "COMPLETED".equals(a.getStatus())).count());
            summary.put("cancelled", entries.stream().filter(a -> "CANCELLED".equals(a.getStatus())).count());
            summary.put("revenue", revenue);
            summary.put("growthPercent", previousRevenue.signum() > 0
                    ? revenue.subtract(previousRevenue).multiply(BigDecimal.valueOf(100))
                            .divide(previousRevenue, 1, RoundingMode.HALF_UP)
                    : null);
            months.add(summary);
            previousRevenue = revenue;
        }

        Map<Long, List<Appointment>> byDoctor = yearly.stream()
                .collect(Collectors.groupingBy(a -> a.getDoctor().getUserId()));
        List<Map<String, Object>> rankings = byDoctor.values().stream().map(entries -> {
            Doctor doctor = entries.get(0).getDoctor();
            return Map.<String, Object>of(
                    "doctorId", doctor.getUserId(),
                    "doctorName", doctor.getFullName(),
                    "specialty", doctor.getSpecialty() != null ? doctor.getSpecialty().getName() : "",
                    "appointments", entries.size(),
                    "confirmed", entries.stream().filter(a -> "CONFIRMED".equals(a.getStatus())).count(),
                    "completed", entries.stream().filter(a -> "COMPLETED".equals(a.getStatus())).count(),
                    "cancelled", entries.stream().filter(a -> "CANCELLED".equals(a.getStatus())).count(),
                    "revenue", earnedFees(entries));
        }).sorted((left, right) -> ((BigDecimal) right.get("revenue")).compareTo((BigDecimal) left.get("revenue")))
                .toList();

        Map<String, Long> statusCounts = Map.of(
                "CONFIRMED", yearly.stream().filter(a -> "CONFIRMED".equals(a.getStatus())).count(),
                "COMPLETED", (long) completed.size(),
                "CANCELLED", yearly.stream().filter(a -> "CANCELLED".equals(a.getStatus())).count());
        List<Map<String, Object>> dailyRevenue = completed.stream().filter(a -> a.getPrice() != null)
                .collect(Collectors.groupingBy(a -> a.getSchedule().getWorkDate(), java.util.TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Appointment::getPrice, BigDecimal::add)))
                .entrySet().stream().map(e -> Map.<String, Object>of("date", e.getKey(), "revenue", e.getValue()))
                .toList();

        // Keep the legacy collection key, but return actual completed appointments, never payment transactions.
        List<Map<String, Object>> transactions = completed.stream()
                .sorted(java.util.Comparator.comparing((Appointment a) -> a.getSchedule().getWorkDate()).reversed()
                        .thenComparing(Appointment::getId))
                .map(a -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("appointmentId", a.getId());
                    row.put("bookingCode", a.getBookingCode());
                    row.put("patientName", a.getPatient().getFullName());
                    row.put("patientPhone", a.getPatient().getPhone());
                    row.put("doctorName", a.getDoctor().getFullName());
                    row.put("specialty", a.getDoctor().getSpecialty() != null
                            ? a.getDoctor().getSpecialty().getName() : "");
                    row.put("amount", a.getPrice());
                    row.put("workDate", a.getSchedule().getWorkDate());
                    row.put("startTime", a.getSchedule().getStartTime());
                    row.put("month", a.getSchedule().getWorkDate().getMonthValue());
                    row.put("status", a.getStatus());
                    return row;
                }).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("year", year);
        result.put("months", months);
        result.put("doctorRankings", rankings);
        result.put("statusCounts", statusCounts);
        result.put("dailyRevenue", dailyRevenue);
        result.put("transactions", transactions);
        result.put("totalTransactions", transactions.size());
        result.put("yearAppointments", (long) yearly.size());
        result.put("yearCompletedAppointments", (long) completed.size());
        result.put("yearRevenue", yearRevenue);
        result.put("avgPerAppointment", completed.isEmpty() ? BigDecimal.ZERO
                : yearRevenue.divide(BigDecimal.valueOf(completed.size()), 2, RoundingMode.HALF_UP));
        result.put("revenueBasis", "COMPLETED_APPOINTMENT_FEES");
        result.put("paymentVerified", false);
        result.put("completedWithoutPrice", completed.stream().filter(entry -> entry.getPrice() == null).count());
        return result;
    }

    private BigDecimal earnedFees(List<Appointment> appointments) {
        return appointments.stream()
                .filter(a -> "COMPLETED".equals(a.getStatus()) && a.getPrice() != null)
                .map(Appointment::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorDto> getDoctors() {
        return doctorRepository.findAll().stream().map(this::mapDoctor).toList();
    }

    @Override
    @Transactional
    public DoctorDto createDoctor(AdminCreateDoctorDto request) {
        validateUniqueIdentity(request.getEmail(), request.getPhone());
        User user = userRepository.save(User.builder()
                .email(normalizeEmail(request.getEmail()))
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("DOCTOR").isActive(true).build());
        Specialty specialty = specialtyRepository.findById(request.getSpecialtyId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyên khoa"));
        Doctor doctor = doctorRepository.save(Doctor.builder().user(user).specialty(specialty)
                .fullName(request.getFullName().trim()).phone(request.getPhone()).degree(request.getDegree())
                .experienceYears(request.getExperienceYears()).consultationFee(request.getConsultationFee())
                .isApproved(request.getApproved() == null || request.getApproved()).build());
        redisCacheService.evictDoctorCache(specialty.getId());
        return mapDoctor(doctor);
    }

    @Override
    public DoctorDto updateDoctor(Long doctorId, UpdateDoctorProfileDto request) {
        return doctorService.updateProfile(doctorId, request);
    }

    @Override
    @Transactional
    public void deleteDoctor(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bác sĩ"));
        if (appointmentRepository.existsByDoctorUserId(doctorId) || scheduleRepository.existsByDoctorUserId(doctorId)) {
            throw new com.datlichkhambenh.backend.exception.AppException(
                    "Không thể xóa bác sĩ đã có lịch làm việc hoặc lịch hẹn; hãy khóa tài khoản thay thế");
        }
        Long specialtyId = doctor.getSpecialty().getId();
        User user = doctor.getUser();
        doctorRepository.delete(doctor);
        doctorRepository.flush();
        userRepository.delete(user);
        redisCacheService.evictDoctorCache(specialtyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientDto> getPatients() {
        return patientRepository.findAll().stream().map(this::mapPatient).toList();
    }

    @Override
    @Transactional
    public PatientDto createPatient(AdminCreatePatientDto request) {
        validateUniqueIdentity(request.getEmail(), request.getPhone());
        User user = userRepository.save(User.builder()
                .email(normalizeEmail(request.getEmail()))
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("PATIENT").isActive(true).build());
        Patient patient = patientRepository.save(Patient.builder().user(user).fullName(request.getFullName().trim())
                .phone(request.getPhone()).dob(request.getDob()).gender(request.getGender())
                .address(request.getAddress()).medicalHistory(request.getMedicalHistory()).build());
        return mapPatient(patient);
    }

    @Override
    public PatientDto updatePatient(Long patientId, UpdatePatientProfileDto request) {
        return patientService.updateProfile(patientId, request);
    }

    @Override
    @Transactional
    public void deletePatient(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bệnh nhân"));
        if (appointmentRepository.existsByPatientUserId(patientId)) {
            throw new com.datlichkhambenh.backend.exception.AppException(
                    "Không thể xóa bệnh nhân đã có lịch sử khám; hãy khóa tài khoản thay thế");
        }
        User user = patient.getUser();
        patientRepository.delete(patient);
        patientRepository.flush();
        userRepository.delete(user);
    }

    private void validateUniqueIdentity(String email, String phone) {
        if (userRepository.existsByEmailIgnoreCase(normalizeEmail(email))) {
            throw new com.datlichkhambenh.backend.exception.AppException("Email đã tồn tại");
        }
        if (phone != null && (doctorRepository.existsByPhone(phone) || patientRepository.existsByPhone(phone))) {
            throw new com.datlichkhambenh.backend.exception.AppException("Số điện thoại đã tồn tại");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private DoctorDto mapDoctor(Doctor d) {
        return DoctorDto.builder().userId(d.getUserId()).email(d.getUser().getEmail())
                .fullName(d.getFullName()).phone(d.getPhone()).degree(d.getDegree())
                .experienceYears(d.getExperienceYears()).consultationFee(d.getConsultationFee())
                .isApproved(d.getIsApproved()).specialtyId(d.getSpecialty().getId())
                .specialtyName(d.getSpecialty().getName()).build();
    }

    private PatientDto mapPatient(Patient p) {
        return PatientDto.builder().userId(p.getUserId()).email(p.getUser().getEmail())
                .fullName(p.getFullName()).phone(p.getPhone()).dob(p.getDob()).gender(p.getGender())
                .address(p.getAddress()).medicalHistory(p.getMedicalHistory()).build();
    }
}
