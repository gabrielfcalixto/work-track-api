package br.com.gfctech.project_manager.controller;

import br.com.gfctech.project_manager.dto.ProjectDTO;
import br.com.gfctech.project_manager.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/project")
public class ProjectController {
    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProjectDTO> getAllProjects() {
        return service.getAllProjects();
    }

    @GetMapping("/{id}")
    public ProjectDTO getProjectById(@PathVariable Long id) {
        return service.getProjectById(id);
    }

    @PostMapping({"", "/add"})
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDTO addProject(@Valid @RequestBody ProjectDTO request) {
        return service.addProject(request);
    }

    @PutMapping("/{id}")
    public ProjectDTO updateProject(@PathVariable Long id, @Valid @RequestBody ProjectDTO request) {
        return service.updateProject(id, request);
    }

    @PatchMapping("/update-status/{id}")
    public ProjectDTO updateStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        return service.updateStatus(id, request.get("status"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable Long id) {
        service.deleteProject(id);
    }
}
