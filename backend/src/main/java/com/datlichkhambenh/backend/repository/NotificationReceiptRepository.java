package com.datlichkhambenh.backend.repository;

import com.datlichkhambenh.backend.entity.NotificationReceipt;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.*;

public interface NotificationReceiptRepository extends JpaRepository<NotificationReceipt, String> {
}
