package com.datlichkhambenh.backend.controller;

import com.datlichkhambenh.backend.dto.response.ApiResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class ClinicInfoController {
    private final Map<String,Object> info;
    public ClinicInfoController(@Value("${app.clinic.name:Phòng khám}") String name,
            @Value("${app.clinic.address:}") String address, @Value("${app.clinic.phone:}") String phone) {
        info=Map.of("name",name,"address",address,"phone",phone,"timeZone","Asia/Ho_Chi_Minh","cancellationHours",24);
    }
    @GetMapping("/api/v1/clinic-info")
    public ApiResponseDto<Map<String,Object>> info() { return ApiResponseDto.success(info); }
}
