package com.datlichkhambenh.backend;

import com.datlichkhambenh.backend.dto.request.CreateBookingDto;
import com.datlichkhambenh.backend.dto.request.CreateScheduleDto;
import com.datlichkhambenh.backend.exception.AppException;
import com.datlichkhambenh.backend.dto.request.UpdateMedicalRecordDto;
import com.datlichkhambenh.backend.entity.*;
import com.datlichkhambenh.backend.exception.BookingConflictException;
import com.datlichkhambenh.backend.messaging.EmailNotificationMessage;
import com.datlichkhambenh.backend.repository.*;
import com.datlichkhambenh.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BookingFlowIntegrationTests {
    @Autowired UserRepository users;
    @Autowired PatientRepository patients;
    @Autowired DoctorRepository doctors;
    @Autowired SpecialtyRepository specialties;
    @Autowired ScheduleRepository schedules;
    @Autowired AppointmentRepository appointments;
    @Autowired MedicalRecordRepository records;
    @Autowired MedicalAuditEntryRepository auditEntries;
    @Autowired MedicalRecordRevisionRepository revisions;
    @Autowired NotificationOutboxRepository outbox;
    @Autowired com.datlichkhambenh.backend.messaging.AppointmentReminderScheduler reminders;
    @Autowired BookingService bookings;
    @Autowired DoctorScheduleService doctorSchedules;
    @Autowired MedicalRecordService medicalRecords;
    @Autowired PlatformTransactionManager transactionManager;
    @MockBean RabbitTemplate rabbit;
    @MockBean com.datlichkhambenh.backend.pattern.notification.NotificationSender sender;
    @Autowired com.datlichkhambenh.backend.messaging.DurableNotificationConsumer delivery;
    private static final java.util.concurrent.atomic.AtomicInteger fixtureDay = new java.util.concurrent.atomic.AtomicInteger(4);
    Patient patient;
    Doctor doctor;
    Schedule slot;

    @BeforeEach void seedIsolatedData() {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
        String unique = UUID.randomUUID().toString();
        Specialty specialty = specialties.save(Specialty.builder().name("Khoa test " + unique).build());
        User patientUser = users.save(User.builder().email(unique + "@patient.test").passwordHash("test-only")
                .role("PATIENT").isActive(true).build());
        patient = patients.save(Patient.builder().user(patientUser).fullName("Patient test").phone(unique.substring(0, 18)).build());
        User doctorUser = users.save(User.builder().email(unique + "@doctor.test").passwordHash("test-only")
                .role("DOCTOR").isActive(true).build());
        doctor = doctors.save(Doctor.builder().user(doctorUser).fullName("Doctor test").phone(unique.substring(0, 18))
                .specialty(specialty).degree("BS").experienceYears(5).consultationFee(new BigDecimal("350000"))
                .isApproved(true).build());
        slot = schedules.save(Schedule.builder().doctor(doctor).workDate(LocalDate.now().plusDays(fixtureDay.getAndAdd(3)))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(9, 30)).isAvailable(true).build());
        });
        reset(rabbit);
    }

    @Test void bookingCommitsThenObserverQueuesImmutableNotification() {
        var booking = bookings.createBooking(patient.getUserId(), new CreateBookingDto(slot.getId(), "Khám định kỳ"));
        assertNotNull(booking.getId());
        assertEquals("CONFIRMED", appointments.findById(booking.getId()).orElseThrow().getStatus());
        assertFalse(schedules.findById(slot.getId()).orElseThrow().getIsAvailable());
        var event = outbox.findByEventKey("BOOKING_CONFIRMED:" + booking.getBookingCode()).orElseThrow();
        assertEquals("PENDING", event.getStatus());
        assertTrue(event.getPayload().contains(booking.getBookingCode()));
        verifyNoInteractions(rabbit);
    }

    @Test void rollbackLeavesSlotAvailableAndObserverSendsNothing() {
        long before = appointments.count();
        long beforeOutbox = outbox.count();
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            bookings.createBooking(patient.getUserId(), new CreateBookingDto(slot.getId(), "Rollback"));
            tx.setRollbackOnly();
        });
        assertEquals(before, appointments.count());
        assertEquals(beforeOutbox, outbox.count());
        assertTrue(schedules.findById(slot.getId()).orElseThrow().getIsAvailable());
        verify(rabbit, after(400).never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test void concurrentBookingsHaveExactlyOneWinner() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> book = () -> {
            start.await();
            try {
                bookings.createBooking(patient.getUserId(), new CreateBookingDto(slot.getId(), "Concurrent"));
                return true;
            } catch (BookingConflictException expected) { return false; }
        };
        try {
            Future<Boolean> first = executor.submit(book);
            Future<Boolean> second = executor.submit(book);
            start.countDown();
            int winners = (first.get(10, TimeUnit.SECONDS) ? 1 : 0) + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, winners);
            assertEquals(1, appointments.findAll().stream().filter(a -> a.getSchedule().getId().equals(slot.getId())).count());
            assertTrue(outbox.findByEventKey("BOOKING_CONFIRMED:" + appointments.findAll().stream()
                    .filter(a -> a.getSchedule().getId().equals(slot.getId())).findFirst().orElseThrow().getBookingCode()).isPresent());
        } finally { executor.shutdownNow(); }
    }

    @Test void cancelPreservesOldAppointmentAndReplacementCanBeBooked() {
        var booking = bookings.createBooking(patient.getUserId(), new CreateBookingDto(slot.getId(), "Original"));
        bookings.cancelBooking(booking.getId(), patient.getUserId());
        assertEquals("CANCELLED", appointments.findById(booking.getId()).orElseThrow().getStatus());
        var replacement = schedules.findByDoctorUserIdAndWorkDateAndIsAvailableTrue(doctor.getUserId(), slot.getWorkDate());
        assertEquals(1, replacement.size());
        assertNotEquals(slot.getId(), replacement.get(0).getId());
        var rebooked = bookings.createBooking(patient.getUserId(), new CreateBookingDto(replacement.get(0).getId(), "Replacement"));
        assertNotEquals(booking.getId(), rebooked.getId());
    }

    @Test void scheduleUpdateWaitsForConcurrentDoctorScheduleChanges() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch started = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Future<Boolean>> update = new java.util.concurrent.atomic.AtomicReference<>();
        LocalDate targetDate = slot.getWorkDate().plusDays(1);
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                Doctor lockedDoctor = doctors.findByIdForUpdate(doctor.getUserId()).orElseThrow();
                update.set(executor.submit(() -> {
                    started.countDown();
                    try {
                        doctorSchedules.updateSlot(doctor.getUserId(), slot.getId(), CreateScheduleDto.builder()
                                .workDate(targetDate).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(10, 30)).build());
                        return true;
                    } catch (AppException expected) { return false; }
                }));
                try {
                    assertTrue(started.await(2, TimeUnit.SECONDS));
                    // Without the shared doctor lock, the update commits before this transaction adds its slot.
                    try { update.get().get(400, TimeUnit.MILLISECONDS); }
                    catch (TimeoutException expected) { /* correctly waits for the doctor's lock */ }
                    catch (Exception e) { throw new RuntimeException(e); }
                } catch (InterruptedException e) { throw new RuntimeException(e); }
                schedules.save(Schedule.builder().doctor(lockedDoctor).workDate(targetDate)
                        .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(10, 30)).isAvailable(true).build());
            });
            assertFalse(update.get().get(5, TimeUnit.SECONDS), "Overlapping update must be rejected after the other change commits");
            assertEquals(slot.getWorkDate(), schedules.findById(slot.getId()).orElseThrow().getWorkDate());
        } finally { executor.shutdownNow(); }
    }

    @Test void reminderIsCreatedOnceAndCancelledBookingsAreSkipped() {
        var booked = bookings.createBooking(patient.getUserId(), new CreateBookingDto(slot.getId(), "Reminder"));
        LocalDateTime scanTime = LocalDateTime.of(slot.getWorkDate().minusDays(1), slot.getStartTime()).plusMinutes(1);
        assertEquals(1, reminders.enqueueDueReminders(scanTime));
        assertEquals(0, reminders.enqueueDueReminders(scanTime.plusMinutes(1)));
        assertTrue(outbox.existsByEventKey("APPOINTMENT_REMINDER:" + booked.getBookingCode()));
        bookings.cancelBooking(booked.getId(), patient.getUserId());
        assertEquals(0, reminders.enqueueDueReminders(scanTime));
    }

    @Test void reminderQueuedBeforeCancellationDoesNotSendAfterCancellation() {
        var booked = bookings.createBooking(patient.getUserId(), new CreateBookingDto(slot.getId(), "Reminder cancellation"));
        LocalDateTime scanTime = LocalDateTime.of(slot.getWorkDate().minusDays(1), slot.getStartTime()).plusMinutes(1);
        assertEquals(1, reminders.enqueueDueReminders(scanTime));
        var reminder = outbox.findByEventKey("APPOINTMENT_REMINDER:" + booked.getBookingCode()).orElseThrow();
        bookings.cancelBooking(booked.getId(), patient.getUserId());
        delivery.deliver(reminder.getId(), reminder.getKind(), reminder.getPayload());
        verifyNoInteractions(sender);
    }

    @Test void unauthorizedMedicalReadIsAuditedWithRequestingActor() {
        slot.setWorkDate(LocalDate.now().minusDays(1)); slot.setIsAvailable(false); schedules.save(slot);
        Appointment appointment = appointments.save(Appointment.builder().bookingCode("BK-" + UUID.randomUUID())
                .patient(patient).doctor(doctor).schedule(slot).price(doctor.getConsultationFee()).status("CONFIRMED").build());
        assertThrows(AppException.class, () -> medicalRecords.getMedicalRecord(patient.getUserId(), appointment.getId()));
        var entries = auditEntries.findByAppointmentIdOrderByIdAsc(appointment.getId());
        assertEquals(1, entries.size());
        assertEquals(patient.getUserId(), entries.getFirst().getActorUserId());
        assertEquals("READ_DENIED", entries.getFirst().getAction());
    }

    @Test void medicalRecordCompletesAppointmentAndIsUpdatedRatherThanDuplicated() {
        slot.setWorkDate(LocalDate.now().minusDays(1));
        slot.setIsAvailable(false);
        schedules.save(slot);
        Appointment appointment = appointments.save(Appointment.builder().bookingCode("BK-" + UUID.randomUUID())
                .patient(patient).doctor(doctor).schedule(slot).price(doctor.getConsultationFee()).status("CONFIRMED").build());
        medicalRecords.updateMedicalRecord(doctor.getUserId(), UpdateMedicalRecordDto.builder()
                .appointmentId(appointment.getId()).diagnosis("Ghi nhận thử nghiệm").doctorNotes("Dữ liệu kiểm thử").build());
        medicalRecords.updateMedicalRecord(doctor.getUserId(), UpdateMedicalRecordDto.builder()
                .appointmentId(appointment.getId()).diagnosis("Nội dung cập nhật").build());
        assertEquals("COMPLETED", appointments.findById(appointment.getId()).orElseThrow().getStatus());
        assertEquals("Nội dung cập nhật", records.findByAppointmentId(appointment.getId()).orElseThrow().getDiagnosis());
        assertEquals("Nội dung cập nhật", medicalRecords.getMedicalRecord(doctor.getUserId(), appointment.getId()).getDiagnosis());
        Long recordId = records.findByAppointmentId(appointment.getId()).orElseThrow().getId();
        var history = revisions.findByMedicalRecordIdOrderByIdAsc(recordId);
        assertEquals(2, history.size());
        assertNull(history.getFirst().getBeforeDiagnosis());
        assertEquals("Ghi nhận thử nghiệm", history.get(1).getBeforeDiagnosis());
        assertEquals("Nội dung cập nhật", history.get(1).getAfterDiagnosis());
        var entries = auditEntries.findByAppointmentIdOrderByIdAsc(appointment.getId());
        assertEquals(java.util.List.of("WRITE", "WRITE", "READ"), entries.stream().map(MedicalAuditEntry::getAction).toList());
        assertTrue(entries.stream().allMatch(entry -> entry.getActorUserId().equals(doctor.getUserId())));
    }
}
