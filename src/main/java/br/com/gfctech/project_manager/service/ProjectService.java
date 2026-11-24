package br.com.gfctech.project_manager.service;

import br.com.gfctech.project_manager.dto.ProjectDTO;
import br.com.gfctech.project_manager.entity.ProjectEntity;
import br.com.gfctech.project_manager.entity.UserEntity.Role;
import br.com.gfctech.project_manager.enums.ProjectStatus;
import br.com.gfctech.project_manager.exceptions.ApiException;
import br.com.gfctech.project_manager.repository.ClientRepository;
import br.com.gfctech.project_manager.repository.ProjectRepository;
import br.com.gfctech.project_manager.repository.TaskRepository;
import br.com.gfctech.project_manager.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProjectService {
    private final ProjectRepository projects;
    private final UserRepository users;
    private final ClientRepository clients;
    private final TaskRepository tasks;

    public ProjectService(ProjectRepository projects, UserRepository users, ClientRepository clients,
                          TaskRepository tasks) {
        this.projects = projects;
        this.users = users;
        this.clients = clients;
        this.tasks = tasks;
    }

    public List<ProjectDTO> getAllProjects() {
        return projects.findAllByOrderByIdDesc().stream().map(ProjectDTO::new).toList();
    }

    public ProjectDTO getProjectById(Long id) {
        return projects.findById(id).map(ProjectDTO::new)
                .orElseThrow(() -> ApiException.notFound("Projeto não encontrado."));
    }

    @Transactional
    public ProjectDTO addProject(ProjectDTO request) {
        var project = new ProjectEntity();
        apply(project, request);
        return new ProjectDTO(projects.save(project));
    }

    @Transactional
    public ProjectDTO updateProject(Long id, ProjectDTO request) {
        var project = lockProject(id);
        apply(project, request);
        return new ProjectDTO(project);
    }

    @Transactional
    public ProjectDTO updateStatus(Long id, String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Informe o status do projeto.");
        }
        ProjectStatus parsed;
        try {
            parsed = ProjectStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Status de projeto inválido.");
        }
        var project = lockProject(id);
        project.setStatus(parsed);
        return new ProjectDTO(project);
    }

    @Transactional
    public void deleteProject(Long id) {
        var project = lockProject(id);
        if (tasks.existsByProjectId(id)) {
            throw ApiException.conflict("Exclua as tarefas do projeto antes de excluir o projeto.");
        }
        projects.delete(project);
    }

    private void apply(ProjectEntity project, ProjectDTO request) {
        if (request.getDeadline().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("A data limite do projeto deve ser igual ou posterior ao início.");
        }
        var manager = users.findById(request.getManagerId())
                .orElseThrow(() -> ApiException.notFound("Responsável não encontrado."));
        boolean managerChanged = project.getManager() == null || !manager.getId().equals(project.getManager().getId());
        if (managerChanged && manager.getRole() == Role.USER) {
            throw new IllegalArgumentException("Selecione um gestor ou administrador como responsável.");
        }
        var client = clients.findById(request.getClientId())
                .orElseThrow(() -> ApiException.notFound("Cliente não encontrado."));
        var memberIds = request.getTeamMemberIds() == null ? Set.<Long>of() : request.getTeamMemberIds();
        var members = users.findAllById(memberIds);
        if (members.size() != memberIds.size()) {
            throw ApiException.notFound("Um ou mais membros da equipe não foram encontrados.");
        }
        project.setName(request.getName().strip());
        project.setDescription(request.getDescription().strip());
        project.setHours(request.getHours());
        project.setStatus(request.getStatus());
        project.setManager(manager);
        project.setClient(client);
        project.setTeamMembers(new HashSet<>(members));
        project.setStartDate(request.getStartDate());
        project.setDeadline(request.getDeadline());
    }

    private ProjectEntity lockProject(Long id) {
        return projects.lockById(id).orElseThrow(() -> ApiException.notFound("Projeto não encontrado."));
    }
}
