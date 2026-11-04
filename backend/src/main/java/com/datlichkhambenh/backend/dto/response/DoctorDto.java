package com.datlichkhambenh.backend.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorDto {

    private Long userId;
    private String email;
    private Boolean isActive;
    private String fullName;
    private String phone;
    private String degree;
    private Integer experienceYears;
    private BigDecimal consultationFee;
    private Boolean isApproved;
    private Long specialtyId;
    private String specialtyName;
}
