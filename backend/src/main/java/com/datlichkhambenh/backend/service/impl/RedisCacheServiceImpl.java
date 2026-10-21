package com.datlichkhambenh.backend.service.impl;

import com.datlichkhambenh.backend.dto.response.DoctorDto;
import com.datlichkhambenh.backend.dto.response.ScheduleDto;
import com.datlichkhambenh.backend.service.RedisCacheService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Profile("!test & !demo")
@RequiredArgsConstructor
public class RedisCacheServiceImpl implements RedisCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.cache.doctor-list-ttl:300}")
    private long doctorListTtl;

    @Value("${app.cache.available-slots-ttl:60}")
    private long availableSlotsTtl;

    @Override
    public List<DoctorDto> getCachedDoctors(Long specialtyId) {
        String key = "doctors:specialty:" + (specialtyId != null ? specialtyId : "all");
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                log.info("[Redis Cache] Hit for doctors list: {}", key);
                return objectMapper.readValue(json, new TypeReference<List<DoctorDto>>() {});
            }
        } catch (Exception e) {
            log.warn("[Redis Cache] Failed to read doctors cache: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public void cacheDoctors(Long specialtyId, List<DoctorDto> doctors) {
        String key = "doctors:specialty:" + (specialtyId != null ? specialtyId : "all");
        try {
            String json = objectMapper.writeValueAsString(doctors);
            redisTemplate.opsForValue().set(key, json, doctorListTtl, TimeUnit.SECONDS);
            log.info("[Redis Cache] Cached doctors list under key: {}", key);
        } catch (Exception e) {
            log.warn("[Redis Cache] Failed to write doctors cache: {}", e.getMessage());
        }
    }

    @Override
    public void evictDoctorCache(Long specialtyId) {
        try {
            String key = "doctors:specialty:" + (specialtyId != null ? specialtyId : "all");
            redisTemplate.delete(key);
            redisTemplate.delete("doctors:specialty:all");
            log.info("[Redis Cache] Evicted doctors cache for key: {}", key);
        } catch (Exception e) {
            log.warn("[Redis Cache] Failed to evict doctors cache: {}", e.getMessage());
        }
    }

    @Override
    public List<ScheduleDto> getCachedAvailableSlots(Long doctorId, LocalDate date) {
        String key = String.format("slots:doctor:%d:date:%s", doctorId, date.toString());
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                log.info("[Redis Cache] Hit for available slots: {}", key);
                return objectMapper.readValue(json, new TypeReference<List<ScheduleDto>>() {});
            }
        } catch (Exception e) {
            log.warn("[Redis Cache] Failed to read slot cache: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public void cacheAvailableSlots(Long doctorId, LocalDate date, List<ScheduleDto> slots) {
        String key = String.format("slots:doctor:%d:date:%s", doctorId, date.toString());
        try {
            String json = objectMapper.writeValueAsString(slots);
            redisTemplate.opsForValue().set(key, json, availableSlotsTtl, TimeUnit.SECONDS);
            log.info("[Redis Cache] Cached slots under key: {}", key);
        } catch (Exception e) {
            log.warn("[Redis Cache] Failed to write slot cache: {}", e.getMessage());
        }
    }

    @Override
    public void evictSlotCache(Long doctorId, LocalDate date) {
        String key = String.format("slots:doctor:%d:date:%s", doctorId, date.toString());
        try {
            redisTemplate.delete(key);
            log.info("[Redis Cache] Evicted slot cache for key: {}", key);
        } catch (Exception e) {
            log.warn("[Redis Cache] Failed to evict slot cache: {}", e.getMessage());
        }
    }
}
