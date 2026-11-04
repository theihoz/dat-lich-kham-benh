package com.datlichkhambenh.backend.dto.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientDto {

    private Long userId;
    private String email;
    private Boolean isActive;
    private String fullName;
    private LocalDate dob;
    private String gender;
    private String phone;
    private String address;
    private String medicalHistory;
}
