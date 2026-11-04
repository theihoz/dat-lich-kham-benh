package com.datlichkhambenh.backend;
import com.datlichkhambenh.backend.entity.*;
import com.datlichkhambenh.backend.repository.*;
import com.datlichkhambenh.backend.security.*;
import com.datlichkhambenh.backend.messaging.NotificationDeliveryService;
import com.datlichkhambenh.backend.exception.AppException;
import com.datlichkhambenh.backend.dto.request.LoginRequestDto;
import com.datlichkhambenh.backend.service.AuthService;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties={"app.public-url=http://localhost","app.auth.rate-limit.enabled=false"})
@AutoConfigureMockMvc @ActiveProfiles("test")
class AuthAccountTokenTests {
 @Autowired UserRepository users;
 @Autowired AuthActionTokenRepository tokens;
 @Autowired AuthRefreshSessionRepository refreshSessions;
 @Autowired AuthActionTokenService actions;
 @Autowired AuthSessionService sessions;
 @Autowired AuthService auth;
 @Autowired PasswordEncoder passwords;
 @MockBean NotificationDeliveryService delivery;
 User user;
 @BeforeEach void account() {
  user=User.builder().email(UUID.randomUUID()+"@example.com").role("PATIENT")
    .passwordHash(passwords.encode("BeforePassword123")).isActive(true).build();user.setEmailVerified(false);users.saveAndFlush(user);
 }
 String request(String purpose) {
  actions.request(user.getEmail(),purpose);
  ArgumentCaptor<String> raw=ArgumentCaptor.forClass(String.class);
  verify(delivery).enqueueAccountMessage(eq(purpose),eq(user.getEmail()),raw.capture());
  return raw.getValue();
 }
 @Test void resetHashesTokenAndUsesItOnceWhileRevokingSessions() {
  var login=sessions.issue(user);
  String raw=request(AuthActionTokenService.RESET);
  assertFalse(tokens.existsById(raw));assertTrue(tokens.existsById(AuthTokenSecrets.hash(raw)));
  actions.reset(raw,"AfterPassword123");
  User changed=users.findById(user.getId()).orElseThrow();
  assertTrue(passwords.matches("AfterPassword123",changed.getPasswordHash()));assertEquals(1,changed.getCredentialsVersion());
  assertThrows(AppException.class,()->actions.reset(raw,"AnotherPassword123"));
  assertThrows(AppException.class,()->sessions.rotate(login.getRefreshToken()));
 }
 @Test void resetRejectsExpiredTokenAndKeepsPassword() {
  String raw=request(AuthActionTokenService.RESET);AuthActionToken token=tokens.findById(AuthTokenSecrets.hash(raw)).orElseThrow();
  token.setExpiresAt(Instant.now().minusSeconds(1));tokens.saveAndFlush(token);
  assertThrows(AppException.class,()->actions.reset(raw,"AfterPassword123"));
  assertTrue(passwords.matches("BeforePassword123",users.findById(user.getId()).orElseThrow().getPasswordHash()));
 }
 @Test void verificationHas24HourLifetimeAndCannotBeReused() {
  String raw=request(AuthActionTokenService.VERIFY);
  assertTrue(tokens.findById(AuthTokenSecrets.hash(raw)).orElseThrow().getExpiresAt().isAfter(Instant.now().plusSeconds(86300)));
  actions.verify(raw);assertTrue(users.findById(user.getId()).orElseThrow().isEmailVerified());
  assertThrows(AppException.class,()->actions.verify(raw));
 }
 @Test void resetTokenCannotVerifyEmail() {
  String raw=request(AuthActionTokenService.RESET);assertThrows(AppException.class,()->actions.verify(raw));
  assertFalse(users.findById(user.getId()).orElseThrow().isEmailVerified());
 }
 @Test void refreshTokenDatabaseContainsOnlyHash() {
  var issued=sessions.issue(user);
  assertFalse(issued.getRefreshToken().contains("."));
  AuthRefreshSession stored=refreshSessions.findByTokenHash(AuthTokenSecrets.hash(issued.getRefreshToken())).orElseThrow();
  assertNotEquals(issued.getRefreshToken(),stored.getTokenHash());assertEquals(64,stored.getTokenHash().length());
 }
 @Test void expiredRefreshSessionIsRejected() {
  var issued=sessions.issue(user);var stored=refreshSessions.findByTokenHash(AuthTokenSecrets.hash(issued.getRefreshToken())).orElseThrow();
  stored.setExpiresAt(Instant.now().minusSeconds(1));refreshSessions.saveAndFlush(stored);
  assertThrows(AppException.class,()->sessions.rotate(issued.getRefreshToken()));
 }
 @Test void concurrentRefreshReuseRevokesWholeFamily() throws Exception {
  var issued=sessions.issue(user);
  var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
  var start=new java.util.concurrent.CountDownLatch(1);
  java.util.concurrent.Callable<Boolean> rotate=()->{start.await();try{sessions.rotate(issued.getRefreshToken());return true;}catch(AppException rejected){return false;}};
  try {
   var first=pool.submit(rotate);var second=pool.submit(rotate);start.countDown();
   assertNotEquals(first.get(5,java.util.concurrent.TimeUnit.SECONDS),second.get(5,java.util.concurrent.TimeUnit.SECONDS));
   var stored=refreshSessions.findByTokenHash(AuthTokenSecrets.hash(issued.getRefreshToken())).orElseThrow();
   assertFalse(sessions.isFamilyActive(stored.getFamilyId(),user.getId()));
  } finally {pool.shutdownNow();}
 }
}
