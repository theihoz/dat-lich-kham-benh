package com.datlichkhambenh.backend.pattern;

import com.datlichkhambenh.backend.dto.response.DoctorDto;
import com.datlichkhambenh.backend.entity.*;
import com.datlichkhambenh.backend.exception.AppException;
import com.datlichkhambenh.backend.messaging.*;
import com.datlichkhambenh.backend.pattern.notification.*;
import com.datlichkhambenh.backend.pattern.sorting.*;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DesignPatternTests {
    private final NotificationCreatorRegistry creators = new NotificationCreatorRegistry(List.of(
            new ConfirmationNotificationCreator(), new CancellationNotificationCreator(),
            new ReminderNotificationCreator()));
    private final DoctorSortRegistry sorts = new DoctorSortRegistry(List.of(new NameSortStrategy(),
            new FeeAscendingSortStrategy(), new FeeDescendingSortStrategy(), new ExperienceSortStrategy()));

    @Test void builderAssemblesBookingWithoutPositionalConstructor() {
        Patient patient = Patient.builder().userId(1L).build();
        Doctor doctor = Doctor.builder().userId(2L).consultationFee(new BigDecimal("300000")).build();
        Schedule schedule = Schedule.builder().id(3L).doctor(doctor).build();
        Appointment booking = Appointment.builder().bookingCode("BK-BUILDER").patient(patient)
                .doctor(doctor).schedule(schedule).symptoms("Khám định kỳ")
                .status("CONFIRMED").price(doctor.getConsultationFee()).build();
        assertSame(patient, booking.getPatient());
        assertSame(schedule, booking.getSchedule());
        assertEquals(new BigDecimal("300000"), booking.getPrice());
        assertEquals("CONFIRMED", booking.getStatus());
    }

    @Test void factoryMethodCreatesThreeDifferentMessagesAndRejectsUnknownType() {
        NotificationContent confirmed = creators.get("BOOKING_CONFIRMED").createMessage(message("BOOKING_CONFIRMED"));
        NotificationContent cancelled = creators.get("BOOKING_CANCELLED").createMessage(message("BOOKING_CANCELLED"));
        NotificationContent reminder = creators.get("APPOINTMENT_REMINDER").createMessage(message("APPOINTMENT_REMINDER"));
        assertInstanceOf(ConfirmationContent.class, confirmed);
        assertInstanceOf(CancellationContent.class, cancelled);
        assertInstanceOf(ReminderContent.class, reminder);
        assertTrue(confirmed.body().contains("đặt lịch khám thành công"));
        assertTrue(cancelled.body().contains("đã được hủy"));
        assertTrue(reminder.subject().contains("Nhắc lịch"));
        assertTrue(confirmed.body().contains("BK-TEST"));
        assertThrows(IllegalArgumentException.class, () -> creators.get("UNKNOWN"));
    }

    @Test void adapterTranslatesDomainContentToJavaMail() {
        JavaMailSender javaMail = mock(JavaMailSender.class);
        NotificationSender adapter = new SmtpNotificationAdapter(javaMail);
        NotificationContent content = creators.get("BOOKING_CONFIRMED").createMessage(message("BOOKING_CONFIRMED"));
        adapter.send("patient@example.test", content);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMail).send(captor.capture());
        assertArrayEquals(new String[]{"patient@example.test"}, captor.getValue().getTo());
        assertEquals(content.subject(), captor.getValue().getSubject());
        assertEquals(content.body(), captor.getValue().getText());
    }

    @Test void queueConsumerComposesFactoryMethodAndAdapter() {
        NotificationSender sender = mock(NotificationSender.class);
        new EmailQueueConsumer(creators, sender).consume(message("BOOKING_CANCELLED"));
        ArgumentCaptor<NotificationContent> content = ArgumentCaptor.forClass(NotificationContent.class);
        verify(sender).send(eq("patient@example.test"), content.capture());
        assertTrue(content.getValue().body().contains("đã được hủy"));
    }

    @Test void strategiesChangeOrderWithoutChangingSearchService() {
        DoctorDto an = DoctorDto.builder().fullName("An").consultationFee(new BigDecimal("500000"))
                .experienceYears(3).build();
        DoctorDto binh = DoctorDto.builder().fullName("Binh").consultationFee(new BigDecimal("200000"))
                .experienceYears(12).build();
        List<DoctorDto> doctors = List.of(binh, an);
        assertSame(an, doctors.stream().sorted(sorts.get(null).comparator()).findFirst().orElseThrow());
        assertSame(binh, doctors.stream().sorted(sorts.get("feeAsc").comparator()).findFirst().orElseThrow());
        assertSame(an, doctors.stream().sorted(sorts.get("feeDesc").comparator()).findFirst().orElseThrow());
        assertSame(binh, doctors.stream().sorted(sorts.get("experience").comparator()).findFirst().orElseThrow());
        assertThrows(AppException.class, () -> sorts.get("unsupported"));
    }

    @Test void newStrategyCanBeRegisteredWithoutEditingRegistry() {
        DoctorSortStrategy custom = new DoctorSortStrategy() {
            public String key() { return "reverseName"; }
            public java.util.Comparator<DoctorDto> comparator() { return new NameSortStrategy().comparator().reversed(); }
        };
        assertSame(custom, new DoctorSortRegistry(List.of(custom)).get("reverseName"));
    }

    private EmailNotificationMessage message(String type) {
        return new EmailNotificationMessage(type, "patient@example.test", "Bệnh nhân demo", "BK-TEST",
                "Bác sĩ demo", "2026-10-24", "09:00", "09:30", "300000");
    }
}
