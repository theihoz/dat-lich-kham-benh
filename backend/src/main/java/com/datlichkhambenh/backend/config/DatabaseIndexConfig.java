package com.datlichkhambenh.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseIndexConfig {

    private final DataSource dataSource;

    @EventListener(ApplicationReadyEvent.class)
    public void createIndexes() {
        String[] indexes = {
            "CREATE INDEX IF NOT EXISTS idx_appointments_patient_id ON appointments(patient_user_id)",
            "CREATE INDEX IF NOT EXISTS idx_appointments_doctor_id ON appointments(doctor_user_id)",
            "CREATE INDEX IF NOT EXISTS idx_appointments_schedule_id ON appointments(schedule_id)",
            "CREATE INDEX IF NOT EXISTS idx_appointments_status ON appointments(status)",
            "CREATE INDEX IF NOT EXISTS idx_appointments_created_at ON appointments(created_at)",
            "CREATE INDEX IF NOT EXISTS idx_schedules_doctor_id ON schedules(doctor_user_id)",
            "CREATE INDEX IF NOT EXISTS idx_schedules_work_date ON schedules(work_date)",
            "CREATE INDEX IF NOT EXISTS idx_schedules_available ON schedules(is_available)",
            "CREATE INDEX IF NOT EXISTS idx_doctors_specialty_id ON doctors(specialty_id)",
            "CREATE INDEX IF NOT EXISTS idx_doctors_approved ON doctors(is_approved)",
            "CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)",
            "CREATE INDEX IF NOT EXISTS idx_users_role ON users(role)",
            "CREATE INDEX IF NOT EXISTS idx_medical_records_appointment ON medical_records(appointment_id)"
        };
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : indexes) {
                try {
                    stmt.executeUpdate(sql);
                } catch (Exception e) {
                    log.debug("Index may already exist: {}", e.getMessage());
                }
            }
            log.info("Database indexes verified/created");
        } catch (Exception e) {
            log.warn("Could not create database indexes: {}", e.getMessage());
        }
    }
}
