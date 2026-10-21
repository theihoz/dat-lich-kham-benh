package com.datlichkhambenh.backend.demo;

import com.datlichkhambenh.backend.dto.response.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@Profile("demo")
@RequiredArgsConstructor
public class DemoNotificationController {
    private final DemoNotificationAdapter inbox;

    @GetMapping("/api/v1/admin/demo-notifications")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponseDto<List<DemoNotificationAdapter.Entry>> notifications() {
        return ApiResponseDto.success(inbox.entries());
    }
}
