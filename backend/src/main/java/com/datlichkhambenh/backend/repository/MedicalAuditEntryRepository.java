package com.datlichkhambenh.backend.repository;

import com.datlichkhambenh.backend.entity.MedicalAuditEntry;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.*;

public interface MedicalAuditEntryRepository extends JpaRepository<MedicalAuditEntry, Long> {
    List<MedicalAuditEntry> findByAppointmentIdOrderByIdAsc(Long appointmentId);
}
