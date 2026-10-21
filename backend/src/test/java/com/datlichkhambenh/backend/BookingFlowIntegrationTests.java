package com.datlichkhambenh.backend;

import com.datlichkhambenh.backend.dto.request.CreateBookingDto;
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
    @Autowired BookingService bookings;
    @Autowired MedicalRecordService medicalRecords;
    @Autowired PlatformTransactionManager transactionManager;
    @MockBean RabbitTemplate rabbit;
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
        slot = schedules.save(Schedule.builder().doctor(doctor).workDate(LocalDate.now().plusDays(4))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(9, 30)).isAvailable(true).build());
        });
        reset(rabbit);
    }

    @Test void bookingCommitsThenObserverQueuesImmutableNotification() {
        var booking = bookings.createBooking(patient.getUserId(), new CreateBookingDto(slot.getId(), "Khám định kỳ"));
        assertNotNull(booking.getId());
        assertEquals("CONFIRMED", appointments.findById(booking.getId()).orElseThrow().getStatus());
        assertFalse(schedules.findById(slot.getId()).orElseThrow().getIsAvailable());
        ArgumentCaptor<EmailNotificationMessage> event = ArgumentCaptor.forClass(EmailNotificationMessage.class);
        verify(rabbit, timeout(3000)).convertAndSend(eq("clinic.email.exchange"), eq("clinic.email.send"), event.capture());
        assertEquals(booking.getBookingCode(), event.getValue().bookingCode());
        assertEquals("BOOKING_CONFIRMED", event.getValue().type());
    }

    @Test void rollbackLeavesSlotAvailableAndObserverSendsNothing() {
        long before = appointments.count();
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            bookings.createBooking(patient.getUserId(), new CreateBookingDto(slot.getId(), "Rollback"));
            tx.setRollbackOnly();
        });
        assertEquals(before, appointments.count());
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
            verify(rabbit, timeout(3000)).convertAndSend(anyString(), anyString(), any(Object.class));
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
    }
}
