package br.com.gfctech.project_manager.dto;

import br.com.gfctech.project_manager.enums.TaskStatus;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardDTO {
    private double totalHoursWorked;
    private double totalHoursMonth;
    private WeeklyHoursDTO weeklyHours;
    private long pendingTasksCount;
    private long completedTasksCount;
    private long tasksInProgress;
    private long projectsManaged;
    private long tasksCreated;
    private Long totalUsers;
    private Long totalProjects;
    private Long totalTasks;
    private Map<TaskStatus, Long> taskDistribution;
}
