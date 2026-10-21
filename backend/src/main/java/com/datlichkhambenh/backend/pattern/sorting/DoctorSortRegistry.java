package com.datlichkhambenh.backend.pattern.sorting;

import com.datlichkhambenh.backend.exception.AppException;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DoctorSortRegistry {
    private final Map<String, DoctorSortStrategy> strategies;
    public DoctorSortRegistry(List<DoctorSortStrategy> strategies) {
        this.strategies = strategies.stream().collect(Collectors.toUnmodifiableMap(
                DoctorSortStrategy::key, Function.identity()));
    }
    public DoctorSortStrategy get(String key) {
        String effectiveKey = key == null || key.isBlank() ? "name" : key;
        DoctorSortStrategy strategy = strategies.get(effectiveKey);
        if (strategy == null) throw new AppException("Cách sắp xếp không hỗ trợ: " + effectiveKey);
        return strategy;
    }
}
