package com.datlichkhambenh.backend.security;
import com.datlichkhambenh.backend.entity.*;
import com.datlichkhambenh.backend.repository.*;
import com.datlichkhambenh.backend.exception.AppException;
import com.datlichkhambenh.backend.dto.response.LoginResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class AuthSessionService {
 private final AuthRefreshSessionRepository sessions;
 private final UserRepository users;
 private final DoctorRepository doctors;
 private final JwtTokenProvider jwt;
 private final Environment environment;
 @Value("${app.jwt.refresh-expiration-ms:604800000}") private long ttl;
 @Transactional public LoginResponseDto issue(User user) {
  User locked=users.findAuthUserForUpdate(user.getId()).orElseThrow(this::unauthorized);
  entityManager.refresh(locked,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
  ensureCanAuthenticate(locked);
  return create(locked,UUID.randomUUID().toString());
 }
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED,noRollbackFor=AppException.class) public LoginResponseDto rotate(String raw) {
  AuthRefreshSession found=sessions.findByTokenHash(AuthTokenSecrets.hash(raw)).orElseThrow(this::unauthorized);
  User user=users.findAuthUserForUpdate(found.getUser().getId()).orElseThrow(this::unauthorized);
  // Refresh the entity after serializing on user, including concurrent rotations of the same token.
  entityManager.refresh(user,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
  entityManager.refresh(found,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
  if(found.getRevokedAt()!=null) { sessions.revokeFamily(found.getFamilyId(),Instant.now()); throw unauthorized(); }
  if(!found.getExpiresAt().isAfter(Instant.now()) || found.getCredentialsVersion()!=user.getCredentialsVersion()) {
   sessions.revokeFamily(found.getFamilyId(),Instant.now()); throw unauthorized();
  }
  ensureCanAuthenticate(user);
  found.setRevokedAt(Instant.now()); sessions.saveAndFlush(found);
  return create(user,found.getFamilyId());
 }
 @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
 @Transactional public void logout(String raw) {
  sessions.findByTokenHash(AuthTokenSecrets.hash(raw)).ifPresent(session -> {
   users.findAuthUserForUpdate(session.getUser().getId());
   sessions.revokeFamily(session.getFamilyId(),Instant.now());
  });
 }
 @Transactional public void revokeAll(User user) { sessions.revokeUser(user.getId(),Instant.now()); }
 @Transactional(readOnly=true) public boolean isFamilyActive(String family,Long userId) {
  return sessions.existsByFamilyIdAndUserIdAndRevokedAtIsNullAndExpiresAtAfter(family,userId,Instant.now());
 }
 public void ensureCanAuthenticate(User user) {
  if(!Boolean.TRUE.equals(user.getIsActive())) throw unauthorized();
  if(environment.acceptsProfiles(Profiles.of("prod")) && !user.isEmailVerified())
   throw new AppException("Vui lòng xác minh email trước khi đăng nhập",HttpStatus.FORBIDDEN);
  if("DOCTOR".equals(user.getRole()) && !doctors.findById(user.getId()).map(d->Boolean.TRUE.equals(d.getIsApproved())).orElse(false))
   throw new AppException("Tài khoản bác sĩ chưa được phê duyệt",HttpStatus.FORBIDDEN);
 }
 private LoginResponseDto create(User user,String family) {
  String raw=AuthTokenSecrets.generate(); AuthRefreshSession session=new AuthRefreshSession();
  session.setId(UUID.randomUUID().toString()); session.setTokenHash(AuthTokenSecrets.hash(raw));
  session.setFamilyId(family); session.setUser(user); session.setCredentialsVersion(user.getCredentialsVersion());
  session.setExpiresAt(Instant.now().plusMillis(ttl)); sessions.save(session);
  return LoginResponseDto.builder().accessToken(jwt.generateToken(user.getEmail(),user.getRole(),user.getCredentialsVersion(),family))
   .refreshToken(raw).tokenType("Bearer").userId(user.getId()).email(user.getEmail()).role(user.getRole()).build();
 }
 private AppException unauthorized() { return new AppException("Phiên đăng nhập không hợp lệ hoặc đã hết hạn",HttpStatus.UNAUTHORIZED); }
}
