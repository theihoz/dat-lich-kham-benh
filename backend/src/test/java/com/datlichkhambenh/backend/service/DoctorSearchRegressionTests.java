package com.datlichkhambenh.backend.service;

import com.datlichkhambenh.backend.dto.response.DoctorDto;
import com.datlichkhambenh.backend.pattern.sorting.DoctorSortRegistry;
import com.datlichkhambenh.backend.pattern.sorting.NameSortStrategy;
import com.datlichkhambenh.backend.repository.DoctorRepository;
import com.datlichkhambenh.backend.repository.SpecialtyRepository;
import com.datlichkhambenh.backend.service.impl.DoctorServiceImpl;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DoctorSearchRegressionTests {
    @Test
    void filtersExcludeUnknownFeeAndExperienceInsteadOfFailing() {
        RedisCacheService cache = mock(RedisCacheService.class);
        DoctorDto unknown = DoctorDto.builder().userId(1L).fullName("Unknown").build();
        DoctorDto known = DoctorDto.builder().userId(2L).fullName("Known")
                .consultationFee(BigDecimal.TEN).experienceYears(5).build();
        when(cache.getCachedDoctors(null)).thenReturn(List.of(unknown, known));
        DoctorServiceImpl service = new DoctorServiceImpl(mock(DoctorRepository.class),
                mock(SpecialtyRepository.class), cache, new DoctorSortRegistry(List.of(new NameSortStrategy())));

        assertEquals(List.of(known), service.searchApprovedDoctors(null, null,
                BigDecimal.ZERO, null, null, "name", 0, 20));
        assertEquals(List.of(known), service.searchApprovedDoctors(null, null,
                null, BigDecimal.valueOf(20), null, "name", 0, 20));
        assertEquals(List.of(known), service.searchApprovedDoctors(null, null,
                null, null, 1, "name", 0, 20));
    }
}
