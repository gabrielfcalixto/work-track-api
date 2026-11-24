package br.com.gfctech.project_manager.dto;

import br.com.gfctech.project_manager.entity.UserEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserDTO {
    private Long id;
    @NotBlank(message = "Informe o nome do usuário.") @Size(max = 255)
    private String name;
    @NotBlank(message = "Informe o login do usuário.") @Size(max = 255)
    private String login;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    @NotBlank(message = "Informe o e-mail do usuário.") @Email @Size(max = 255)
    private String email;
    @NotBlank(message = "Informe o perfil de acesso.")
    private String role;
    private LocalDate joinDate;

    public UserDTO(UserEntity user) {
        id = user.getId();
        name = user.getName();
        login = user.getLogin();
        email = user.getEmail();
        role = user.getRole().name();
        joinDate = user.getJoinDate();
    }
}
