package com.datlichkhambenh.backend.repository;

import com.datlichkhambenh.backend.entity.MedicalRecordRevision;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.*;

public interface MedicalRecordRevisionRepository extends JpaRepository<MedicalRecordRevision, Long> {
    List<MedicalRecordRevision> findByMedicalRecordIdOrderByIdAsc(Long medicalRecordId);
}
