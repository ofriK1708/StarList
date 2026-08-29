package service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Builder;
import model.enums.DifficultyLevel;
import model.enums.HabitFrequency;
import model.enums.ScheduledTimeType;

@Builder
public record AddHabitRequest(
        @NotBlank String title,
        String description,
        @NotNull HabitFrequency frequency,
        @NotNull DifficultyLevel difficultyLevel,

        /** Required when frequency is WEEKLY or CUSTOM. ISO day: 1 = Monday … 7 = Sunday. */
        @Min(1) @Max(7) Integer scheduledDayOfWeek,

        /** Optional for WEEKLY and CUSTOM. Informational only. */
        ScheduledTimeType scheduledTimeType,

        /** Required when scheduledTimeType is CUSTOM. */
        @Min(0) @Max(23) Integer scheduledHour,

        /** Required when frequency is CUSTOM. Must be one of: 7, 14, 30. */
        Integer customIntervalDays,

        /** Required when frequency is MULTI_DAY. ISO days of week (1=Mon…7=Sun); 2–6 values. */
        List<Integer> scheduledDaysOfWeek
) {}
