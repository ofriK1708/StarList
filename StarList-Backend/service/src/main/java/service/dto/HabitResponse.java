package service.dto;

import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;
import model.domain.Habit;
import model.enums.CompletionStatus;
import model.enums.DifficultyLevel;
import model.enums.HabitFrequency;
import model.enums.ScheduledTimeType;
import java.util.List;

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
        Boolean isActive,
        /** ISO day of week (1=Mon…7=Sun). Present for WEEKLY and CUSTOM; null for DAILY. */
        Integer scheduledDayOfWeek,
        ScheduledTimeType scheduledTimeType,
        Integer scheduledHour,
        Integer customIntervalDays,
        /** ISO days of week (1=Mon…7=Sun). Present for MULTI_DAY; null for other frequencies. */
        List<Integer> scheduledDaysOfWeek,
        /** "day" for DAILY and MULTI_DAY, "week" for WEEKLY, "period" for CUSTOM. */
        String streakUnit,
        List<CompletionStatus> monthCompletions,
        /** Current-period state; {@code null} on mutation responses, which have no period context. */
        HabitPeriodStatus periodStatus
) {

    /** Factory for mutation responses (create, update) — {@code monthCompletions} is {@code null}. */
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
                .scheduledDayOfWeek(habit.getScheduledDayOfWeek())
                .scheduledTimeType(habit.getScheduledTimeType())
                .scheduledHour(habit.getScheduledHour())
                .customIntervalDays(habit.getCustomIntervalDays())
                .scheduledDaysOfWeek(habit.getScheduledDaysOfWeek())
                .streakUnit(streakUnitFor(habit.getFrequency()))
                .build();
    }

    /** Factory for GET responses — includes pre-computed per-period completion statuses for the month. */
    public static HabitResponse from(Habit habit, List<CompletionStatus> monthCompletions) {
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
                .scheduledDayOfWeek(habit.getScheduledDayOfWeek())
                .scheduledTimeType(habit.getScheduledTimeType())
                .scheduledHour(habit.getScheduledHour())
                .customIntervalDays(habit.getCustomIntervalDays())
                .scheduledDaysOfWeek(habit.getScheduledDaysOfWeek())
                .streakUnit(streakUnitFor(habit.getFrequency()))
                .monthCompletions(monthCompletions)
                .build();
    }

    /** Factory for GET responses that also carry current-period state for the AI assistant. */
    public static HabitResponse from(Habit habit, List<CompletionStatus> monthCompletions,
                                     HabitPeriodStatus periodStatus) {
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
                .scheduledDayOfWeek(habit.getScheduledDayOfWeek())
                .scheduledTimeType(habit.getScheduledTimeType())
                .scheduledHour(habit.getScheduledHour())
                .customIntervalDays(habit.getCustomIntervalDays())
                .scheduledDaysOfWeek(habit.getScheduledDaysOfWeek())
                .streakUnit(streakUnitFor(habit.getFrequency()))
                .monthCompletions(monthCompletions)
                .periodStatus(periodStatus)
                .build();
    }

    private static String streakUnitFor(HabitFrequency frequency) {
        if (frequency == null) return "day";
        return switch (frequency) {
            case WEEKLY -> "week";
            case CUSTOM -> "period";
            case MULTI_DAY, DAILY -> "day";
        };
    }
}
