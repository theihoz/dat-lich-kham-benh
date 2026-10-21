package com.datlichkhambenh.backend.service.impl;

import com.datlichkhambenh.backend.dto.request.UpdateMedicalRecordDto;
import com.datlichkhambenh.backend.dto.response.MedicalRecordDto;
import com.datlichkhambenh.backend.entity.Appointment;
import com.datlichkhambenh.backend.entity.MedicalRecord;
import com.datlichkhambenh.backend.exception.ResourceNotFoundException;
import com.datlichkhambenh.backend.exception.AppException;
import com.datlichkhambenh.backend.repository.AppointmentRepository;
import com.datlichkhambenh.backend.repository.MedicalRecordRepository;
import com.datlichkhambenh.backend.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;

    @Override
    @Transactional(readOnly = true)
    public MedicalRecordDto getMedicalRecord(Long doctorUserId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Lịch hẹn không tồn tại"));
        if (!appointment.getDoctor().getUserId().equals(doctorUserId)) {
            throw new AppException("Bạn không quản lý lịch hẹn này", org.springframework.http.HttpStatus.FORBIDDEN);
        }
        // Chưa lập bệnh án là một kết quả hợp lệ, không phải lỗi HTTP.
        return medicalRecordRepository.findByAppointmentId(appointmentId).map(record -> MedicalRecordDto.builder()
                .id(record.getId()).appointmentId(appointmentId).bookingCode(appointment.getBookingCode())
                .patientName(appointment.getPatient().getFullName()).doctorName(appointment.getDoctor().getFullName())
                .diagnosis(record.getDiagnosis()).doctorNotes(record.getDoctorNotes())
                .createdAt(record.getCreatedAt()).build()).orElse(null);
    }

    @Override
    @Transactional
    public void updateMedicalRecord(Long doctorUserId, UpdateMedicalRecordDto request) {
        // 1. Tìm lịch hẹn — ném 404 nếu không tồn tại
        Appointment appointment = appointmentRepository.findByIdForUpdate(request.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Lịch hẹn không tồn tại"));

        if (!appointment.getDoctor().getUserId().equals(doctorUserId)) {
            throw new AppException("Bạn không quản lý lịch hẹn này");
        }
        if (!"CONFIRMED".equals(appointment.getStatus()) && !"COMPLETED".equals(appointment.getStatus())) {
            throw new AppException("Không thể ghi bệnh án cho lịch đã hủy");
        }
        java.time.LocalDateTime appointmentTime = java.time.LocalDateTime.of(
                appointment.getSchedule().getWorkDate(), appointment.getSchedule().getStartTime());
        if (appointmentTime.isAfter(java.time.LocalDateTime.now())) {
            throw new AppException("Chỉ có thể ghi bệnh án khi đã đến giờ khám");
        }

        // 2. Tạo mới hoặc lấy bệnh án hiện có (1-1 với Appointment)
        MedicalRecord record = medicalRecordRepository.findByAppointmentId(appointment.getId())
                .orElseGet(() -> MedicalRecord.builder().appointment(appointment).build());

        // 3. Cập nhật nội dung chẩn đoán
        record.setDiagnosis(request.getDiagnosis());
        record.setDoctorNotes(request.getDoctorNotes());
        medicalRecordRepository.save(record);

        // 4. Chuyển trạng thái lịch hẹn sang COMPLETED
        appointment.setStatus("COMPLETED");
        appointmentRepository.save(appointment);
    }
}
