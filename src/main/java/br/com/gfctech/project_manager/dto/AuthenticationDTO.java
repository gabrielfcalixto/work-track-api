package br.com.gfctech.project_manager.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthenticationDTO(@JsonAlias("username") @NotBlank @Size(max = 255) String login,
                                @NotBlank @Size(max = 72) String password) {}
