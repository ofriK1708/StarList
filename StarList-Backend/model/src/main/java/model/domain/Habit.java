package model.domain;

import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.enums.DifficultyLevel;
import model.enums.HabitFrequency;
import model.enums.ScheduledTimeType;

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

    /**
     * ISO day of week (1 = Monday … 7 = Sunday) on which the habit is intended to be done.
     * Required for WEEKLY and CUSTOM frequency; null for DAILY.
     */
    private Integer scheduledDayOfWeek;

    /**
     * Preferred time of day. Informational only — does not gate completion.
     * Present for WEEKLY and CUSTOM; null for DAILY.
     */
    private ScheduledTimeType scheduledTimeType;

    /**
     * Hour of day (0–23) when {@code scheduledTimeType} is {@link ScheduledTimeType#CUSTOM}.
     * Null otherwise.
     */
    private Integer scheduledHour;

    /**
     * Interval in days for CUSTOM frequency. Valid values: 7, 14, 30.
     * Null for DAILY and WEEKLY.
     */
    private Integer customIntervalDays;

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