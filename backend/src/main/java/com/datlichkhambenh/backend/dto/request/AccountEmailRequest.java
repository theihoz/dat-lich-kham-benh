package com.datlichkhambenh.backend.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data public class AccountEmailRequest {
 @NotBlank @Email @Size(max=255) private String email;
}
