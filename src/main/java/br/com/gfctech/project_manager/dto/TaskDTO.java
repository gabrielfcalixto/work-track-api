package br.com.gfctech.project_manager.dto;

import jakarta.validation.constraints.*;

import br.com.gfctech.project_manager.entity.TaskEntity;
import br.com.gfctech.project_manager.entity.UserEntity;
import br.com.gfctech.project_manager.enums.TaskPriority;
import br.com.gfctech.project_manager.enums.TaskStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {
    private Long id;
    @NotBlank(message = "Informe o nome da tarefa.") @Size(max = 255)
    private String name;
    @NotBlank(message = "Informe a descrição da tarefa.") @Size(max = 255)
    private String description;
    @NotNull @PositiveOrZero
    private Double estimatedHours;
    @NotNull
    private TaskStatus status;
    @NotNull
    private TaskPriority priority;
    private Double totalHours;
    @NotNull @Positive
    private Long projectId;
    private Set<@NotNull @Positive Long> assignedUserIds;
    @NotNull(message = "Informe a data de início.")
    private LocalDate startDate;
    @NotNull(message = "Informe a data limite.")
    private LocalDate deadline;

    public TaskDTO(TaskEntity taskEntity) {
        this.id = taskEntity.getId();
        this.name = taskEntity.getName();
        this.description = taskEntity.getDescription();
        this.estimatedHours = taskEntity.getEstimatedHours();
        this.status = taskEntity.getStatus();
        this.priority = taskEntity.getPriority();
        this.totalHours = taskEntity.getTotalHours() == null ? 0.0 : taskEntity.getTotalHours();
        this.projectId = taskEntity.getProject().getId();
        this.assignedUserIds = taskEntity.getAssignedUsers().stream()
                                         .map(UserEntity::getId)
                                         .collect(Collectors.toSet());
        this.startDate = taskEntity.getStartDate();
        this.deadline = taskEntity.getDeadline();
    }
}
