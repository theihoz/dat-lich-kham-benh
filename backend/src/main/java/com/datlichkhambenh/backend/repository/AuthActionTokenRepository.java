package com.datlichkhambenh.backend.repository;
import com.datlichkhambenh.backend.entity.AuthActionToken;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
public interface AuthActionTokenRepository extends JpaRepository<AuthActionToken,String> {
 @Modifying @Query("update AuthActionToken t set t.consumedAt=:now where t.user.id=:userId and t.purpose=:purpose and t.consumedAt is null")
 int consumePrevious(@Param("userId") Long userId,@Param("purpose") String purpose,@Param("now") Instant now);
}
