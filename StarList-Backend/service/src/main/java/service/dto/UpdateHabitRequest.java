package service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import model.enums.DifficultyLevel;
import model.enums.HabitFrequency;

@Builder
public record UpdateHabitRequest(
        @NotBlank String title,
        String description,
        @NotNull HabitFrequency frequency,
        @NotNull DifficultyLevel difficultyLevel
) {}
