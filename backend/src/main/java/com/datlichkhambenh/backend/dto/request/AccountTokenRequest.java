package com.datlichkhambenh.backend.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data public class AccountTokenRequest {
 @NotBlank @Size(max=1024) private String token;
}
