package br.com.gfctech.project_manager.service;

import br.com.gfctech.project_manager.dto.UserDTO;
import br.com.gfctech.project_manager.entity.UserEntity;
import br.com.gfctech.project_manager.entity.UserEntity.Role;
import br.com.gfctech.project_manager.exceptions.ApiException;
import br.com.gfctech.project_manager.repository.ProjectRepository;
import br.com.gfctech.project_manager.repository.TaskRepository;
import br.com.gfctech.project_manager.repository.TimeEntryRepository;
import br.com.gfctech.project_manager.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final TimeEntryRepository entries;
    private final EmailService mail;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, ProjectRepository projects, TaskRepository tasks,
                       TimeEntryRepository entries, EmailService mail, PasswordEncoder encoder) {
        this.users = users;
        this.projects = projects;
        this.tasks = tasks;
        this.entries = entries;
        this.mail = mail;
        this.encoder = encoder;
    }

    public List<UserDTO> getAllUsers() {
        return users.findAll(Sort.by("name", "id")).stream().map(UserDTO::new).toList();
    }

    public UserDTO getUserById(Long id) {
        return users.findById(id).map(UserDTO::new)
                .orElseThrow(() -> ApiException.notFound("Usuário não encontrado."));
    }

    @Transactional
    public void addUser(UserDTO request) {
        var user = new UserEntity();
        apply(user, request);
        boolean emailPassword = request.getPassword() == null || request.getPassword().isBlank();
        if (emailPassword && !mail.isEnabled()) {
            throw new IllegalArgumentException("Informe uma senha inicial para o usuário.");
        }
        String password = emailPassword ? randomCode() : request.getPassword();
        user.setPassword(encodePassword(password));
        users.saveAndFlush(user);
        if (emailPassword) {
            mail.send(user.getEmail(), "Seu acesso ao Work Track",
                    "Sua senha inicial é " + password + ". Altere a senha no seu perfil após entrar.");
        }
    }

    @Transactional
    public UserDTO updateUser(Long actorId, Long id, UserDTO request) {
        var user = lockUser(id);
        requireValidRoleChange(actorId, id, parseRole(request.getRole()));
        apply(user, request);
        return new UserDTO(user);
    }

    @Transactional
    public UserDTO updatePermissions(Long actorId, Long id, String role) {
        var user = lockUser(id);
        var parsed = parseRole(role);
        requireValidRoleChange(actorId, id, parsed);
        user.setRole(parsed);
        return new UserDTO(user);
    }

    @Transactional
    public void deleteUser(Long actorId, Long id) {
        if (Objects.equals(actorId, id)) {
            throw ApiException.conflict("Sua conta não pode ser excluída pela própria sessão.");
        }
        var user = lockUser(id);
        if (projects.existsByManagerIdOrTeamMembersId(id, id) || tasks.existsByAssignedUsersId(id)
                || entries.existsByUserId(id)) {
            throw ApiException.conflict("O usuário possui vínculos com projetos, tarefas ou lançamentos.");
        }
        users.delete(user);
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        var user = lockUser(userId);
        if (!encoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("A senha atual está incorreta.");
        }
        if (oldPassword.equals(newPassword)) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da atual.");
        }
        setPassword(user, newPassword);
    }

    @Transactional
    public void generatePasswordResetCode(String email) {
        if (!mail.isEnabled()) {
            throw ApiException.unavailable("A recuperação por e-mail está desativada neste ambiente.");
        }
        users.lockByEmail(normalizeEmail(email)).ifPresent(user -> {
            var now = LocalDateTime.now();
            if (user.getCodeExpirationDate() != null && user.getCodeExpirationDate().isAfter(now.plusMinutes(14))) {
                return;
            }
            String code = randomCode();
            user.setResetPasswordCode(encoder.encode(code));
            user.setCodeExpirationDate(now.plusMinutes(15));
            mail.send(user.getEmail(), "Recuperação de senha do Work Track",
                    "Seu código de recuperação é " + code + ". Ele é válido por 15 minutos.");
        });
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        var user = users.lockByEmail(normalizeEmail(email))
                .orElseThrow(UserService::invalidResetCode);
        String storedCode = user.getResetPasswordCode();
        if (storedCode == null || user.getCodeExpirationDate() == null
                || !user.getCodeExpirationDate().isAfter(LocalDateTime.now())
                || !storedCode.startsWith("$2") || !encoder.matches(code, storedCode)) {
            throw invalidResetCode();
        }
        setPassword(user, newPassword);
    }

    private void apply(UserEntity user, UserDTO request) {
        String login = request.getLogin().strip();
        String email = normalizeEmail(request.getEmail());
        if (users.findByLogin(login).filter(existing -> !existing.getId().equals(user.getId())).isPresent()) {
            throw ApiException.conflict("O login já está em uso.");
        }
        if (users.findByEmail(email).filter(existing -> !existing.getId().equals(user.getId())).isPresent()) {
            throw ApiException.conflict("O e-mail já está em uso.");
        }
        if (!Objects.equals(user.getEmail(), email) || !Objects.equals(user.getLogin(), login)) {
            clearResetCode(user);
        }
        user.setName(request.getName().strip());
        user.setLogin(login);
        user.setEmail(email);
        user.setRole(parseRole(request.getRole()));
    }

    private UserEntity lockUser(Long id) {
        return users.lockById(id).orElseThrow(() -> ApiException.notFound("Usuário não encontrado."));
    }

    private static void requireValidRoleChange(Long actorId, Long id, Role role) {
        if (Objects.equals(actorId, id) && role != Role.ADMIN) {
            throw ApiException.conflict("Seu perfil de administrador não pode ser removido pela própria sessão.");
        }
    }

    private static Role parseRole(String value) {
        try {
            return Role.valueOf(value == null ? "" : value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Perfil de acesso inválido.");
        }
    }

    private void setPassword(UserEntity user, String password) {
        user.setPassword(encodePassword(password));
        clearResetCode(user);
    }

    private String encodePassword(String password) {
        if (password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("A senha deve ter ao menos 8 caracteres e no máximo 72 bytes.");
        }
        return encoder.encode(password);
    }

    private static void clearResetCode(UserEntity user) {
        user.setResetPasswordCode(null);
        user.setCodeExpirationDate(null);
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static String randomCode() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static IllegalArgumentException invalidResetCode() {
        return new IllegalArgumentException("Código de recuperação inválido ou expirado.");
    }
}
