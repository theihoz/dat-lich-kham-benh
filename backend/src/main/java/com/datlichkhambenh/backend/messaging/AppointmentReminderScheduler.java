package com.datlichkhambenh.backend.messaging;

import com.datlichkhambenh.backend.repository.AppointmentRepository;
import com.datlichkhambenh.backend.service.AsyncEmailNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AppointmentReminderScheduler {
    private final AppointmentRepository appointments;
    private final AsyncEmailNotificationService notifications;

    /** Catch up within the next 24h; unique event keys prevent a restart from making duplicates. */
    @Transactional
    public int enqueueDueReminders(LocalDateTime now) {
        LocalDateTime until = now.plusHours(24);
        var due = appointments.findReminderCandidates(now.toLocalDate(), now.toLocalTime(),
                until.toLocalDate(), until.toLocalTime(), PageRequest.of(0, 200));
        for (var appointment : due) notifications.sendAppointmentReminder(appointment);
        return due.size();
    }
}
