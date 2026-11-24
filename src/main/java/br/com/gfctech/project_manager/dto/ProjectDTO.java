package br.com.gfctech.project_manager.dto;

import jakarta.validation.constraints.*;

import br.com.gfctech.project_manager.entity.ProjectEntity;
import br.com.gfctech.project_manager.enums.ProjectStatus;
import lombok.*;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDTO {
    private Long id;
    @NotBlank(message = "Informe o nome do projeto.") @Size(max = 255)
    private String name;
    @NotBlank(message = "Informe a descrição do projeto.") @Size(max = 255)
    private String description;
    @NotNull @PositiveOrZero
    private Float hours;
    @NotNull
    private ProjectStatus status;
    @NotNull @Positive
    private Long managerId;
    @NotNull @Positive
    private Long clientId;
    private Set<@NotNull @Positive Long> teamMemberIds;
    @NotNull(message = "Informe a data de início.")
    private LocalDate startDate;
    @NotNull(message = "Informe a data limite.")
    private LocalDate deadline;

    public ProjectDTO(ProjectEntity projectEntity) {
        this.id = projectEntity.getId();
        this.name = projectEntity.getName();
        this.description = projectEntity.getDescription();
        this.hours = projectEntity.getHours();
        this.status = projectEntity.getStatus();
        this.managerId = projectEntity.getManager() != null ? projectEntity.getManager().getId() : null;
        this.clientId = projectEntity.getClient() != null ? projectEntity.getClient().getId() : null;
        this.teamMemberIds = projectEntity.getTeamMembers() != null 
                ? projectEntity.getTeamMembers().stream().map(member -> member.getId()).collect(Collectors.toSet()) 
                : null;
        this.startDate = projectEntity.getStartDate();
        this.deadline = projectEntity.getDeadline();
    }
}
