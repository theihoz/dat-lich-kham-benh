package com.datlichkhambenh.backend.pattern.sorting;

import com.datlichkhambenh.backend.dto.response.DoctorDto;
import org.springframework.stereotype.Component;
import java.util.Comparator;

@Component
public class ExperienceSortStrategy implements DoctorSortStrategy {
    @Override public String key() { return "experience"; }
    @Override public Comparator<DoctorDto> comparator() {
        return Comparator.comparing(DoctorDto::getExperienceYears).reversed();
    }
}
