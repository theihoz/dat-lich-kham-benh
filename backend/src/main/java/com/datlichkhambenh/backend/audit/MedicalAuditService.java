package com.datlichkhambenh.backend.audit;

import com.datlichkhambenh.backend.entity.*;
import com.datlichkhambenh.backend.repository.*;
import com.datlichkhambenh.backend.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Clinical content belongs only in restricted revision rows, never in audit metadata or logs. */
@Service
@RequiredArgsConstructor
public class MedicalAuditService {
    private final MedicalAuditEntryRepository entries;
    private final MedicalRecordRevisionRepository revisions;
    private final CurrentUserService currentUser;

    public Long resolveActor(Long trustedServiceActor) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetails details) {
            return currentUser.getUserId(details);
        }
        if (trustedServiceActor == null) throw new IllegalArgumentException("Audit actor required");
        return trustedServiceActor;
    }
    @Transactional
    public void record(Long actorId, String action, Long recordId, Long appointmentId) {
        entries.save(MedicalAuditEntry.builder().actorUserId(resolveActor(actorId)).action(action)
                .medicalRecordId(recordId).appointmentId(appointmentId).occurredAt(LocalDateTime.now(ZoneOffset.UTC)).build());
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void denied(Long actorId, String action, Long appointmentId) {
        record(actorId, action, null, appointmentId);
    }
    @Transactional
    public void revision(Long actorId, Long recordId, String beforeDiagnosis, String beforeNotes,
                         String afterDiagnosis, String afterNotes) {
        revisions.save(MedicalRecordRevision.builder().actorUserId(resolveActor(actorId)).medicalRecordId(recordId)
                .beforeDiagnosis(beforeDiagnosis).beforeNotes(beforeNotes).afterDiagnosis(afterDiagnosis)
                .afterNotes(afterNotes).occurredAt(LocalDateTime.now(ZoneOffset.UTC)).build());
    }
}
