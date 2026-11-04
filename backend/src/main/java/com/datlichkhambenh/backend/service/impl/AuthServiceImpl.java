package com.datlichkhambenh.backend.service.impl;

import com.datlichkhambenh.backend.dto.request.LoginRequestDto;
import com.datlichkhambenh.backend.dto.request.RefreshTokenRequestDto;
import com.datlichkhambenh.backend.dto.request.RegisterRequestDto;
import com.datlichkhambenh.backend.dto.response.LoginResponseDto;
import com.datlichkhambenh.backend.dto.response.UserDto;
import com.datlichkhambenh.backend.entity.Doctor;
import com.datlichkhambenh.backend.entity.Patient;
import com.datlichkhambenh.backend.entity.Specialty;
import com.datlichkhambenh.backend.entity.User;
import com.datlichkhambenh.backend.exception.AppException;
import com.datlichkhambenh.backend.exception.ResourceNotFoundException;
import com.datlichkhambenh.backend.repository.DoctorRepository;
import com.datlichkhambenh.backend.repository.PatientRepository;
import com.datlichkhambenh.backend.repository.SpecialtyRepository;
import com.datlichkhambenh.backend.repository.UserRepository;
import com.datlichkhambenh.backend.security.JwtTokenProvider;
import com.datlichkhambenh.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @org.springframework.beans.factory.annotation.Autowired
    private com.datlichkhambenh.backend.security.AuthSessionService sessions;
    @org.springframework.beans.factory.annotation.Autowired
    private com.datlichkhambenh.backend.security.AuthActionTokenService actionTokens;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

    @Override
    @Transactional
    public UserDto register(RegisterRequestDto request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new AppException("Email đã tồn tại trên hệ thống");
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()
                && (patientRepository.existsByPhone(request.getPhone()) || doctorRepository.existsByPhone(request.getPhone()))) {
            throw new AppException("Số điện thoại đã tồn tại trên hệ thống");
        }

        String role = request.getRole().toUpperCase();
        if (!"PATIENT".equals(role) && !"DOCTOR".equals(role)) {
            throw new AppException("Chỉ được đăng ký tài khoản PATIENT hoặc DOCTOR");
        }
        if (request.getPhone() == null || request.getPhone().isBlank()) {
            throw new AppException("Số điện thoại không được để trống");
        }
        if ("DOCTOR".equals(role)
                && (request.getExperienceYears() == null || request.getConsultationFee() == null)) {
            throw new AppException("Bác sĩ phải cung cấp số năm kinh nghiệm và giá khám");
        }
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .isActive(true)
                .build();

        user.setEmailVerified(false);
        User savedUser = userRepository.save(user);

        if ("PATIENT".equals(role)) {
            Patient patient = Patient.builder()
                    .user(savedUser)
                    .fullName(request.getFullName())
                    .phone(request.getPhone())
                    .dob(request.getDob())
                    .gender(request.getGender())
                    .address(request.getAddress())
                    .medicalHistory(request.getMedicalHistory())
                    .build();
            patientRepository.save(patient);
        } else if ("DOCTOR".equals(role)) {
            if (request.getSpecialtyId() == null) {
                throw new AppException("Bác sĩ phải đăng ký thuộc 1 chuyên khoa (specialtyId)");
            }
            Specialty specialty = specialtyRepository.findById(request.getSpecialtyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyên khoa"));

            Doctor doctor = Doctor.builder()
                    .user(savedUser)
                    .specialty(specialty)
                    .fullName(request.getFullName())
                    .phone(request.getPhone())
                    .degree(request.getDegree())
                    .experienceYears(request.getExperienceYears())
                    .consultationFee(request.getConsultationFee())
                    .isApproved(false)
                    .build();
            doctorRepository.save(doctor);
        }

        actionTokens.request(savedUser.getEmail(), com.datlichkhambenh.backend.security.AuthActionTokenService.VERIFY);
        return UserDto.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .isActive(savedUser.getIsActive())
                .createdAt(savedUser.getCreatedAt())
                .updatedAt(savedUser.getUpdatedAt())
                .build();
    }

    @Override
    public LoginResponseDto login(LoginRequestDto request) {
        String email = normalizeEmail(request.getEmail());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        ensureAccountCanAuthenticate(user);

        return sessions.issue(user);
    }

    @Override
    public LoginResponseDto refreshToken(RefreshTokenRequestDto request) {
        return sessions.rotate(request.getRefreshToken());
    }

    private void ensureAccountCanAuthenticate(User user) {
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AppException("Tài khoản đã bị khóa", HttpStatus.FORBIDDEN);
        }
        if ("DOCTOR".equals(user.getRole())) {
            Doctor doctor = doctorRepository.findById(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ bác sĩ"));
            if (!Boolean.TRUE.equals(doctor.getIsApproved())) {
                throw new AppException("Tài khoản bác sĩ chưa được Admin phê duyệt", HttpStatus.FORBIDDEN);
            }
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    @Transactional
    public void changePassword(String email, com.datlichkhambenh.backend.dto.request.ChangePasswordDto request) {
        User found = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        User user = userRepository.findAuthUserForUpdate(found.getId()).orElseThrow();
        entityManager.refresh(user, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException("Mật khẩu hiện tại không đúng");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setCredentialsVersion(user.getCredentialsVersion() + 1);
        userRepository.save(user);
        sessions.revokeAll(user);
        actionTokens.invalidateResetTokens(user);
    }
}
