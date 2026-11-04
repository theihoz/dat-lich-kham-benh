package com.datlichkhambenh.backend;

import com.datlichkhambenh.backend.entity.User;
import com.datlichkhambenh.backend.repository.UserRepository;
import com.datlichkhambenh.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "management.endpoints.web.exposure.include=health,info")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PasswordSecurityRegressionTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtTokenProvider tokens;

    private static final String PASSWORD_REQUEST = """
            {"currentPassword":"OldSecret123","newPassword":"NewSecret123"}
            """;

    @Test
    void adminAccessTokenCanReadProtectedActuatorEndpoint() throws Exception {
        User admin = users.saveAndFlush(User.builder().email("actuator-regression@example.com")
                .passwordHash(encoder.encode("OldSecret123")).role("ADMIN").isActive(true).build());
        mvc.perform(get("/actuator/info").servletPath("/actuator/info")
                .header("Authorization", "Bearer " + tokens.generateToken(admin.getEmail(), admin.getRole())))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousPasswordChangeRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/v1/auth/change-password").servletPath("/api/v1/auth/change-password").contentType(MediaType.APPLICATION_JSON)
                .content(PASSWORD_REQUEST)).andExpect(status().isUnauthorized());
    }

    @Test
    void accessTokenCanChangeOwnPassword() throws Exception {
        User user = users.saveAndFlush(User.builder().email("password-regression@example.com")
                .passwordHash(encoder.encode("OldSecret123")).role("PATIENT").isActive(true).build());
        mvc.perform(post("/api/v1/auth/change-password").servletPath("/api/v1/auth/change-password").contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokens.generateToken(user.getEmail(), user.getRole()))
                .content(PASSWORD_REQUEST)).andExpect(status().isOk());
        assertTrue(encoder.matches("NewSecret123", users.findById(user.getId()).orElseThrow().getPasswordHash()));
    }

    @Test
    void refreshTokenCannotChangePassword() throws Exception {
        User user = users.saveAndFlush(User.builder().email("refresh-password-regression@example.com")
                .passwordHash(encoder.encode("OldSecret123")).role("PATIENT").isActive(true).build());
        mvc.perform(post("/api/v1/auth/change-password").servletPath("/api/v1/auth/change-password").contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokens.generateRefreshToken(user.getEmail()))
                .content(PASSWORD_REQUEST)).andExpect(status().isUnauthorized());
    }
}
