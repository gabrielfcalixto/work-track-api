package br.com.gfctech.project_manager.controller;

import br.com.gfctech.project_manager.dto.AcessDTO;
import br.com.gfctech.project_manager.dto.AuthenticationDTO;
import br.com.gfctech.project_manager.dto.ResetPasswordDTO;
import br.com.gfctech.project_manager.secury.jwt.JwtUtils;
import br.com.gfctech.project_manager.service.UserDetailsImpl;
import br.com.gfctech.project_manager.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação")
public class AuthController {
    private final AuthenticationManager authentication;
    private final JwtUtils jwt;
    private final UserService users;

    public AuthController(AuthenticationManager authentication, JwtUtils jwt, UserService users) {
        this.authentication = authentication;
        this.jwt = jwt;
        this.users = users;
    }

    @PostMapping("/login")
    public AcessDTO login(@Valid @RequestBody AuthenticationDTO request) {
        var session = authentication.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.login().strip(), request.password()));
        return new AcessDTO(jwt.generateTokenFromUserDetailsImpl((UserDetailsImpl) session.getPrincipal()));
    }

    @PostMapping("/generate-reset-code")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void generateResetCode(@Valid @RequestBody ResetCodeRequest request) {
        users.generatePasswordResetCode(request.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordDTO request) {
        users.resetPassword(request.email(), request.code(), request.newPassword());
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<Map<String, String>> invalidLogin() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Login ou senha inválidos."));
    }

    public record ResetCodeRequest(@NotBlank @Email @Size(max = 255) String email) {}
}
