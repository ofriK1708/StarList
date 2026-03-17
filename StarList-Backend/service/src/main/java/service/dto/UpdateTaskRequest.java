package service.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import lombok.Builder;
import model.enums.DifficultyLevel;

/**
 * Partial-update request for a task. All fields are optional — only non-null fields are applied.
 * At least one field should be provided; an all-null body is a no-op.
 */
@Builder
public record UpdateTaskRequest(
        String title,
        String description,
        DifficultyLevel difficultyLevel,
        @Positive Integer durationMinutes,
        @Future Instant dueDate
) {}