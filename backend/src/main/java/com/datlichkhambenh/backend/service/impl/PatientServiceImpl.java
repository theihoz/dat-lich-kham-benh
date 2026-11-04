package com.datlichkhambenh.backend.service.impl;

import com.datlichkhambenh.backend.dto.response.MedicalRecordDto;
import com.datlichkhambenh.backend.dto.response.PatientDto;
import com.datlichkhambenh.backend.dto.request.UpdatePatientProfileDto;
import com.datlichkhambenh.backend.entity.MedicalRecord;
import com.datlichkhambenh.backend.entity.Patient;
import com.datlichkhambenh.backend.exception.ResourceNotFoundException;
import com.datlichkhambenh.backend.repository.MedicalRecordRepository;
import com.datlichkhambenh.backend.repository.PatientRepository;
import com.datlichkhambenh.backend.repository.DoctorRepository;
import com.datlichkhambenh.backend.exception.AppException;
import com.datlichkhambenh.backend.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final DoctorRepository doctorRepository;
    private final com.datlichkhambenh.backend.audit.MedicalAuditService audit;

    @Override
    @Transactional(readOnly = true)
    public PatientDto getProfile(Long patientUserId) {
        Patient patient = patientRepository.findById(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ bệnh nhân không tồn tại"));
        return mapToDto(patient);
    }

    @Override
    @Transactional
    public PatientDto updateProfile(Long patientUserId, UpdatePatientProfileDto request) {
        Patient patient = patientRepository.findById(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ bệnh nhân không tồn tại"));

        if (request.getPhone() != null && !request.getPhone().equals(patient.getPhone())
                && (patientRepository.existsByPhone(request.getPhone()) || doctorRepository.existsByPhone(request.getPhone()))) {
            throw new AppException("Số điện thoại đã được sử dụng");
        }
        if (request.getFullName() != null) patient.setFullName(request.getFullName().trim());
        if (request.getPhone() != null) patient.setPhone(request.getPhone());
        if (request.getDob() != null) patient.setDob(request.getDob());
        if (request.getGender() != null) patient.setGender(request.getGender());
        if (request.getAddress() != null) patient.setAddress(request.getAddress());
        if (request.getMedicalHistory() != null) patient.setMedicalHistory(request.getMedicalHistory());

        Patient saved = patientRepository.save(patient);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public List<MedicalRecordDto> getPatientMedicalRecords(Long patientUserId) {
        Long actorId = audit.resolveActor(patientUserId);
        List<MedicalRecord> records = medicalRecordRepository.findByAppointmentPatientUserId(actorId);
        if (records.isEmpty()) audit.record(actorId, "READ_LIST", null, null);
        for (MedicalRecord record : records) audit.record(actorId, "READ", record.getId(), record.getAppointment().getId());
        List<MedicalRecordDto> list = records.stream().map(r -> MedicalRecordDto.builder()
                .id(r.getId())
                .appointmentId(r.getAppointment().getId())
                .bookingCode(r.getAppointment().getBookingCode())
                .patientName(r.getAppointment().getPatient().getFullName())
                .doctorName(r.getAppointment().getDoctor().getFullName())
                .diagnosis(r.getDiagnosis())
                .doctorNotes(r.getDoctorNotes())
                .createdAt(r.getCreatedAt())
                .build()
        ).collect(Collectors.toList());

        // Sắp xếp bệnh án mới nhất lên đầu
        list.sort(java.util.Comparator.comparing(MedicalRecordDto::getCreatedAt,
                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));

        return list;
    }

    private PatientDto mapToDto(Patient patient) {
        return PatientDto.builder()
                .userId(patient.getUserId())
                .email(patient.getUser().getEmail())
                .fullName(patient.getFullName())
                .phone(patient.getPhone())
                .dob(patient.getDob())
                .gender(patient.getGender())
                .address(patient.getAddress())
                .medicalHistory(patient.getMedicalHistory())
                .build();
    }
}
