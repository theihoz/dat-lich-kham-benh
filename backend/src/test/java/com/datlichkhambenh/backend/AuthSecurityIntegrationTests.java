package com.datlichkhambenh.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSecurityIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicRegistrationCannotCreateAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"attacker@example.com","password":"Secret123",
                                 "role":"ADMIN","fullName":"Attacker","phone":"0900000000"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void adminApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void specialtiesAreAvailableForDoctorRegistration() throws Exception {
        mockMvc.perform(get("/api/v1/doctors/specialties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void missingFaviconDoesNotProduceServerError() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().isNoContent());
    }

    @Test
    void malformedJsonIsClientErrorNotServerError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":,}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void wrongHttpMethodIsMethodNotAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/auth/login"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void unknownApiPathIsNotFound() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void readinessProbeIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk());
    }
}
