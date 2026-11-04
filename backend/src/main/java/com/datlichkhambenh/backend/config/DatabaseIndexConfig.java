package com.datlichkhambenh.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;

@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DatabaseIndexConfig {

    private final DataSource dataSource;

    @EventListener(ApplicationReadyEvent.class)
    public void createIndexes() {
        String[] indexes = {
            "CREATE INDEX idx_appointments_patient_id ON appointments(patient_id)",
            "CREATE INDEX idx_appointments_doctor_id ON appointments(doctor_id)",
            "CREATE INDEX idx_appointments_schedule_id ON appointments(schedule_id)",
            "CREATE INDEX idx_appointments_status ON appointments(status)",
            "CREATE INDEX idx_appointments_created_at ON appointments(created_at)",
            "CREATE INDEX idx_schedules_doctor_id ON schedules(doctor_id)",
            "CREATE INDEX idx_schedules_work_date ON schedules(work_date)",
            "CREATE INDEX idx_schedules_available ON schedules(is_available)",
            "CREATE INDEX idx_doctors_specialty_id ON doctors(specialty_id)",
            "CREATE INDEX idx_doctors_approved ON doctors(is_approved)",
            "CREATE INDEX idx_users_email ON users(email)",
            "CREATE INDEX idx_users_role ON users(role)",
            "CREATE INDEX idx_medical_records_appointment ON medical_records(appointment_id)"
        };
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : indexes) {
                // MySQL does not support CREATE INDEX IF NOT EXISTS. Check JDBC
                // metadata first and use portable DDL for both MySQL and H2.
                String ddl = sql;
                String name = ddl.split(" ")[2];
                String table = ddl.substring(ddl.indexOf(" ON ") + 4, ddl.indexOf('('));
                try {
                    if (!indexExists(conn, table, name)) stmt.executeUpdate(ddl);
                } catch (Exception e) {
                    log.warn("Could not create index {}: {}", name, e.getMessage());
                }
            }
            log.info("Database indexes verified/created");
        } catch (Exception e) {
            log.warn("Could not create database indexes: {}", e.getMessage());
        }
    }

    private boolean indexExists(Connection connection, String table, String index) throws java.sql.SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String tableName = metadata.storesUpperCaseIdentifiers() ? table.toUpperCase(Locale.ROOT) : table;
        try (ResultSet rows = metadata.getIndexInfo(connection.getCatalog(), null, tableName, false, false)) {
            while (rows.next()) {
                if (index.equalsIgnoreCase(rows.getString("INDEX_NAME"))) return true;
            }
        }
        return false;
    }
}
