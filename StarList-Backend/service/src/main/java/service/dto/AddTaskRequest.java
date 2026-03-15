package service.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import lombok.Builder;
import model.enums.DifficultyLevel;

@Builder
public record AddTaskRequest(
        @NotBlank String title,
        String description,
        @NotNull DifficultyLevel difficultyLevel,
        @Positive Integer durationMinutes,
        @Future Instant dueDate
) {}
