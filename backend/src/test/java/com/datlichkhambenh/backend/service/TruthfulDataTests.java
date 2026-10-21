package com.datlichkhambenh.backend.service;

import com.datlichkhambenh.backend.dto.response.MedicalRecordDto;
import com.datlichkhambenh.backend.entity.Appointment;
import com.datlichkhambenh.backend.entity.Doctor;
import com.datlichkhambenh.backend.entity.MedicalRecord;
import com.datlichkhambenh.backend.entity.Patient;
import com.datlichkhambenh.backend.entity.Schedule;
import com.datlichkhambenh.backend.entity.Specialty;
import com.datlichkhambenh.backend.repository.*;
import com.datlichkhambenh.backend.service.impl.AdminManagementServiceImpl;
import com.datlichkhambenh.backend.service.impl.PatientServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TruthfulDataTests {
    @Mock PatientRepository patientRepository;
    @Mock MedicalRecordRepository medicalRecordRepository;
    @Mock DoctorRepository doctorRepository;
    @Mock SpecialtyRepository specialtyRepository;
    @Mock AppointmentRepository appointmentRepository;
    @Mock RedisCacheService redisCacheService;
    @Mock UserRepository userRepository;
    @Mock ScheduleRepository scheduleRepository;
    @Mock DoctorService doctorService;
    @Mock PatientService patientService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock MedicalServiceRepository medicalServiceRepository;
    @InjectMocks PatientServiceImpl patientServiceImpl;
    @InjectMocks AdminManagementServiceImpl adminService;

    @Test
    void patientWithoutRecordsReceivesAnEmptyList() {
        when(medicalRecordRepository.findByAppointmentPatientUserId(7L)).thenReturn(List.of());

        assertTrue(patientServiceImpl.getPatientMedicalRecords(7L).isEmpty());
        verify(medicalRecordRepository).findByAppointmentPatientUserId(7L);
        verifyNoMoreInteractions(medicalRecordRepository);
        verifyNoInteractions(patientRepository, doctorRepository);
    }

    @Test
    void patientReceivesOnlyStoredRecordsNewestFirstWithoutInventedDiagnoses() {
        Appointment visit = appointment(42L, 2L, "2026-01-10", "COMPLETED", "120000.10");
        MedicalRecord older = MedicalRecord.builder().id(3L).appointment(visit).diagnosis("Stored diagnosis")
                .doctorNotes("Stored instructions").createdAt(LocalDateTime.of(2026, 1, 10, 10, 0)).build();
        MedicalRecord newer = MedicalRecord.builder().id(4L).appointment(visit).diagnosis("Updated diagnosis")
                .doctorNotes(null).createdAt(LocalDateTime.of(2026, 1, 11, 10, 0)).build();
        when(medicalRecordRepository.findByAppointmentPatientUserId(7L)).thenReturn(List.of(older, newer));

        List<MedicalRecordDto> records = patientServiceImpl.getPatientMedicalRecords(7L);

        assertEquals(List.of(4L, 3L), records.stream().map(MedicalRecordDto::getId).toList());
        assertEquals(42L, records.get(0).getAppointmentId());
        assertEquals("BOOK-42", records.get(0).getBookingCode());
        assertEquals("Patient 7", records.get(0).getPatientName());
        assertEquals("Doctor 2", records.get(0).getDoctorName());
        assertEquals("Updated diagnosis", records.get(0).getDiagnosis());
        assertNull(records.get(0).getDoctorNotes());
        assertEquals("Stored instructions", records.get(1).getDoctorNotes());
        verifyNoInteractions(patientRepository, doctorRepository);
    }

    @Test
    void emptySystemStatsContainNoOffsets() {
        when(appointmentRepository.findAll()).thenReturn(List.of());

        Map<String, Object> stats = adminService.getSystemStats();

        for (String key : List.of("totalDoctors", "totalPatients", "totalAppointments", "totalSpecialties",
                "cancelledAppointments", "cancellationRate")) {
            assertEquals(0.0, ((Number) stats.get(key)).doubleValue(), key);
        }
        assertMoney("0", stats.get("totalRevenue"));
        assertTrue(((List<?>) stats.get("topDoctors")).isEmpty());
    }

    @Test
    void systemStatsUseRepositoryCountsAndOnlyCompletedFees() {
        when(doctorRepository.count()).thenReturn(2L);
        when(patientRepository.count()).thenReturn(7L);
        when(specialtyRepository.count()).thenReturn(1L);
        when(appointmentRepository.count()).thenReturn(4L);
        when(appointmentRepository.countByStatus("CANCELLED")).thenReturn(1L);
        when(appointmentRepository.findAll()).thenReturn(List.of(
                appointment(1L, 2L, "2026-01-10", "COMPLETED", "120000.10"),
                appointment(2L, 2L, "2026-01-11", "COMPLETED", "80000.20"),
                appointment(3L, 3L, "2026-02-10", "CONFIRMED", "999999"),
                appointment(4L, 3L, "2026-02-11", "CANCELLED", "999999")));

        Map<String, Object> stats = adminService.getSystemStats();

        assertEquals(2L, stats.get("totalDoctors"));
        assertEquals(7L, stats.get("totalPatients"));
        assertEquals(4L, stats.get("totalAppointments"));
        assertEquals(1L, stats.get("totalSpecialties"));
        assertEquals(1L, stats.get("cancelledAppointments"));
        assertEquals(25.0, stats.get("cancellationRate"));
        assertMoney("200000.30", stats.get("totalRevenue"));
    }

    @Test
    void emptyAnalyticsHaveTwelveZeroMonthsAndNoInventedRows() {
        when(appointmentRepository.findAll()).thenReturn(List.of());

        Map<String, Object> analytics = adminService.getAnalytics(2026);

        assertEquals(0L, analytics.get("yearAppointments"));
        assertEquals(0L, analytics.get("yearCompletedAppointments"));
        assertEquals(0, analytics.get("totalTransactions"));
        assertMoney("0", analytics.get("yearRevenue"));
        assertMoney("0", analytics.get("avgPerAppointment"));
        assertEquals(Map.of("CONFIRMED", 0L, "COMPLETED", 0L, "CANCELLED", 0L), analytics.get("statusCounts"));
        for (String key : List.of("transactions", "doctorRankings", "dailyRevenue")) {
            assertTrue(rows(analytics, key).isEmpty(), key);
        }
        List<Map<String, Object>> months = rows(analytics, "months");
        assertEquals(12, months.size());
        for (int i = 0; i < months.size(); i++) {
            Map<String, Object> month = months.get(i);
            assertEquals(i + 1, month.get("month"));
            assertEquals(0, month.get("appointments"));
            assertEquals(0L, month.get("completed"));
            assertEquals(0L, month.get("cancelled"));
            assertMoney("0", month.get("revenue"));
            assertNull(month.get("growthPercent"));
        }
        assertEquals(false, analytics.get("paymentVerified"));
    }

    @Test
    void analyticsTotalsMonthlyDailyAndRankingsAgreeOnActualCompletedFees() {
        when(appointmentRepository.findAll()).thenReturn(List.of(
                appointment(1L, 2L, "2026-01-10", "COMPLETED", "120000.10"),
                appointment(2L, 2L, "2026-01-10", "COMPLETED", "80000.20"),
                appointment(3L, 3L, "2026-02-10", "COMPLETED", "300000.30"),
                appointment(4L, 2L, "2026-02-11", "CONFIRMED", "999999"),
                appointment(5L, 3L, "2026-02-12", "CANCELLED", "999999"),
                appointment(6L, 2L, "2025-12-31", "COMPLETED", "9000000")));

        Map<String, Object> analytics = adminService.getAnalytics(2026);

        assertEquals(5L, analytics.get("yearAppointments"));
        assertEquals(3L, analytics.get("yearCompletedAppointments"));
        assertEquals(3, analytics.get("totalTransactions"));
        assertMoney("500000.60", analytics.get("yearRevenue"));
        assertMoney("166666.87", analytics.get("avgPerAppointment"));
        assertEquals(Map.of("CONFIRMED", 1L, "COMPLETED", 3L, "CANCELLED", 1L), analytics.get("statusCounts"));
        List<Map<String, Object>> months = rows(analytics, "months");
        assertEquals(2, months.get(0).get("appointments"));
        assertEquals(3, months.get(1).get("appointments"));
        assertMoney("200000.30", months.get(0).get("revenue"));
        assertMoney("300000.30", months.get(1).get("revenue"));
        assertMoney("50.0", months.get(1).get("growthPercent"));
        assertMoney("500000.60", sum(rows(analytics, "months"), "revenue"));
        assertMoney("500000.60", sum(rows(analytics, "dailyRevenue"), "revenue"));
        assertMoney("500000.60", sum(rows(analytics, "doctorRankings"), "revenue"));
        assertEquals(3L, rows(analytics, "doctorRankings").get(0).get("doctorId"));
        assertEquals(2, rows(analytics, "doctorRankings").get(0).get("appointments"));
        assertEquals(1L, rows(analytics, "doctorRankings").get(0).get("completed"));
        List<Map<String, Object>> completedRows = rows(analytics, "transactions");
        assertEquals(List.of(3L, 1L, 2L), completedRows.stream().map(row -> row.get("appointmentId")).toList());
        assertMoney("500000.60", sum(completedRows, "amount"));
        assertEquals("BOOK-3", completedRows.get(0).get("bookingCode"));
        assertEquals(LocalDate.of(2026, 2, 10), completedRows.get(0).get("workDate"));
        assertEquals("Patient 7", completedRows.get(0).get("patientName"));
        assertEquals("Doctor 3", completedRows.get(0).get("doctorName"));
        for (Map<String, Object> row : completedRows) {
            assertEquals("COMPLETED", row.get("status"));
            assertFalse(row.containsKey("transactionId"));
            assertFalse(row.containsKey("paymentMethod"));
            assertFalse(row.containsKey("paymentDate"));
        }
        assertEquals("COMPLETED_APPOINTMENT_FEES", analytics.get("revenueBasis"));
        assertEquals(false, analytics.get("paymentVerified"));
    }

    @Test
    void missingPricesRemainUnknownAndNonCompletedAppointmentsEarnNothing() {
        when(appointmentRepository.findAll()).thenReturn(List.of(
                appointment(1L, 2L, "2026-01-10", "COMPLETED", null),
                appointment(2L, 2L, "2026-01-11", "CONFIRMED", "500000"),
                appointment(3L, 2L, "2026-01-12", "CANCELLED", "600000")));

        Map<String, Object> analytics = adminService.getAnalytics(2026);

        assertMoney("0", analytics.get("yearRevenue"));
        assertMoney("0", analytics.get("avgPerAppointment"));
        assertEquals(1L, analytics.get("completedWithoutPrice"));
        assertEquals(1, rows(analytics, "transactions").size());
        assertNull(rows(analytics, "transactions").get(0).get("amount"));
        assertMoney("0", rows(analytics, "doctorRankings").get(0).get("revenue"));
        assertTrue(rows(analytics, "dailyRevenue").isEmpty());
    }

    private Appointment appointment(long id, long doctorId, String date, String status, String price) {
        Specialty specialty = Specialty.builder().id(1L).name("Specialty").build();
        Doctor doctor = Doctor.builder().userId(doctorId).fullName("Doctor " + doctorId)
                .specialty(specialty).build();
        Patient patient = Patient.builder().userId(7L).fullName("Patient 7").phone("0900000007").build();
        Schedule schedule = Schedule.builder().id(id).doctor(doctor).workDate(LocalDate.parse(date))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(9, 30)).build();
        return Appointment.builder().id(id).bookingCode("BOOK-" + id).doctor(doctor).patient(patient)
                .schedule(schedule).status(status).price(price == null ? null : new BigDecimal(price)).build();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rows(Map<String, Object> analytics, String key) {
        return (List<Map<String, Object>>) analytics.get(key);
    }

    private BigDecimal sum(List<Map<String, Object>> rows, String key) {
        return rows.stream().map(row -> (BigDecimal) row.get(key)).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void assertMoney(String expected, Object actual) {
        assertInstanceOf(BigDecimal.class, actual);
        assertEquals(0, new BigDecimal(expected).compareTo((BigDecimal) actual), "Expected " + expected + ", got " + actual);
    }
}
