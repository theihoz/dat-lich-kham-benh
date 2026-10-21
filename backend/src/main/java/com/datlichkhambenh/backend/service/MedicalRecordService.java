package com.datlichkhambenh.backend.service;

import com.datlichkhambenh.backend.dto.request.UpdateMedicalRecordDto;
import com.datlichkhambenh.backend.dto.response.MedicalRecordDto;

/**
 * MedicalRecordService — Nghiệp vụ cập nhật hồ sơ bệnh án sau khám.
 * Controller chỉ được gọi interface này, không được trực tiếp dùng Repository.
 */
public interface MedicalRecordService {
    MedicalRecordDto getMedicalRecord(Long doctorUserId, Long appointmentId);

    /**
     * Bác sĩ ghi chẩn đoán và notes sau khi khám xong.
     * Đồng thời chuyển trạng thái Appointment sang COMPLETED.
     *
     * @param request DTO chứa appointmentId, diagnosis, doctorNotes
     */
    void updateMedicalRecord(Long doctorUserId, UpdateMedicalRecordDto request);
}
