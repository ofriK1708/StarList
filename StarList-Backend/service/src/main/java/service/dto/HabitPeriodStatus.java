package service.dto;

import java.time.LocalDate;
import lombok.Builder;

/**
 * Pre-computed state of a habit's <em>current</em> completion period.
 *
 * <p>Exists so the AI assistant never has to do calendar arithmetic. Handing the model a
 * {@code lastCompletedDate} and expecting it to work out whether that falls inside the current
 * ISO week is exactly how it ended up telling users to complete habits they had already done.
 *
 * @param periodStart         first day of the current period, or {@code null} when the habit is
 *                            not scheduled today (MULTI_DAY on an off-day)
 * @param periodEnd           last day of the current period, or {@code null} as above
 * @param dueDate             day the habit is expected to be completed by; equals {@code periodEnd}
 *                            for DAILY and MULTI_DAY
 * @param scheduledToday      whether the habit has an active period today at all
 * @param completedThisPeriod whether a completion already exists inside the current period
 * @param daysUntilDue        days from today to {@code dueDate}; negative once past due
 * @param daysLate            days elapsed since {@code dueDate}; {@code 0} when on time
 */
@Builder
public record HabitPeriodStatus(
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate dueDate,
        boolean scheduledToday,
        boolean completedThisPeriod,
        int daysUntilDue,
        int daysLate
) {
}
