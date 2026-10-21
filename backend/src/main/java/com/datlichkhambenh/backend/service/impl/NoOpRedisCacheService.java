package com.datlichkhambenh.backend.service.impl;

import com.datlichkhambenh.backend.dto.response.DoctorDto;
import com.datlichkhambenh.backend.dto.response.ScheduleDto;
import com.datlichkhambenh.backend.service.RedisCacheService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Profile({"test", "demo"})
public class NoOpRedisCacheService implements RedisCacheService {
    public List<DoctorDto> getCachedDoctors(Long specialtyId) { return null; }
    public void cacheDoctors(Long specialtyId, List<DoctorDto> doctors) { }
    public void evictDoctorCache(Long specialtyId) { }
    public List<ScheduleDto> getCachedAvailableSlots(Long doctorId, LocalDate date) { return null; }
    public void cacheAvailableSlots(Long doctorId, LocalDate date, List<ScheduleDto> slots) { }
    public void evictSlotCache(Long doctorId, LocalDate date) { }
}
