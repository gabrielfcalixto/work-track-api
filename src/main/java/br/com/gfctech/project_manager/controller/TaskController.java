package br.com.gfctech.project_manager.controller;

import br.com.gfctech.project_manager.dto.TaskDTO;
import br.com.gfctech.project_manager.enums.TaskStatus;
import br.com.gfctech.project_manager.service.TaskService;
import br.com.gfctech.project_manager.service.UserDetailsImpl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/task")
public class TaskController {
    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping
    public List<TaskDTO> getAllTasks() {
        return service.getAllTasks();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN') or @taskRepository.existsByIdAndAssignedUsersId(#id, principal.id)")
    public TaskDTO getTaskById(@PathVariable Long id) {
        return service.getTaskById(id);
    }

    @GetMapping("/me")
    public List<TaskDTO> getMyTasks(@AuthenticationPrincipal UserDetailsImpl user) {
        return service.getTasksByUserId(user.getId());
    }

    @GetMapping("/usertask/{userId}")
    @PreAuthorize("#userId == principal.id or hasAuthority('ADMIN')")
    public List<TaskDTO> getTasksByUserId(@PathVariable Long userId) {
        return service.getTasksByUserId(userId);
    }

    @PostMapping({"", "/add"})
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDTO addTask(@Valid @RequestBody TaskDTO request) {
        return service.addTask(request);
    }

    @PutMapping("/{id}")
    public TaskDTO updateTask(@PathVariable Long id, @Valid @RequestBody TaskDTO request) {
        return service.updateTask(id, request);
    }

    @PatchMapping("/update-status/{id}")
    public TaskDTO updateStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String status = request.get("status");
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Informe o status da tarefa.");
        }
        TaskStatus parsed;
        try {
            parsed = TaskStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Status de tarefa inválido.");
        }
        return service.updateTaskStatus(id, parsed);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id) {
        service.deleteTask(id);
    }

    @PostMapping("/{id}/assign-user")
    public void assignUser(@PathVariable Long id, @Valid @RequestBody TaskAssignment request) {
        service.assignUserToTask(id, request.userId());
    }

    public record TaskAssignment(@NotNull @Positive Long userId) {}
}
