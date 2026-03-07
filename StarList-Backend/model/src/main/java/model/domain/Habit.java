package model.domain;

import java.time.Instant;
import java.time.LocalDate;
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
public class Habit {

    private Long id;
    private Long userId;
    private String title;
    private String description;
    private HabitFrequency frequency;
    private DifficultyLevel difficultyLevel;
    private Integer coinReward;
    private Integer coinPenalty;
    private Integer currentStreak;
    private Integer bestStreak;
    private Integer totalCompletions;
    private LocalDate lastCompletedDate;
    private Instant createdAt;
    private Boolean isActive;
    private Instant deletedAt;
}
