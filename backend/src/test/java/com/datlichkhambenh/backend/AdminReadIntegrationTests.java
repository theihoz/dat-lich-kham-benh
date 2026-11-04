package com.datlichkhambenh.backend;
import com.datlichkhambenh.backend.entity.User;
import com.datlichkhambenh.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class AdminReadIntegrationTests {
 @Autowired MockMvc mvc; @Autowired UserRepository users;
 @Test @WithMockUser(authorities="ADMIN") void usersArePagedAndFilteredByDatabase() throws Exception {
  for(int i=0;i<5;i++) users.save(User.builder().email("paging"+i+"@example.test").passwordHash("unused").role("PATIENT").isActive(true).build());
  users.save(User.builder().email("paging-admin@example.test").passwordHash("unused").role("ADMIN").isActive(true).build());
  mvc.perform(get("/api/v1/admin/users").param("role","PATIENT").param("page","1").param("size","2"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2)).andExpect(header().string("X-Total-Count","5"));
 }
 @Test @WithMockUser(authorities="ADMIN") void appointmentQueryIsAvailableAndRejectsUnboundedPageSize() throws Exception {
  mvc.perform(get("/api/v1/admin/appointments").param("size","100000")).andExpect(status().isBadRequest());
  mvc.perform(get("/api/v1/admin/appointments").param("size","20")).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
 }
}
