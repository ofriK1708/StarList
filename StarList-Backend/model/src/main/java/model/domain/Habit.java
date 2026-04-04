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

    @Builder.Default
    private Integer coinReward = 0;

    private Integer coinPenalty;

    @Builder.Default
    private Integer currentStreak = 0;

    @Builder.Default
    private Integer bestStreak = 0;

    @Builder.Default
    private Integer totalCompletions = 0;

    private LocalDate lastCompletedDate;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Boolean isActive = Boolean.TRUE;

    private Instant deletedAt;
}