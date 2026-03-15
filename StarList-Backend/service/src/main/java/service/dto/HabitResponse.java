package service.dto;

import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;
import model.domain.Habit;
import model.enums.DifficultyLevel;
import model.enums.HabitFrequency;

@Builder
public record HabitResponse(
        Long habitId,
        String title,
        String description,
        HabitFrequency frequency,
        DifficultyLevel difficultyLevel,
        Integer coinReward,
        Integer coinPenalty,
        Integer currentStreak,
        Integer bestStreak,
        Integer totalCompletions,
        LocalDate lastCompletedDate,
        Instant createdAt,
        Boolean isActive
) {

    public static HabitResponse from(Habit habit) {
        return HabitResponse.builder()
                .habitId(habit.getId())
                .title(habit.getTitle())
                .description(habit.getDescription())
                .frequency(habit.getFrequency())
                .difficultyLevel(habit.getDifficultyLevel())
                .coinReward(habit.getCoinReward())
                .coinPenalty(habit.getCoinPenalty())
                .currentStreak(habit.getCurrentStreak())
                .bestStreak(habit.getBestStreak())
                .totalCompletions(habit.getTotalCompletions())
                .lastCompletedDate(habit.getLastCompletedDate())
                .createdAt(habit.getCreatedAt())
                .isActive(habit.getIsActive())
                .build();
    }
}
