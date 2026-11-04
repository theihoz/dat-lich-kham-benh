package com.datlichkhambenh.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class ProductionSafetyTests {
    private MockEnvironment valid() {
        MockEnvironment env = new MockEnvironment(); env.setActiveProfiles("prod");
        env.withProperty("app.jwt.secret", "a-long-random-production-jwt-secret-over-32-characters")
           .withProperty("spring.datasource.url", "jdbc:mysql://db/clinic?sslMode=REQUIRED")
           .withProperty("spring.datasource.username", "clinic")
           .withProperty("spring.datasource.password", "long-database-password")
           .withProperty("spring.data.redis.password", "long-cache-password")
           .withProperty("spring.rabbitmq.username", "clinic")
           .withProperty("spring.rabbitmq.password", "long-broker-password")
           .withProperty("app.public-url", "https://clinic.example.org")
           .withProperty("spring.mail.host", "smtp.example.org")
           .withProperty("spring.mail.username", "clinic@example.org")
           .withProperty("spring.mail.password", "smtp-app-password")
           .withProperty("spring.jpa.hibernate.ddl-auto", "validate")
           .withProperty("app.metrics-token", "a-separate-metrics-only-token-at-least-32-characters")
           .withProperty("spring.flyway.enabled", "true");
        return env;
    }
    @Test void acceptsExplicitProductionConfiguration() {
        assertDoesNotThrow(() -> new ProductionSafetyEnvironment().postProcessEnvironment(valid(), new SpringApplication()));
    }
    @Test void refusesMissingOrWeakSecretsAndInsecureUrls() {
        for (String key : new String[]{"app.jwt.secret", "spring.datasource.password", "spring.data.redis.password", "spring.rabbitmq.password"}) {
            MockEnvironment env = valid().withProperty(key, "");
            assertThrows(IllegalStateException.class, () -> new ProductionSafetyEnvironment().postProcessEnvironment(env, new SpringApplication()), key);
        }
        assertThrows(IllegalStateException.class, () -> new ProductionSafetyEnvironment().postProcessEnvironment(valid().withProperty("app.public-url", "http://clinic.example.org"), new SpringApplication()));
    }
    @Test void refusesDemoOrSchemaMutationInProduction() {
        MockEnvironment mixed=valid(); mixed.setActiveProfiles("prod", "demo");
        assertThrows(IllegalStateException.class, () -> new ProductionSafetyEnvironment().postProcessEnvironment(mixed, new SpringApplication()));
        assertThrows(IllegalStateException.class, () -> new ProductionSafetyEnvironment().postProcessEnvironment(valid().withProperty("spring.jpa.hibernate.ddl-auto", "update"), new SpringApplication()));
    }
    @Test void leavesDemoUnchanged() {
        MockEnvironment env = new MockEnvironment(); env.setActiveProfiles("demo");
        assertDoesNotThrow(() -> new ProductionSafetyEnvironment().postProcessEnvironment(env, new SpringApplication()));
    }
}
