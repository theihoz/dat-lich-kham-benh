package com.datlichkhambenh.backend.service.impl;

import com.datlichkhambenh.backend.dto.request.UpdateDoctorProfileDto;
import com.datlichkhambenh.backend.dto.response.DoctorDto;
import com.datlichkhambenh.backend.dto.response.SpecialtyDto;
import com.datlichkhambenh.backend.entity.Doctor;
import com.datlichkhambenh.backend.exception.AppException;
import com.datlichkhambenh.backend.exception.ResourceNotFoundException;
import com.datlichkhambenh.backend.repository.DoctorRepository;
import com.datlichkhambenh.backend.repository.SpecialtyRepository;
import com.datlichkhambenh.backend.service.DoctorService;
import com.datlichkhambenh.backend.service.RedisCacheService;
import com.datlichkhambenh.backend.pattern.sorting.DoctorSortRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {
    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;
    private final RedisCacheService redisCacheService;
    private final DoctorSortRegistry doctorSortRegistry;
    @org.springframework.beans.factory.annotation.Autowired
    private com.datlichkhambenh.backend.service.query.DoctorSearchQueryService databaseSearch;
    @org.springframework.beans.factory.annotation.Value("${app.search.database-pagination:false}")
    private boolean databasePagination;

    @Override
    @Transactional(readOnly = true)
    public List<DoctorDto> searchApprovedDoctors(Long specialtyId, String keyword, BigDecimal minFee,
                                                  BigDecimal maxFee, Integer minExperience, String sort,
                                                  int page, int size) {
        if (databasePagination) {
            doctorSortRegistry.get(sort); // Preserve Strategy registry validation and the public sort keys.
            return databaseSearch.search(specialtyId,keyword,minFee,maxFee,minExperience,sort,page,size);
        }
        List<DoctorDto> cached = redisCacheService.getCachedDoctors(specialtyId);
        if (cached == null) {
            List<Doctor> doctors = specialtyId == null
                    ? doctorRepository.findByIsApprovedTrueAndUserIsActiveTrue()
                    : doctorRepository.findBySpecialtyIdAndIsApprovedTrueAndUserIsActiveTrue(specialtyId);
            cached = doctors.stream().map(this::mapToDto).toList();
            redisCacheService.cacheDoctors(specialtyId, cached);
        }

        Stream<DoctorDto> stream = cached.stream();
        if (keyword != null && !keyword.isBlank()) {
            String normalized = keyword.trim().toLowerCase(Locale.ROOT);
            stream = stream.filter(d -> d.getFullName().toLowerCase(Locale.ROOT).contains(normalized));
        }
        if (minFee != null) stream = stream.filter(d -> d.getConsultationFee() != null && d.getConsultationFee().compareTo(minFee) >= 0);
        if (maxFee != null) stream = stream.filter(d -> d.getConsultationFee() != null && d.getConsultationFee().compareTo(maxFee) <= 0);
        if (minExperience != null) stream = stream.filter(d -> d.getExperienceYears() != null && d.getExperienceYears() >= minExperience);

        List<DoctorDto> filtered = stream.sorted(doctorSortRegistry.get(sort).comparator()).toList();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int from = (int) Math.min((long) safePage * safeSize, filtered.size());
        return filtered.subList(from, Math.min(from + safeSize, filtered.size()));
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorDto getApprovedDoctor(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .filter(d -> Boolean.TRUE.equals(d.getIsApproved()) && Boolean.TRUE.equals(d.getUser().getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bác sĩ đang hoạt động"));
        return mapToDto(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorDto getProfile(Long doctorUserId) {
        return mapToDto(findDoctor(doctorUserId));
    }

    @Override
    @Transactional
    public DoctorDto updateProfile(Long doctorUserId, UpdateDoctorProfileDto request) {
        Doctor doctor = findDoctor(doctorUserId);
        Long oldSpecialtyId = doctor.getSpecialty().getId();
        if (request.getPhone() != null && !request.getPhone().equals(doctor.getPhone())
                && doctorRepository.existsByPhone(request.getPhone())) {
            throw new AppException("Số điện thoại đã được sử dụng");
        }
        if (request.getFullName() != null) doctor.setFullName(request.getFullName().trim());
        if (request.getPhone() != null) doctor.setPhone(request.getPhone());
        if (request.getDegree() != null) doctor.setDegree(request.getDegree().trim());
        if (request.getExperienceYears() != null) doctor.setExperienceYears(request.getExperienceYears());
        if (request.getConsultationFee() != null) doctor.setConsultationFee(request.getConsultationFee());
        if (request.getSpecialtyId() != null && !request.getSpecialtyId().equals(oldSpecialtyId)) {
            doctor.setSpecialty(specialtyRepository.findById(request.getSpecialtyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyên khoa")));
        }
        Doctor saved = doctorRepository.save(doctor);
        redisCacheService.evictDoctorCache(oldSpecialtyId);
        redisCacheService.evictDoctorCache(saved.getSpecialty().getId());
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpecialtyDto> getSpecialties() {
        return specialtyRepository.findAll().stream()
                .map(s -> SpecialtyDto.builder().id(s.getId()).name(s.getName()).description(s.getDescription()).build())
                .toList();
    }

    private Doctor findDoctor(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ bác sĩ"));
    }

    private DoctorDto mapToDto(Doctor doctor) {
        return DoctorDto.builder()
                .userId(doctor.getUserId()).email(doctor.getUser().getEmail())
                .fullName(doctor.getFullName()).phone(doctor.getPhone()).degree(doctor.getDegree())
                .experienceYears(doctor.getExperienceYears()).consultationFee(doctor.getConsultationFee())
                .isApproved(doctor.getIsApproved()).specialtyId(doctor.getSpecialty().getId())
                .specialtyName(doctor.getSpecialty().getName()).build();
    }
}
