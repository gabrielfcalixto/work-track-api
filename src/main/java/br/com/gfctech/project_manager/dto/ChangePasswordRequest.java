package br.com.gfctech.project_manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(@NotBlank @Size(max = 72) String oldPassword,
                                    @NotBlank @Size(min = 8, max = 72) String newPassword) {}
