package com.datlichkhambenh.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RegisterRequestDto {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự")
    private String password;

    @NotBlank(message = "Role không được để trống (PATIENT hoặc DOCTOR)")
    @Pattern(regexp = "^(PATIENT|DOCTOR)$", message = "Role phải là PATIENT hoặc DOCTOR")
    private String role;

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    @Pattern(regexp = "^$|^[0-9+]{9,15}$", message = "Số điện thoại không đúng định dạng")
    private String phone;
    @Past(message = "Ngày sinh phải ở trong quá khứ")
    private LocalDate dob;
    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "Giới tính phải là MALE, FEMALE hoặc OTHER")
    private String gender;
    private String address;
    private String medicalHistory;

    private Long specialtyId;
    private String degree;
    @PositiveOrZero(message = "Số năm kinh nghiệm không được âm")
    private Integer experienceYears;
    @DecimalMin(value = "0", message = "Giá khám không được âm")
    private BigDecimal consultationFee;

    public RegisterRequestDto() {}

    public RegisterRequestDto(String email, String password, String role, String fullName, String phone, LocalDate dob, String gender, String address, String medicalHistory, Long specialtyId, String degree, Integer experienceYears, BigDecimal consultationFee) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.fullName = fullName;
        this.phone = phone;
        this.dob = dob;
        this.gender = gender;
        this.address = address;
        this.medicalHistory = medicalHistory;
        this.specialtyId = specialtyId;
        this.degree = degree;
        this.experienceYears = experienceYears;
        this.consultationFee = consultationFee;
    }

    public static RegisterRequestDtoBuilder builder() {
        return new RegisterRequestDtoBuilder();
    }

    public static class RegisterRequestDtoBuilder {
        private String email;
        private String password;
        private String role;
        private String fullName;
        private String phone;
        private LocalDate dob;
        private String gender;
        private String address;
        private String medicalHistory;
        private Long specialtyId;
        private String degree;
        private Integer experienceYears;
        private BigDecimal consultationFee;

        public RegisterRequestDtoBuilder email(String email) { this.email = email; return this; }
        public RegisterRequestDtoBuilder password(String password) { this.password = password; return this; }
        public RegisterRequestDtoBuilder role(String role) { this.role = role; return this; }
        public RegisterRequestDtoBuilder fullName(String fullName) { this.fullName = fullName; return this; }
        public RegisterRequestDtoBuilder phone(String phone) { this.phone = phone; return this; }
        public RegisterRequestDtoBuilder dob(LocalDate dob) { this.dob = dob; return this; }
        public RegisterRequestDtoBuilder gender(String gender) { this.gender = gender; return this; }
        public RegisterRequestDtoBuilder address(String address) { this.address = address; return this; }
        public RegisterRequestDtoBuilder medicalHistory(String medicalHistory) { this.medicalHistory = medicalHistory; return this; }
        public RegisterRequestDtoBuilder specialtyId(Long specialtyId) { this.specialtyId = specialtyId; return this; }
        public RegisterRequestDtoBuilder degree(String degree) { this.degree = degree; return this; }
        public RegisterRequestDtoBuilder experienceYears(Integer experienceYears) { this.experienceYears = experienceYears; return this; }
        public RegisterRequestDtoBuilder consultationFee(BigDecimal consultationFee) { this.consultationFee = consultationFee; return this; }

        public RegisterRequestDto build() {
            return new RegisterRequestDto(email, password, role, fullName, phone, dob, gender, address, medicalHistory, specialtyId, degree, experienceYears, consultationFee);
        }
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getMedicalHistory() { return medicalHistory; }
    public void setMedicalHistory(String medicalHistory) { this.medicalHistory = medicalHistory; }

    public Long getSpecialtyId() { return specialtyId; }
    public void setSpecialtyId(Long specialtyId) { this.specialtyId = specialtyId; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public BigDecimal getConsultationFee() { return consultationFee; }
    public void setConsultationFee(BigDecimal consultationFee) { this.consultationFee = consultationFee; }
}
