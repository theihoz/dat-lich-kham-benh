package com.datlichkhambenh.backend.service;

import com.datlichkhambenh.backend.dto.request.LoginRequestDto;
import com.datlichkhambenh.backend.dto.request.RefreshTokenRequestDto;
import com.datlichkhambenh.backend.entity.Doctor;
import com.datlichkhambenh.backend.entity.User;
import com.datlichkhambenh.backend.exception.AppException;
import com.datlichkhambenh.backend.repository.DoctorRepository;
import com.datlichkhambenh.backend.repository.PatientRepository;
import com.datlichkhambenh.backend.repository.SpecialtyRepository;
import com.datlichkhambenh.backend.repository.UserRepository;
import com.datlichkhambenh.backend.security.JwtTokenProvider;
import com.datlichkhambenh.backend.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.BeforeEach;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTests {
    @Mock UserRepository userRepository;
    @Mock PatientRepository patientRepository;
    @Mock DoctorRepository doctorRepository;
    @Mock SpecialtyRepository specialtyRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    JwtTokenProvider jwtTokenProvider;
    AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret",
                "test-secret-key-for-unit-tests-only-must-be-256bit-long-enough");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 3_600_000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshExpirationMs", 86_400_000L);
        authService = new AuthServiceImpl(userRepository, patientRepository, doctorRepository,
                specialtyRepository, passwordEncoder, authenticationManager, jwtTokenProvider);
        ReflectionTestUtils.setField(authService, "sessions", new com.datlichkhambenh.backend.security.AuthSessionService(
                org.mockito.Mockito.mock(com.datlichkhambenh.backend.repository.AuthRefreshSessionRepository.class),
                userRepository, doctorRepository, jwtTokenProvider, new org.springframework.mock.env.MockEnvironment()));
    }

    @Test
    void pendingDoctorCannotLogin() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("doctor@example.com");
        request.setPassword("Secret123");
        User user = User.builder().id(7L).email("doctor@example.com").role("DOCTOR").isActive(true).build();
        Doctor doctor = Doctor.builder().user(user).isApproved(false).build();
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmailIgnoreCase("doctor@example.com")).thenReturn(Optional.of(user));
        when(doctorRepository.findById(7L)).thenReturn(Optional.of(doctor));

        assertThrows(AppException.class, () -> authService.login(request));
    }

    @Test
    void accessTokenCannotBeUsedAsRefreshToken() {
        String accessToken = jwtTokenProvider.generateToken("patient@example.com", "PATIENT");
        RefreshTokenRequestDto request = new RefreshTokenRequestDto(accessToken);

        assertThrows(AppException.class, () -> authService.refreshToken(request));
    }
}
