package service.dto;

import lombok.Builder;
import model.enums.DifficultyLevel;
import model.enums.HabitFrequency;

/**
 * Partial-update request for a habit. All fields are optional — only non-null fields are applied.
 * At least one field should be provided; an all-null body is a no-op.
 */
@Builder
public record UpdateHabitRequest(
        String title,
        String description,
        HabitFrequency frequency,
        DifficultyLevel difficultyLevel
) {}
