package br.com.gfctech.project_manager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordDTO(@NotBlank @Email @Size(max = 255) String email,
                               @NotBlank @Size(max = 64) String code,
                               @NotBlank @Size(min = 8, max = 72) String newPassword) {}
