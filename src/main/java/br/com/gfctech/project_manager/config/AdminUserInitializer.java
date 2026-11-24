package br.com.gfctech.project_manager.config;

import br.com.gfctech.project_manager.entity.UserEntity;
import br.com.gfctech.project_manager.entity.UserEntity.Role;
import br.com.gfctech.project_manager.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!demo & !test")
public class AdminUserInitializer implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String login;
    private final String email;
    private final String password;

    public AdminUserInitializer(UserRepository users, PasswordEncoder encoder,
                                @Value("${worktrack.admin.login}") String login,
                                @Value("${worktrack.admin.email}") String email,
                                @Value("${worktrack.admin.password}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.login = login.strip();
        this.email = email.strip().toLowerCase(Locale.ROOT);
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (password.isEmpty() || users.findByLogin(login).isPresent()) {
            return;
        }
        if (login.isBlank() || email.isBlank() || password.isBlank() || password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Configure ADMIN_LOGIN, ADMIN_EMAIL e uma senha inicial válida.");
        }
        var admin = new UserEntity();
        admin.setName("Administrador");
        admin.setLogin(login);
        admin.setEmail(email);
        admin.setPassword(encoder.encode(password));
        admin.setRole(Role.ADMIN);
        users.save(admin);
    }
}
