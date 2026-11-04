package com.datlichkhambenh.backend.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data public class ResetPasswordRequest {
 @NotBlank @Size(max=1024) private String token;
 @NotBlank @Size(min=8,max=72) private String password;
}
