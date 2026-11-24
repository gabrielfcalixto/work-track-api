package br.com.gfctech.project_manager.controller;

import br.com.gfctech.project_manager.dto.DashboardDTO;
import br.com.gfctech.project_manager.enums.TaskStatus;
import br.com.gfctech.project_manager.service.DashboardService;
import br.com.gfctech.project_manager.service.UserDetailsImpl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {
    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping
    public DashboardDTO getDashboardData(@AuthenticationPrincipal UserDetailsImpl user) {
        return dashboard.getDashboardData(user.getId(), user.getRole());
    }

    @PutMapping("/weekly-goal")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateWeeklyGoal(@AuthenticationPrincipal UserDetailsImpl user,
                                 @Valid @RequestBody WeeklyGoalRequest request) {
        dashboard.updateWeeklyGoal(user.getId(), request.hours());
    }

    public record WeeklyGoalRequest(
            @NotNull(message = "Informe a meta semanal.")
            @Min(value = 1, message = "A meta semanal deve ser de pelo menos 1 hora.")
            @Max(value = 168, message = "A meta semanal deve ser de até 168 horas.")
            @Digits(integer = 3, fraction = 2, message = "Use até duas casas decimais na meta semanal.")
            BigDecimal hours) {}

    @GetMapping("/user-hours/{userId}")
    @PreAuthorize("#userId == principal.id or hasAuthority('ADMIN')")
    public double getUserHours(@PathVariable Long userId) {
        return dashboard.getUserHours(userId);
    }

    @GetMapping("/user-hours-month/{userId}")
    @PreAuthorize("#userId == principal.id or hasAuthority('ADMIN')")
    public double getTotalHoursMonth(@PathVariable Long userId) {
        return dashboard.getTotalHoursMonth(userId);
    }

    @GetMapping("/tasks/pending/{userId}")
    @PreAuthorize("#userId == principal.id or hasAuthority('ADMIN')")
    public long getPendingTasksCount(@PathVariable Long userId) {
        return dashboard.countTasks(userId, TaskStatus.NAO_INICIADA);
    }

    @GetMapping("/tasks/completed/{userId}")
    @PreAuthorize("#userId == principal.id or hasAuthority('ADMIN')")
    public long getCompletedTasksCount(@PathVariable Long userId) {
        return dashboard.countTasks(userId, TaskStatus.CONCLUIDA);
    }

    @GetMapping("/tasks/ongoing/{userId}")
    @PreAuthorize("#userId == principal.id or hasAuthority('ADMIN')")
    public long getOngoingTasksCount(@PathVariable Long userId) {
        return dashboard.countTasks(userId, TaskStatus.EM_ANDAMENTO);
    }

    @GetMapping("/tasks/distribution/{userId}")
    @PreAuthorize("#userId == principal.id or hasAuthority('ADMIN')")
    public Map<TaskStatus, Long> getTaskDistribution(@PathVariable Long userId) {
        return dashboard.getTaskDistribution(userId);
    }

    @GetMapping("/projects/managed/{managerId}")
    @PreAuthorize("#managerId == principal.id or hasAuthority('ADMIN')")
    public long countProjectsByManager(@PathVariable Long managerId) {
        return dashboard.countProjectsByManager(managerId);
    }
}
