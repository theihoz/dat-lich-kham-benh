package com.datlichkhambenh.backend.repository;

import com.datlichkhambenh.backend.entity.NotificationOutbox;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.*;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, String> {
    Optional<NotificationOutbox> findByEventKey(String eventKey);
    boolean existsByEventKey(String eventKey);
    long countByStatus(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NotificationOutbox n where n.status = 'PENDING' and n.nextAttemptAt <= :now order by n.createdAt, n.id")
    List<NotificationOutbox> findDueForUpdate(@Param("now") LocalDateTime now, Pageable pageable);
}
