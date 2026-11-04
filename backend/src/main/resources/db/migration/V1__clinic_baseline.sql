CREATE TABLE users (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, email VARCHAR(255) NOT NULL,
 password_hash VARCHAR(255) NOT NULL, role VARCHAR(20) NOT NULL,
 is_active BOOLEAN NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6),
 CONSTRAINT uk_users_email UNIQUE(email)
);
CREATE TABLE specialties (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL, description TEXT);
CREATE TABLE doctors (
 user_id BIGINT PRIMARY KEY, specialty_id BIGINT NOT NULL, full_name VARCHAR(255) NOT NULL,
 phone VARCHAR(20), degree VARCHAR(100), experience_years INTEGER, consultation_fee DECIMAL(12,2), is_approved BOOLEAN NOT NULL,
 CONSTRAINT uk_doctors_phone UNIQUE(phone), CONSTRAINT fk_doctors_user FOREIGN KEY(user_id) REFERENCES users(id),
 CONSTRAINT fk_doctors_specialty FOREIGN KEY(specialty_id) REFERENCES specialties(id)
);
CREATE TABLE patients (
 user_id BIGINT PRIMARY KEY, full_name VARCHAR(255) NOT NULL, dob DATE, gender VARCHAR(10), phone VARCHAR(20), address TEXT, medical_history TEXT,
 CONSTRAINT uk_patients_phone UNIQUE(phone), CONSTRAINT fk_patients_user FOREIGN KEY(user_id) REFERENCES users(id)
);
CREATE TABLE schedules (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, doctor_id BIGINT NOT NULL, work_date DATE NOT NULL, start_time TIME(6) NOT NULL, end_time TIME(6) NOT NULL, is_available BOOLEAN NOT NULL,
 CONSTRAINT fk_schedules_doctor FOREIGN KEY(doctor_id) REFERENCES doctors(user_id)
);
CREATE TABLE appointments (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, booking_code VARCHAR(100) NOT NULL, patient_id BIGINT NOT NULL, doctor_id BIGINT NOT NULL,
 schedule_id BIGINT NOT NULL, symptoms TEXT, status VARCHAR(20) NOT NULL, price DECIMAL(12,2), created_at DATETIME(6) NOT NULL,
 CONSTRAINT uk_appointments_booking_code UNIQUE(booking_code), CONSTRAINT uk_appointments_schedule UNIQUE(schedule_id),
 CONSTRAINT fk_appointments_patient FOREIGN KEY(patient_id) REFERENCES patients(user_id),
 CONSTRAINT fk_appointments_doctor FOREIGN KEY(doctor_id) REFERENCES doctors(user_id),
 CONSTRAINT fk_appointments_schedule FOREIGN KEY(schedule_id) REFERENCES schedules(id)
);
CREATE TABLE medical_records (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, appointment_id BIGINT NOT NULL, diagnosis TEXT, doctor_notes TEXT, created_at DATETIME(6) NOT NULL,
 CONSTRAINT uk_medical_records_appointment UNIQUE(appointment_id), CONSTRAINT fk_medical_records_appointment FOREIGN KEY(appointment_id) REFERENCES appointments(id)
);
CREATE TABLE medical_services (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(30) NOT NULL UNIQUE, name VARCHAR(150) NOT NULL, description TEXT,
 specialty_id BIGINT, default_price DECIMAL(12,2) NOT NULL, duration_minutes INTEGER NOT NULL, active BOOLEAN NOT NULL,
 CONSTRAINT fk_services_specialty FOREIGN KEY(specialty_id) REFERENCES specialties(id)
);
CREATE INDEX idx_appointments_patient_id ON appointments(patient_id);
CREATE INDEX idx_appointments_doctor_id ON appointments(doctor_id);
CREATE INDEX idx_appointments_status ON appointments(status);
CREATE INDEX idx_appointments_created_at ON appointments(created_at);
CREATE INDEX idx_schedules_doctor_date ON schedules(doctor_id,work_date,start_time,end_time);
CREATE INDEX idx_schedules_available ON schedules(is_available);
CREATE INDEX idx_users_role ON users(role);
