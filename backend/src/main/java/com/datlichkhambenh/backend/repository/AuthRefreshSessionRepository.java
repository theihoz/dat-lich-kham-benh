package com.datlichkhambenh.backend.repository;
import com.datlichkhambenh.backend.entity.AuthRefreshSession;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;
public interface AuthRefreshSessionRepository extends JpaRepository<AuthRefreshSession,String> {
 Optional<AuthRefreshSession> findByTokenHash(String hash);
 boolean existsByFamilyIdAndUserIdAndRevokedAtIsNullAndExpiresAtAfter(String familyId,Long userId,Instant now);
 @Modifying @Query("update AuthRefreshSession s set s.revokedAt=:now where s.familyId=:family and s.revokedAt is null")
 int revokeFamily(@Param("family") String family,@Param("now") Instant now);
 @Modifying @Query("update AuthRefreshSession s set s.revokedAt=:now where s.user.id=:userId and s.revokedAt is null")
 int revokeUser(@Param("userId") Long userId,@Param("now") Instant now);
}
