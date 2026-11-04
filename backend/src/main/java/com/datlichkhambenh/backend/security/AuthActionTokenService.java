package com.datlichkhambenh.backend.security;
import com.datlichkhambenh.backend.entity.*;
import com.datlichkhambenh.backend.repository.*;
import com.datlichkhambenh.backend.messaging.NotificationDeliveryService;
import com.datlichkhambenh.backend.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Locale;
@Service @RequiredArgsConstructor
public class AuthActionTokenService {
 public static final String RESET="PASSWORD_RESET", VERIFY="EMAIL_VERIFICATION";
 private final UserRepository users;
 private final AuthActionTokenRepository tokens;
 private final AuthSessionService sessions;
 private final PasswordEncoder passwords;
 private final NotificationDeliveryService delivery;
 @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;
 @Transactional public void request(String email,String purpose) {
  users.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT)).filter(u->Boolean.TRUE.equals(u.getIsActive()))
   .ifPresent(u->{ User user=users.findAuthUserForUpdate(u.getId()).orElseThrow();
    if(VERIFY.equals(purpose) && user.isEmailVerified()) return;
    tokens.consumePrevious(user.getId(),purpose,Instant.now());
    String raw=AuthTokenSecrets.generate(); AuthActionToken token=new AuthActionToken();
    token.setTokenHash(AuthTokenSecrets.hash(raw));token.setUser(user);token.setPurpose(purpose);
    token.setExpiresAt(Instant.now().plusSeconds(RESET.equals(purpose)?1800:86400));tokens.save(token);
    delivery.enqueueAccountMessage(purpose,user.getEmail(),raw);
   });
 }
 @Transactional public void invalidateResetTokens(User user) { tokens.consumePrevious(user.getId(),RESET,Instant.now()); }
 @Transactional public void reset(String raw,String password) {
  AuthActionToken token=consume(raw,RESET); User user=token.getUser();
  user.setPasswordHash(passwords.encode(password));user.setCredentialsVersion(user.getCredentialsVersion()+1);
  users.save(user);sessions.revokeAll(user);
 }
 @Transactional public void verify(String raw) { AuthActionToken token=consume(raw,VERIFY);token.getUser().setEmailVerified(true); }
 private AuthActionToken consume(String raw,String purpose) {
  AuthActionToken token=tokens.findById(AuthTokenSecrets.hash(raw)).orElseThrow(this::invalid);
  User locked=users.findAuthUserForUpdate(token.getUser().getId()).orElseThrow(this::invalid);
  em.refresh(locked,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
  em.refresh(token,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
  if(!purpose.equals(token.getPurpose()) || token.getConsumedAt()!=null || !token.getExpiresAt().isAfter(Instant.now())) throw invalid();
  token.setConsumedAt(Instant.now());tokens.save(token);return token;
 }
 private AppException invalid() { return new AppException("Liên kết không hợp lệ, đã sử dụng hoặc hết hạn"); }
}
