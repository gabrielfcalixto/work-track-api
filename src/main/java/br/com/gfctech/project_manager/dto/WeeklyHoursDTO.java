package br.com.gfctech.project_manager.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record WeeklyHoursDTO(LocalDate weekStart, LocalDate weekEnd, List<Day> days,
                             double totalHours, BigDecimal goalHours) {
    public record Day(LocalDate date, double hours) {}
}
