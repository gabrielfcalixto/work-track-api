package br.com.gfctech.project_manager.dto;

import br.com.gfctech.project_manager.entity.TimeEntryEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;

public record TimeEntryDTO(Long id, Long taskId, String taskName, Long userId, String description,
                           LocalDate entryDate, @JsonFormat(pattern = "HH:mm") LocalTime startTime,
                           @JsonFormat(pattern = "HH:mm") LocalTime endTime, double hoursLogged) {
    public TimeEntryDTO(TimeEntryEntity entry) {
        this(entry.getId(), entry.getTaskEntity().getId(), entry.getTaskEntity().getName(),
                entry.getUser().getId(), entry.getDescription(), entry.getEntryDate(),
                entry.getStartTime(), entry.getEndTime(), entry.getHoursLogged());
    }
}
