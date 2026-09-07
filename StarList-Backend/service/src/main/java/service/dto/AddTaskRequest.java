package service.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Builder;
import model.enums.DifficultyLevel;

@Builder
public record AddTaskRequest(
        @NotBlank @Size(max = 120) String title,
        @Size(max = 1000) String description,
        @NotNull DifficultyLevel difficultyLevel,
        @PositiveOrZero Integer durationMinutes,
        Instant dueDate
) {}
