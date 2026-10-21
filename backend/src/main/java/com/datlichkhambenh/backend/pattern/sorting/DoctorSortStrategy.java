package com.datlichkhambenh.backend.pattern.sorting;

import com.datlichkhambenh.backend.dto.response.DoctorDto;
import java.util.Comparator;

public interface DoctorSortStrategy {
    String key();
    Comparator<DoctorDto> comparator();
}
