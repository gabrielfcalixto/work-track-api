package br.com.gfctech.project_manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record TimeEntryRequest(@Positive Long taskId, Long userId,
                               @NotBlank(message = "Informe a descrição do lançamento.")
                               @Size(max = 255, message = "A descrição deve ter até 255 caracteres.") String description,
                               @NotNull(message = "Informe a data do lançamento.")
                               @PastOrPresent(message = "A data do lançamento deve ser hoje ou anterior.") LocalDate entryDate,
                               @NotNull(message = "Informe o horário inicial.") LocalTime startTime,
                               @NotNull(message = "Informe o horário final.") LocalTime endTime) {
}
