package service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.enums.DifficultyLevel;
import model.enums.HabitFrequency;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHabitRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private HabitFrequency frequency;

    @NotNull
    private DifficultyLevel difficultyLevel;
}
