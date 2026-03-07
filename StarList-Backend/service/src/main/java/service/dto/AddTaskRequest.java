package service.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.enums.DifficultyLevel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddTaskRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private DifficultyLevel difficultyLevel;

    @Positive
    private Integer durationMinutes;

    @Future
    private Instant dueDate;
}
