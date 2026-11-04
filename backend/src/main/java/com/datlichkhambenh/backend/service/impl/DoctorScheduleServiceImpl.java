package com.datlichkhambenh.backend.service.impl;

import com.datlichkhambenh.backend.dto.request.CreateScheduleDto;
import com.datlichkhambenh.backend.dto.response.ScheduleDto;
import com.datlichkhambenh.backend.entity.Doctor;
import com.datlichkhambenh.backend.entity.Schedule;
import com.datlichkhambenh.backend.exception.AppException;
import com.datlichkhambenh.backend.exception.ResourceNotFoundException;
import com.datlichkhambenh.backend.repository.DoctorRepository;
import com.datlichkhambenh.backend.repository.ScheduleRepository;
import com.datlichkhambenh.backend.repository.AppointmentRepository;
import com.datlichkhambenh.backend.service.DoctorScheduleService;
import com.datlichkhambenh.backend.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorScheduleServiceImpl implements DoctorScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final DoctorRepository doctorRepository;
    private final RedisCacheService redisCacheService;
    private final AppointmentRepository appointmentRepository;

    @Override
    @Transactional
    public ScheduleDto createSlot(Long doctorUserId, CreateScheduleDto request) {
        Doctor doctor = doctorRepository.findByIdForUpdate(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Bác sĩ không tồn tại"));

        if (!Boolean.TRUE.equals(doctor.getIsApproved())) {
            throw new AppException("Tài khoản Bác sĩ chưa được Admin phê duyệt");
        }
        if (!request.getWorkDate().isAfter(LocalDate.now())) {
            throw new AppException("Ngày làm việc phải sau ngày hiện tại");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new AppException("Giờ kết thúc phải sau giờ bắt đầu");
        }
        if (scheduleRepository.existsByDoctorUserIdAndWorkDateAndStartTimeLessThanAndEndTimeGreaterThan(
                doctorUserId, request.getWorkDate(), request.getEndTime(), request.getStartTime())) {
            throw new AppException("Khung giờ bị trùng với ca làm việc đã đăng ký");
        }

        Schedule schedule = Schedule.builder()
                .doctor(doctor)
                .workDate(request.getWorkDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .isAvailable(true)
                .build();

        Schedule saved = scheduleRepository.save(schedule);

        // Evict Redis cache cho bác sĩ & ngày làm việc này
        redisCacheService.evictSlotCache(doctorUserId, request.getWorkDate());

        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleDto> getAvailableSlots(Long doctorUserId, LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            return List.of();
        }
        Doctor doctor = doctorRepository.findById(doctorUserId)
                .filter(d -> Boolean.TRUE.equals(d.getIsApproved()) && Boolean.TRUE.equals(d.getUser().getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bác sĩ đang hoạt động"));
        // Pattern Cache-Aside: 1. Đọc từ Redis trước
        List<ScheduleDto> cached = redisCacheService.getCachedAvailableSlots(doctorUserId, date);
        if (cached != null) {
            LocalDateTime now = LocalDateTime.now();
            return cached.stream().filter(slot -> LocalDateTime.of(slot.getWorkDate(), slot.getStartTime())
                    .isAfter(now)).toList();
        }

        // 2. Cache miss -> query DB
        List<Schedule> slots = scheduleRepository.findByDoctorUserIdAndWorkDateAndIsAvailableTrue(doctorUserId, date);
        LocalDateTime now = LocalDateTime.now();
        List<ScheduleDto> result = slots.stream()
                .filter(slot -> LocalDateTime.of(slot.getWorkDate(), slot.getStartTime()).isAfter(now))
                .map(this::mapToDto).collect(Collectors.toList());

        // 3. Ghi lại vào Redis cache
        redisCacheService.cacheAvailableSlots(doctorUserId, date, result);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleDto> getDoctorSlots(Long doctorUserId) {
        return scheduleRepository
                .findByDoctorUserIdAndWorkDateGreaterThanEqualOrderByWorkDateAscStartTimeAsc(doctorUserId, LocalDate.now())
                .stream().map(this::mapToDto).toList();
    }

    @Override
    @Transactional
    public void toggleSlotAvailability(Long doctorUserId, Long scheduleId, boolean available) {
        Schedule schedule = scheduleRepository.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Ca khám không tồn tại"));

        if (!schedule.getDoctor().getUserId().equals(doctorUserId)) {
            throw new AppException("Bạn không quản lý ca khám này", org.springframework.http.HttpStatus.FORBIDDEN);
        }
        if (appointmentRepository.existsByScheduleId(scheduleId)) {
            throw new AppException("Không thể thay đổi ca khám đã có lịch hẹn");
        }
        if (java.time.LocalDateTime.of(schedule.getWorkDate(), schedule.getStartTime())
                .isBefore(java.time.LocalDateTime.now())) {
            throw new AppException("Không thể thay đổi ca khám đã qua");
        }

        schedule.setIsAvailable(available);
        scheduleRepository.save(schedule);

        redisCacheService.evictSlotCache(schedule.getDoctor().getUserId(), schedule.getWorkDate());
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ScheduleDto updateSlot(Long doctorUserId, Long scheduleId, CreateScheduleDto request) {
        Schedule schedule = scheduleRepository.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Ca khám không tồn tại"));

        if (!schedule.getDoctor().getUserId().equals(doctorUserId)) {
            throw new AppException("Bạn không quản lý ca khám này", org.springframework.http.HttpStatus.FORBIDDEN);
        }
        // Keep schedule -> doctor lock order consistent with booking's foreign-key checks.
        // READ_COMMITTED makes overlap checks see changes committed while waiting for this lock.
        doctorRepository.findByIdForUpdate(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Bác sĩ không tồn tại"));
        if (appointmentRepository.existsByScheduleId(scheduleId)) {
            throw new AppException("Không thể chỉnh sửa ca khám đã có lịch hẹn");
        }
        if (!request.getWorkDate().isAfter(LocalDate.now())) {
            throw new AppException("Ngày làm việc phải sau ngày hiện tại");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new AppException("Giờ kết thúc phải sau giờ bắt đầu");
        }
        if (scheduleRepository.existsDuplicateSlot(
                doctorUserId, request.getWorkDate(), request.getStartTime(), request.getEndTime(), scheduleId)) {
            throw new AppException("Khung giờ bị trùng với ca làm việc đã đăng ký khác");
        }

        LocalDate oldDate = schedule.getWorkDate();
        schedule.setWorkDate(request.getWorkDate());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        Schedule saved = scheduleRepository.save(schedule);

        redisCacheService.evictSlotCache(doctorUserId, oldDate);
        if (!oldDate.equals(request.getWorkDate())) {
            redisCacheService.evictSlotCache(doctorUserId, request.getWorkDate());
        }

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void deleteSlot(Long doctorUserId, Long scheduleId) {
        Schedule schedule = scheduleRepository.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Ca khám không tồn tại"));

        if (!schedule.getDoctor().getUserId().equals(doctorUserId)) {
            throw new AppException("Bạn không quản lý ca khám này", org.springframework.http.HttpStatus.FORBIDDEN);
        }
        if (appointmentRepository.existsByScheduleId(scheduleId)) {
            throw new AppException("Không thể xóa ca khám đã có lịch hẹn");
        }

        scheduleRepository.delete(schedule);
        redisCacheService.evictSlotCache(doctorUserId, schedule.getWorkDate());
    }

    private ScheduleDto mapToDto(Schedule schedule) {
        return ScheduleDto.builder()
                .id(schedule.getId())
                .doctorId(schedule.getDoctor().getUserId())
                .doctorName(schedule.getDoctor().getFullName())
                .workDate(schedule.getWorkDate())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .isAvailable(schedule.getIsAvailable())
                .isBooked(appointmentRepository.existsByScheduleId(schedule.getId()))
                .build();
    }
}
