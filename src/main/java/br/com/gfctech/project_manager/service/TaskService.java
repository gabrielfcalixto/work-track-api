package br.com.gfctech.project_manager.service;

import br.com.gfctech.project_manager.dto.TaskDTO;
import br.com.gfctech.project_manager.entity.ProjectEntity;
import br.com.gfctech.project_manager.entity.TaskEntity;
import br.com.gfctech.project_manager.enums.ProjectStatus;
import br.com.gfctech.project_manager.enums.TaskStatus;
import br.com.gfctech.project_manager.exceptions.ApiException;
import br.com.gfctech.project_manager.repository.ProjectRepository;
import br.com.gfctech.project_manager.repository.TaskRepository;
import br.com.gfctech.project_manager.repository.TimeEntryRepository;
import br.com.gfctech.project_manager.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository tasks;
    private final ProjectRepository projects;
    private final UserRepository users;
    private final TimeEntryRepository entries;

    public TaskService(TaskRepository tasks, ProjectRepository projects, UserRepository users,
                       TimeEntryRepository entries) {
        this.tasks = tasks;
        this.projects = projects;
        this.users = users;
        this.entries = entries;
    }

    public List<TaskDTO> getAllTasks() {
        return tasks.findAllByOrderByIdDesc().stream().map(TaskDTO::new).toList();
    }

    public TaskDTO getTaskById(Long id) {
        return tasks.findById(id).map(TaskDTO::new)
                .orElseThrow(() -> ApiException.notFound("Tarefa não encontrada."));
    }

    public List<TaskDTO> getTasksByUserId(Long userId) {
        return tasks.findByUserId(userId).stream().map(TaskDTO::new).toList();
    }

    @Transactional
    public TaskDTO addTask(TaskDTO request) {
        var project = lockProject(request.getProjectId());
        var task = new TaskEntity();
        task.setProject(project);
        apply(task, request);
        tasks.save(task);
        refreshProjectStatus(project);
        return new TaskDTO(task);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TaskDTO updateTask(Long id, TaskDTO request) {
        var project = lockProjectOfTask(id);
        var task = lockTask(id);
        if (!project.getId().equals(request.getProjectId())) {
            throw new IllegalArgumentException("O projeto de uma tarefa existente não pode ser alterado.");
        }
        var previousStatus = task.getStatus();
        apply(task, request);
        if (previousStatus != task.getStatus()) {
            refreshProjectStatus(project);
        }
        return new TaskDTO(task);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TaskDTO updateTaskStatus(Long id, TaskStatus status) {
        var project = lockProjectOfTask(id);
        var task = lockTask(id);
        setStatus(task, status);
        refreshProjectStatus(project);
        return new TaskDTO(task);
    }

    @Transactional
    public void assignUserToTask(Long id, Long userId) {
        var task = lockTask(id);
        var user = users.findById(userId).orElseThrow(() -> ApiException.notFound("Usuário não encontrado."));
        task.getAssignedUsers().add(user);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteTask(Long id) {
        var project = lockProjectOfTask(id);
        var task = lockTask(id);
        if (task.getStatus() != TaskStatus.NAO_INICIADA || entries.existsByTaskEntityId(id)) {
            throw ApiException.conflict("Só é possível excluir uma tarefa que ainda não foi iniciada e não possui lançamentos.");
        }
        tasks.delete(task);
        refreshProjectStatus(project);
    }

    private void apply(TaskEntity task, TaskDTO request) {
        if (request.getDeadline().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("A data limite da tarefa deve ser igual ou posterior ao início.");
        }
        var assignedIds = request.getAssignedUserIds() == null ? Set.<Long>of() : request.getAssignedUserIds();
        var assignedUsers = users.findAllById(assignedIds);
        if (assignedUsers.size() != assignedIds.size()) {
            throw ApiException.notFound("Um ou mais usuários atribuídos não foram encontrados.");
        }
        setStatus(task, request.getStatus());
        task.setName(request.getName().strip());
        task.setDescription(request.getDescription().strip());
        task.setEstimatedHours(request.getEstimatedHours());
        task.setPriority(request.getPriority());
        task.setStartDate(request.getStartDate());
        task.setDeadline(request.getDeadline());
        task.setAssignedUsers(new HashSet<>(assignedUsers));
    }

    private static void setStatus(TaskEntity task, TaskStatus status) {
        if (task.getStatus() == TaskStatus.CONCLUIDA && status != TaskStatus.CONCLUIDA) {
            throw ApiException.conflict("Uma tarefa concluída não pode ser reaberta.");
        }
        task.setStatus(status);
    }

    private void refreshProjectStatus(ProjectEntity project) {
        if (project.getStatus() == ProjectStatus.CANCELADO || !tasks.existsByProjectId(project.getId())) {
            return;
        }
        boolean hasIncompleteTasks = tasks.existsByProjectIdAndStatusNot(project.getId(), TaskStatus.CONCLUIDA);
        if (!hasIncompleteTasks) {
            project.setStatus(ProjectStatus.CONCLUIDO);
        } else if (project.getStatus() == ProjectStatus.CONCLUIDO) {
            project.setStatus(ProjectStatus.EM_ANDAMENTO);
        }
    }

    private ProjectEntity lockProjectOfTask(Long taskId) {
        return lockProject(tasks.findProjectId(taskId)
                .orElseThrow(() -> ApiException.notFound("Tarefa não encontrada.")));
    }

    private ProjectEntity lockProject(Long id) {
        return projects.lockById(id).orElseThrow(() -> ApiException.notFound("Projeto não encontrado."));
    }

    private TaskEntity lockTask(Long id) {
        return tasks.lockById(id).orElseThrow(() -> ApiException.notFound("Tarefa não encontrada."));
    }
}
