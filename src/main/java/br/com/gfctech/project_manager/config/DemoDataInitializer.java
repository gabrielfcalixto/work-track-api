package br.com.gfctech.project_manager.config;

import br.com.gfctech.project_manager.dto.ClientDTO;
import br.com.gfctech.project_manager.dto.ProjectDTO;
import br.com.gfctech.project_manager.dto.TaskDTO;
import br.com.gfctech.project_manager.dto.TimeEntryRequest;
import br.com.gfctech.project_manager.entity.UserEntity;
import br.com.gfctech.project_manager.entity.UserEntity.Role;
import br.com.gfctech.project_manager.enums.ProjectStatus;
import br.com.gfctech.project_manager.enums.TaskPriority;
import br.com.gfctech.project_manager.enums.TaskStatus;
import br.com.gfctech.project_manager.repository.UserRepository;
import br.com.gfctech.project_manager.service.ClientService;
import br.com.gfctech.project_manager.service.ProjectService;
import br.com.gfctech.project_manager.service.TaskService;
import br.com.gfctech.project_manager.service.TimeEntryService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo")
public class DemoDataInitializer implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final ClientService clients;
    private final ProjectService projects;
    private final TaskService tasks;
    private final TimeEntryService entries;

    public DemoDataInitializer(UserRepository users, PasswordEncoder encoder, ClientService clients,
                               ProjectService projects, TaskService tasks, TimeEntryService entries) {
        this.users = users;
        this.encoder = encoder;
        this.clients = clients;
        this.projects = projects;
        this.tasks = tasks;
        this.entries = entries;
    }

    @Override
    @Transactional
    public void run(String... args) {
        var admin = addUser("admin", "Alex Ribeiro", Role.ADMIN);
        var manager = addUser("gestor", "Marina Lopes", Role.MANAGER);
        var member = addUser("dev", "Rafa Martins", Role.USER);
        var client = clients.addClient(new ClientDTO(null, "Estúdio Horizonte", null, null,
                "00000000000", "contato@example.test", "(11) 0000-0000", null));
        var today = LocalDate.now();
        var start = today.minusDays(14);
        var deadline = today.plusDays(21);
        var project = projects.addProject(new ProjectDTO(null, "Portal do cliente",
                "Centralizar solicitações, arquivos e o acompanhamento de entregas.",
                80f, ProjectStatus.EM_ANDAMENTO, manager.getId(), client.id(),
                Set.of(admin.getId(), manager.getId(), member.getId()), start, deadline));
        var running = tasks.addTask(new TaskDTO(null, "Implementar cadastro de solicitações",
                "Criar o formulário e validar os campos principais.", 12.0,
                TaskStatus.EM_ANDAMENTO, TaskPriority.ALTA, null, project.getId(),
                Set.of(admin.getId(), member.getId()), start, deadline));
        var completed = tasks.addTask(new TaskDTO(null, "Mapear jornada do cliente",
                "Revisar as etapas de abertura e acompanhamento de solicitações.", 6.0,
                TaskStatus.CONCLUIDA, TaskPriority.MEDIA, null, project.getId(),
                Set.of(manager.getId(), member.getId()), start, today));
        tasks.addTask(new TaskDTO(null, "Revisar acessibilidade do portal",
                "Verificar navegação por teclado, mensagens de erro e contraste.", 5.0,
                TaskStatus.NAO_INICIADA, TaskPriority.MEDIA, null, project.getId(),
                Set.of(admin.getId(), manager.getId()), today, deadline));
        entries.create(member.getId(), new TimeEntryRequest(running.getId(), null, "Validação do formulário",
                today, LocalTime.of(9, 0), LocalTime.of(10, 30)));
        entries.create(manager.getId(), new TimeEntryRequest(completed.getId(), null, "Revisão das etapas da jornada",
                today, LocalTime.of(13, 0), LocalTime.of(14, 45)));
        entries.create(admin.getId(), new TimeEntryRequest(running.getId(), null, "Revisão do contrato da API",
                today, LocalTime.of(10, 0), LocalTime.of(11, 0)));
    }

    private UserEntity addUser(String login, String name, Role role) {
        var user = new UserEntity();
        user.setName(name);
        user.setLogin(login);
        user.setEmail(login + "@example.test");
        user.setPassword(encoder.encode("WorktrackDemo!"));
        user.setRole(role);
        return users.save(user);
    }
}
