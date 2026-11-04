package com.datlichkhambenh.backend;

import com.datlichkhambenh.backend.config.DatabaseIndexConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseIndexTests {
    @Autowired DataSource dataSource;
    @Autowired DatabaseIndexConfig indexes;

    @Test
    void relationshipIndexesExistAndInitializationIsRepeatable() throws Exception {
        indexes.createIndexes();
        indexes.createIndexes();
        try (Connection connection = dataSource.getConnection()) {
            assertIndexes(connection, "APPOINTMENTS", "idx_appointments_patient_id", "idx_appointments_doctor_id");
            assertIndexes(connection, "SCHEDULES", "idx_schedules_doctor_id");
        }
    }

    private void assertIndexes(Connection connection, String table, String... expected) throws Exception {
        Set<String> names = new HashSet<>();
        try (var rows = connection.getMetaData().getIndexInfo(connection.getCatalog(), null, table, false, false)) {
            while (rows.next()) {
                String name = rows.getString("INDEX_NAME");
                if (name != null) names.add(name.toLowerCase(java.util.Locale.ROOT));
            }
        }
        for (String name : expected) assertTrue(names.contains(name), "Missing database index " + name);
    }
}
