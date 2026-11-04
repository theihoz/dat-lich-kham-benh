package com.datlichkhambenh.backend;

import com.datlichkhambenh.backend.entity.User;
import com.datlichkhambenh.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"app.public-url=http://localhost", "app.auth.rate-limit.enabled=false"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSessionLifecycleTests {
    @org.springframework.boot.test.mock.mockito.MockBean com.datlichkhambenh.backend.messaging.NotificationDeliveryService delivery;
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired ObjectMapper json;
    String email;
    @BeforeEach void account() {
        email = UUID.randomUUID()+"@example.com";
        users.saveAndFlush(User.builder().email(email).passwordHash(passwords.encode("OldPassword123"))
                .role("PATIENT").isActive(true).build());
    }
    MvcResult login() throws Exception {
        return mvc.perform(post("/api/v1/auth/login").servletPath("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(
                        java.util.Map.of("email",email,"password","OldPassword123"))))
                .andExpect(status().isOk()).andReturn();
    }
    String access(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsString()).at("/data/accessToken").asText(); }
    Cookie refresh(MvcResult result) { Cookie cookie=result.getResponse().getCookie("clinic_refresh"); assertNotNull(cookie); return cookie; }
    @Test void browserLoginUsesHttpOnlyCookieAndDoesNotReturnRefreshToken() throws Exception {
        MvcResult result=login();
        assertTrue(refresh(result).isHttpOnly());
        assertEquals("Strict",refresh(result).getAttribute("SameSite"));
        assertEquals("/api/v1/auth",refresh(result).getPath());
        assertTrue(json.readTree(result.getResponse().getContentAsString()).at("/data/refreshToken").isMissingNode());
    }
    @Test void rotationRejectsReplayAndRevokesTokenFamily() throws Exception {
        Cookie old=refresh(login());
        MvcResult rotated=mvc.perform(post("/api/v1/auth/refresh").header("Origin","http://localhost")
                .header("X-CSRF-Token","clinic").cookie(old)).andExpect(status().isOk()).andReturn();
        Cookie next=refresh(rotated); assertNotEquals(old.getValue(),next.getValue());
        mvc.perform(post("/api/v1/auth/refresh").header("Origin","http://localhost")
                .header("X-CSRF-Token","clinic").cookie(old)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").header("Origin","http://localhost")
                .header("X-CSRF-Token","clinic").cookie(next)).andExpect(status().isUnauthorized());
    }
    @Test void cookieRefreshRejectsCrossOrigin() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh").header("Origin","https://attacker.example")
                .header("X-CSRF-Token","clinic").cookie(refresh(login()))).andExpect(status().isForbidden());
    }
    @Test void cookieRefreshRequiresCsrfHeader() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh").header("Origin","http://localhost")
                .cookie(refresh(login()))).andExpect(status().isForbidden());
    }
    @Test void logoutRevokesRefreshSession() throws Exception {
        MvcResult session=login();
        Cookie token=refresh(session);
        mvc.perform(post("/api/v1/auth/logout").header("Origin","http://localhost")
                .header("X-CSRF-Token","clinic").cookie(token)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/stats").servletPath("/api/v1/admin/stats")
                .header("Authorization","Bearer "+access(session))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").header("Origin","http://localhost")
                .header("X-CSRF-Token","clinic").cookie(token)).andExpect(status().isUnauthorized());
    }
    @Test void passwordChangeRevokesExistingAccessAndRefreshTokens() throws Exception {
        MvcResult session=login();
        mvc.perform(post("/api/v1/auth/change-password").servletPath("/api/v1/auth/change-password")
                .header("Authorization","Bearer "+access(session)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"OldPassword123\",\"newPassword\":\"NewPassword123\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/stats").servletPath("/api/v1/admin/stats")
                .header("Authorization","Bearer "+access(session))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").header("Origin","http://localhost")
                .header("X-CSRF-Token","clinic").cookie(refresh(session))).andExpect(status().isUnauthorized());
    }
    @Test void resetRequestDoesNotRevealWhetherEmailExists() throws Exception {
        String existing=mvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(java.util.Map.of("email",email))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String unknown=mvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"missing@example.com\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(existing,unknown);
    }
    @Test void explicitApiModeSupportsBodyTokensWithoutCookies() throws Exception {
        MvcResult login=mvc.perform(post("/api/v1/auth/login").header("X-Auth-Client","api")
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(
                        java.util.Map.of("email",email,"password","OldPassword123"))))
                .andExpect(status().isOk()).andReturn();
        assertNull(login.getResponse().getCookie("clinic_refresh"));
        String raw=json.readTree(login.getResponse().getContentAsString()).at("/data/refreshToken").asText();
        assertFalse(raw.isBlank());
        mvc.perform(post("/api/v1/auth/refresh").header("X-Auth-Client","api")
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("refreshToken",raw))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }
}
