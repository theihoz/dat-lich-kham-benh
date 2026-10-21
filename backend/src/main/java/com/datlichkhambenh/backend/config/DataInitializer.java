package com.datlichkhambenh.backend.config;

import com.datlichkhambenh.backend.entity.*;
import com.datlichkhambenh.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Slf4j
@Component
@Profile({"dev", "demo"})
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;
    private final ScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalServiceRepository medicalServiceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("=== DataInitializer: Bắt đầu khởi tạo dữ liệu mẫu phòng khám ===");

        // 1. Chuyên khoa (8 Chuyên khoa)
        Specialty s1 = createSpecialtyIfNotExists("Tim mạch", "Chẩn đoán và điều trị các bệnh về tim, mạch máu, tăng huyết áp, suy tim");
        Specialty s2 = createSpecialtyIfNotExists("Thần kinh", "Điều trị các bệnh về não, tủy sống, rối loạn tiền đình, đau nửa đầu");
        Specialty s3 = createSpecialtyIfNotExists("Nhi khoa", "Chăm sóc sức khỏe và điều trị bệnh cho trẻ em từ sơ sinh đến 16 tuổi");
        Specialty s4 = createSpecialtyIfNotExists("Nội tổng quát", "Khám sức khỏe định kỳ, điều trị các bệnh lý nội khoa mãn tính");
        Specialty s5 = createSpecialtyIfNotExists("Da liễu", "Điều trị các bệnh về da, tóc, móng và tư vấn thẩm mỹ da liễu");
        Specialty s6 = createSpecialtyIfNotExists("Mắt", "Khám, đo thị lực và điều trị các bệnh lý nhãn khoa");
        Specialty s7 = createSpecialtyIfNotExists("Tai - Mũi - Họng", "Khám và điều trị các bệnh viêm xoang, viêm họng, viêm tai giữa");
        Specialty s8 = createSpecialtyIfNotExists("Cơ - Xương - Khớp", "Chẩn đoán và điều trị thoái hóa khớp, thoát vị đĩa đệm, chấn thương");
        createServiceIfMissing("KHAM-TQ", "Khám sức khỏe tổng quát", s4, 350000, 45);
        createServiceIfMissing("KHAM-TIM", "Khám chuyên khoa Tim mạch", s1, 400000, 30);
        createServiceIfMissing("KHAM-NHI", "Khám Nhi khoa", s3, 250000, 30);
        createServiceIfMissing("KHAM-DL", "Khám Da liễu", s5, 220000, 30);
        createServiceIfMissing("KHAM-MAT", "Khám và đo thị lực", s6, 280000, 40);

        // 2. Admin Users
        createAdminUser("admin@clinic.vn", "Admin@123");
        createAdminUser("admin@clinic", "admin@2026");

        // 3. Doctor Users & Profiles (8 Bác sĩ mặc định)
        Doctor doc1 = createDoctorUser("bs.nguyenvana@clinic.vn", "Doctor@123", "BS. Nguyễn Văn An", "0901234567", s1, "Tiến sĩ Y khoa - CKI Tim mạch", 15, BigDecimal.valueOf(350000), true);
        Doctor doc2 = createDoctorUser("bs.tranthib@clinic.vn", "Doctor@123", "BS. Trần Thị Bích", "0912345678", s3, "Thạc sĩ Y khoa - Nhi khoa", 10, BigDecimal.valueOf(250000), true);
        Doctor doc3 = createDoctorUser("bs.lephuongc@clinic.vn", "Doctor@123", "BS. Lê Phương Chi", "0923456789", s5, "Bác sĩ CKI Da liễu", 7, BigDecimal.valueOf(200000), true);
        Doctor doc4 = createDoctorUser("doctor@clinic", "doctor@2026", "BS. Hoàng Văn Cường", "0909999888", s2, "Phó Giáo giáo Y học - Thần kinh", 20, BigDecimal.valueOf(400000), true);
        Doctor doc5 = createDoctorUser("bs.phamminhd@clinic.vn", "Doctor@123", "BS. Phạm Minh Đức", "0933445566", s4, "Thạc sĩ Nội tổng quát", 12, BigDecimal.valueOf(280000), true);
        Doctor doc6 = createDoctorUser("bs.vothuyt@clinic.vn", "Doctor@123", "BS. Võ Thùy Trang", "0944556677", s6, "Bác sĩ CKI Nhãn khoa", 8, BigDecimal.valueOf(220000), true);
        Doctor doc7 = createDoctorUser("bs.dangquoch@clinic.vn", "Doctor@123", "BS. Đặng Quốc Hùng", "0955667788", s7, "Bác sĩ CKII Tai Mũi Họng", 14, BigDecimal.valueOf(300000), true);
        Doctor doc8 = createDoctorUser("bs.ngobichn@clinic.vn", "Doctor@123", "BS. Ngô Bích Ngọc", "0966778899", s8, "Bác sĩ Chuyên khoa Xương Khớp", 5, BigDecimal.valueOf(200000), false); // Chưa duyệt

        // Sinh tự động thêm 50 bác sĩ nữa
        Specialty[] specialties = {s1, s2, s3, s4, s5, s6, s7, s8};
        String[] ho = {"Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng", "Bùi", "Đỗ", "Hồ", "Ngô"};
        String[] tenDem = {"Văn", "Thị", "Minh", "Hồng", "Anh", "Đức", "Thành", "Ngọc", "Xuân", "Quốc", "Gia", "Khánh", "Nhật", "Hải"};
        String[] ten = {"An", "Bình", "Chương", "Dương", "Đạt", "Hùng", "Hải", "Khánh", "Linh", "Minh", "Nam", "Phong", "Sơn", "Trang", "Tùng", "Vinh", "Vy", "Yến"};

        for (int i = 1; i <= 50; i++) {
            String email = "doctor" + i + "@clinic.vn";
            String password = "Doctor@123";
            String docHo = ho[i % ho.length];
            String docDem = tenDem[(i * 3) % tenDem.length];
            String docTen = ten[(i * 7) % ten.length];
            String fullName = "BS. " + docHo + " " + docDem + " " + docTen;
            String phone = "09" + String.format("%08d", 10000000 + i);
            Specialty spec = specialties[i % specialties.length];
            String degree = (i % 3 == 0) ? "Tiến sĩ Y khoa - CKII" : ((i % 2 == 0) ? "Thạc sĩ Y khoa - CKI" : "Bác sĩ chuyên khoa");
            int expYears = 5 + (i % 20);
            BigDecimal fee = BigDecimal.valueOf(150000 + (i % 6) * 50000);
            createDoctorUser(email, password, fullName, phone, spec, degree, expYears, fee, true);
        }

        // 4. Patient Users & Profiles (5 Bệnh nhân mặc định)
        Patient pat1 = createPatientUser("bn.hoangminh@gmail.com", "Patient@123", "Hoàng Minh Đức", "0934567890", LocalDate.of(1990, 5, 15), "MALE", "123 Nguyễn Huệ, Quận 1, TP.HCM", "Tiền sử tăng huyết áp, dị ứng Penicillin");
        Patient pat2 = createPatientUser("bn.thuylinh@gmail.com", "Patient@123", "Nguyễn Thùy Linh", "0945678901", LocalDate.of(1995, 9, 22), "FEMALE", "456 Lê Lợi, Quận 3, TP.HCM", "Không có tiền sử đặc biệt");
        Patient pat3 = createPatientUser("patient@clinic", "user@2026", "Trần Thị Bình", "0911223344", LocalDate.of(1988, 3, 10), "FEMALE", "789 Điện Biên Phủ, Bình Thạnh, TP.HCM", "Tiền sử viêm dạ dày");
        Patient pat4 = createPatientUser("bn.tranvannam@gmail.com", "Patient@123", "Trần Văn Nam", "0977889900", LocalDate.of(2000, 11, 5), "MALE", "12 Lý Tự Trọng, Quận 1, TP.HCM", "Hen suyễn nhẹ");
        Patient pat5 = createPatientUser("bn.lethimai@gmail.com", "Patient@123", "Lê Thị Mai", "0988990011", LocalDate.of(1982, 1, 30), "FEMALE", "99 Võ Văn Tần, Quận 3, TP.HCM", "Tiểu đường tuýp 2");

        // Sinh tự động thêm 40 bệnh nhân nữa
        for (int i = 1; i <= 40; i++) {
            String email = "patient" + i + "@gmail.com";
            String password = "Patient@123";
            String patHo = ho[(i + 5) % ho.length];
            String patDem = tenDem[(i * 5) % tenDem.length];
            String patTen = ten[(i * 9) % ten.length];
            String fullName = patHo + " " + patDem + " " + patTen;
            String phone = "08" + String.format("%08d", 20000000 + i);
            LocalDate dob = LocalDate.of(1960 + (i % 45), 1 + (i % 12), 1 + (i % 28));
            String gender = (i % 2 == 0) ? "MALE" : "FEMALE";
            String address = (10 + i) + " Đường số " + (i % 10 + 1) + ", TP.HCM";
            String medicalHistory = (i % 4 == 0) ? "Tiền sử dị ứng thuốc" : ((i % 5 == 0) ? "Tiền sử huyết áp cao" : "Không có tiền sử bệnh lý đặc biệt");
            createPatientUser(email, password, fullName, phone, dob, gender, address, medicalHistory);
        }

        // 5. Schedules (Ca làm việc / Slot khám)
        if (scheduleRepository.count() == 0) {
            LocalDate today = LocalDate.now();

            // BS. Nguyễn Văn An (Tim mạch)
            Schedule sc1 = createSchedule(doc1, today.plusDays(1), LocalTime.of(8, 0), LocalTime.of(8, 30), false);
            Schedule sc2 = createSchedule(doc1, today.plusDays(1), LocalTime.of(8, 30), LocalTime.of(9, 0), true);
            Schedule sc3 = createSchedule(doc1, today.plusDays(1), LocalTime.of(9, 0), LocalTime.of(9, 30), true);
            Schedule sc4 = createSchedule(doc1, today.plusDays(2), LocalTime.of(14, 0), LocalTime.of(14, 30), true);
            Schedule sc5 = createSchedule(doc1, today.plusDays(2), LocalTime.of(14, 30), LocalTime.of(15, 0), true);

            // BS. Trần Thị Bích (Nhi khoa)
            // Ca đã khám phải nằm trong quá khứ, nhất quán với bệnh án mẫu.
            Schedule sc6 = createSchedule(doc2, today.minusDays(3), LocalTime.of(9, 0), LocalTime.of(9, 30), false);
            Schedule sc7 = createSchedule(doc2, today.plusDays(1), LocalTime.of(9, 30), LocalTime.of(10, 0), true);
            Schedule sc8 = createSchedule(doc2, today.plusDays(2), LocalTime.of(10, 0), LocalTime.of(10, 30), true);

            // BS. Lê Phương Chi (Da liễu)
            Schedule sc9 = createSchedule(doc3, today.plusDays(1), LocalTime.of(13, 30), LocalTime.of(14, 0), true);
            Schedule sc10 = createSchedule(doc3, today.plusDays(1), LocalTime.of(14, 0), LocalTime.of(14, 30), true);

            // BS. Hoàng Văn Cường (Thần kinh)
            Schedule sc11 = createSchedule(doc4, today.plusDays(1), LocalTime.of(10, 0), LocalTime.of(10, 30), true);
            Schedule sc12 = createSchedule(doc4, today.plusDays(2), LocalTime.of(15, 0), LocalTime.of(15, 30), true);

            // BS. Phạm Minh Đức (Nội tổng quát)
            Schedule sc13 = createSchedule(doc5, today.plusDays(1), LocalTime.of(8, 0), LocalTime.of(8, 30), true);
            Schedule sc14 = createSchedule(doc5, today.plusDays(1), LocalTime.of(8, 30), LocalTime.of(9, 0), true);

            // 6. Appointments & Medical Records mẫu
            if (appointmentRepository.count() == 0) {
                // Lịch hẹn 1: Hoàng Minh Đức đặt BS. Nguyễn Văn An
                Appointment app1 = appointmentRepository.save(Appointment.builder()
                        .bookingCode("BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                        .patient(pat1)
                        .doctor(doc1)
                        .schedule(sc1)
                        .symptoms("Đau tức ngực trái khi gắng sức, thỉnh thoảng hồi hộp đánh trống ngực")
                        .status("CONFIRMED")
                        .price(doc1.getConsultationFee())
                        .createdAt(LocalDateTime.now().minusDays(1))
                        .build());

                // Lịch hẹn 2: Nguyễn Thùy Linh đặt BS. Trần Thị Bích (Đã khám hoàn tất + Bệnh án)
                Appointment app2 = appointmentRepository.save(Appointment.builder()
                        .bookingCode("BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                        .patient(pat2)
                        .doctor(doc2)
                        .schedule(sc6)
                        .symptoms("Trẻ bị sốt cao 39 độ C kèm ho khan, biếng ăn 2 ngày nay")
                        .status("COMPLETED")
                        .price(doc2.getConsultationFee())
                        .createdAt(LocalDateTime.now().minusDays(3))
                        .build());

                // Tạo Hồ sơ bệnh án cho Lịch hẹn 2
                medicalRecordRepository.save(MedicalRecord.builder()
                        .appointment(app2)
                        .diagnosis("Viêm đường hô hấp trên cấp tính do siêu vi")
                        .doctorNotes("Bệnh nhi sốt nhẹ, họng đỏ, phế quản thô nhẹ. Đã kê đơn hạ sốt Paracetamol siro, siro ho thảo dược và rửa mũi bằng nước muối sinh lý. Nhắc nhở theo dõi thân nhiệt và tái khám sau 3 ngày nếu không giảm sốt.")
                        .createdAt(LocalDateTime.now().minusDays(3))
                        .build());

                // Lịch hẹn 3: Trần Văn Nam đặt BS. Phạm Minh Đức (CONFIRMED)
                appointmentRepository.save(Appointment.builder()
                        .bookingCode("BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                        .patient(pat4)
                        .doctor(doc5)
                        .schedule(sc13)
                        .symptoms("Khám sức khỏe tổng quát và kiểm tra chỉ số đường huyết, mỡ máu")
                        .status("CONFIRMED")
                        .price(doc5.getConsultationFee())
                        .createdAt(LocalDateTime.now().minusHours(5))
                        .build());
            }
        }

        log.info("=== DataInitializer: Hoàn tất khởi tạo dữ liệu mẫu phòng khám thành công! ===");
    }

    private Specialty createSpecialtyIfNotExists(String name, String description) {
        return specialtyRepository.findAll().stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> specialtyRepository.save(Specialty.builder().name(name).description(description).build()));
    }

    private void createServiceIfMissing(String code, String name, Specialty specialty, long price, int duration) {
        if (medicalServiceRepository.existsByCodeIgnoreCase(code)) return;
        medicalServiceRepository.save(MedicalService.builder().code(code).name(name)
                .description("Dịch vụ khám và tư vấn bởi đội ngũ bác sĩ chuyên khoa.")
                .specialty(specialty).defaultPrice(BigDecimal.valueOf(price))
                .durationMinutes(duration).active(true).build());
    }

    private void createAdminUser(String email, String password) {
        if (userRepository.existsByEmailIgnoreCase(email)) return;
        User admin = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role("ADMIN")
                .isActive(true)
                .build();
        userRepository.save(admin);
        log.info("[Seed] Created development admin account: {}", email);
    }

    private Doctor createDoctorUser(String email, String password, String fullName, String phone, Specialty specialty, String degree, int expYears, BigDecimal fee, boolean approved) {
        return doctorRepository.findAll().stream()
                .filter(d -> d.getUser() != null && email.equalsIgnoreCase(d.getUser().getEmail()))
                .findFirst()
                .orElseGet(() -> {
                    User user = userRepository.save(User.builder()
                            .email(email)
                            .passwordHash(passwordEncoder.encode(password))
                            .role("DOCTOR")
                            .isActive(true)
                            .build());

                    Doctor doctor = Doctor.builder()
                            .user(user)
                            .specialty(specialty)
                            .fullName(fullName)
                            .phone(phone)
                            .degree(degree)
                            .experienceYears(expYears)
                            .consultationFee(fee)
                            .isApproved(approved)
                            .build();
                    Doctor saved = doctorRepository.save(doctor);
                    log.info("[Seed] Created development doctor account: {} ({})", fullName, email);
                    return saved;
                });
    }

    private Patient createPatientUser(String email, String password, String fullName, String phone, LocalDate dob, String gender, String address, String medicalHistory) {
        return patientRepository.findAll().stream()
                .filter(p -> p.getUser() != null && email.equalsIgnoreCase(p.getUser().getEmail()))
                .findFirst()
                .orElseGet(() -> {
                    User user = userRepository.save(User.builder()
                            .email(email)
                            .passwordHash(passwordEncoder.encode(password))
                            .role("PATIENT")
                            .isActive(true)
                            .build());

                    Patient patient = Patient.builder()
                            .user(user)
                            .fullName(fullName)
                            .phone(phone)
                            .dob(dob)
                            .gender(gender)
                            .address(address)
                            .medicalHistory(medicalHistory)
                            .build();
                    Patient saved = patientRepository.save(patient);
                    log.info("[Seed] Created development patient account: {} ({})", fullName, email);
                    return saved;
                });
    }

    private Schedule createSchedule(Doctor doctor, LocalDate workDate, LocalTime startTime, LocalTime endTime, boolean isAvailable) {
        return scheduleRepository.save(Schedule.builder()
                .doctor(doctor)
                .workDate(workDate)
                .startTime(startTime)
                .endTime(endTime)
                .isAvailable(isAvailable)
                .build());
    }
}
