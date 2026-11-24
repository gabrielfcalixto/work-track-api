package br.com.gfctech.project_manager.service;

import br.com.gfctech.project_manager.dto.DashboardDTO;
import br.com.gfctech.project_manager.dto.WeeklyHoursDTO;
import br.com.gfctech.project_manager.entity.UserEntity.Role;
import br.com.gfctech.project_manager.enums.TaskStatus;
import br.com.gfctech.project_manager.exceptions.ApiException;
import br.com.gfctech.project_manager.repository.ProjectRepository;
import br.com.gfctech.project_manager.repository.TaskRepository;
import br.com.gfctech.project_manager.repository.TimeEntryRepository;
import br.com.gfctech.project_manager.repository.UserRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private static final BigDecimal DEFAULT_WEEKLY_GOAL_HOURS = BigDecimal.valueOf(40);

    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final TimeEntryRepository entries;
    private final UserRepository users;

    public DashboardService(ProjectRepository projects, TaskRepository tasks,
                            TimeEntryRepository entries, UserRepository users) {
        this.projects = projects;
        this.tasks = tasks;
        this.entries = entries;
        this.users = users;
    }

    public DashboardDTO getDashboardData(Long userId, Role role) {
        var result = new DashboardDTO();
        var distribution = getTaskDistribution(userId);
        result.setTaskDistribution(distribution);
        result.setTotalHoursWorked(getUserHours(userId));
        result.setTotalHoursMonth(getTotalHoursMonth(userId));
        result.setWeeklyHours(getWeeklyHours(userId));
        result.setPendingTasksCount(distribution.get(TaskStatus.NAO_INICIADA));
        result.setCompletedTasksCount(distribution.get(TaskStatus.CONCLUIDA));
        result.setTasksInProgress(distribution.get(TaskStatus.EM_ANDAMENTO));
        if (role != Role.USER) {
            result.setProjectsManaged(countProjectsByManager(userId));
            result.setTasksCreated(tasks.countByProjectManagerId(userId));
        }
        if (role == Role.ADMIN) {
            result.setTotalUsers(users.count());
            result.setTotalProjects(projects.count());
            result.setTotalTasks(tasks.count());
        }
        return result;
    }

    @Transactional
    public void updateWeeklyGoal(Long userId, BigDecimal hours) {
        var user = users.lockById(userId).orElseThrow(() -> ApiException.notFound("Usuário não encontrado."));
        user.setWeeklyGoalHours(hours);
    }

    private WeeklyHoursDTO getWeeklyHours(Long userId) {
        LocalDate start = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate end = start.plusWeeks(1);
        var dailyHours = entries.sumHoursByDate(userId, start, end).stream().collect(Collectors.toMap(
                TimeEntryRepository.DailyHours::getEntryDate, TimeEntryRepository.DailyHours::getLoggedHours));
        var days = start.datesUntil(end)
                .map(date -> new WeeklyHoursDTO.Day(date, dailyHours.getOrDefault(date, 0.0)))
                .toList();
        double total = days.stream().mapToDouble(WeeklyHoursDTO.Day::hours).sum();
        var goal = users.findWeeklyGoalHours(userId).orElse(DEFAULT_WEEKLY_GOAL_HOURS);
        return new WeeklyHoursDTO(start, end.minusDays(1), days, total, goal);
    }

    public double getUserHours(Long userId) {
        return entries.sumHoursByUser(userId);
    }

    public double getTotalHoursMonth(Long userId) {
        var month = YearMonth.now();
        return entries.sumHoursInPeriod(userId, month.atDay(1), month.plusMonths(1).atDay(1));
    }

    public long countProjectsByManager(Long userId) {
        return projects.countByManagerId(userId);
    }

    public long countTasks(Long userId, TaskStatus status) {
        return getTaskDistribution(userId).get(status);
    }

    public Map<TaskStatus, Long> getTaskDistribution(Long userId) {
        var distribution = new EnumMap<TaskStatus, Long>(TaskStatus.class);
        for (var status : TaskStatus.values()) {
            distribution.put(status, 0L);
        }
        for (var row : tasks.countTasksByStatus(userId)) {
            distribution.put((TaskStatus) row[0], ((Number) row[1]).longValue());
        }
        return distribution;
    }
}
