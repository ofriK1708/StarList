package service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import model.enums.HabitFrequency;
import org.springframework.stereotype.Component;
import repository.entity.HabitEntity;

@Component
public class HabitPeriodCalculator {

    /**
     * Returns the start and end of the current completion period for the given habit.
     * <ul>
     *   <li>DAILY — {@code [today, today]}</li>
     *   <li>WEEKLY — {@code [Monday of today's ISO week, Sunday of today's ISO week]}</li>
     *   <li>CUSTOM — interval window anchored to the habit's {@code createdAt} date</li>
     * </ul>
     *
     * @return a two-element array {@code [periodStart, periodEnd]} (inclusive on both ends)
     */
    public LocalDate[] currentPeriod(HabitEntity habit, LocalDate today) {
        return switch (habit.getFrequency()) {
            case DAILY -> new LocalDate[]{today, today};
            case WEEKLY -> {
                LocalDate monday = today.with(DayOfWeek.MONDAY);
                yield new LocalDate[]{monday, monday.plusDays(6)};
            }
            case CUSTOM -> customPeriodContaining(habit, today);
            // For MULTI_DAY, a "current period" only exists if today is one of the scheduled days.
            // Returns [today, today] when applicable; otherwise null signals no active period.
            case MULTI_DAY -> {
                List<Integer> days = habit.getScheduledDaysOfWeek();
                int todayIso = today.getDayOfWeek().getValue();
                yield (days != null && days.contains(todayIso))
                        ? new LocalDate[]{today, today}
                        : null;
            }
        };
    }

    /**
     * Returns true if the habit is WEEKLY and today falls after the scheduled day of week
     * in the current ISO week — meaning the completion is "late" and incurs a penalty.
     * Always returns false for DAILY and CUSTOM habits.
     */
    /**
     * Returns true if the habit is WEEKLY and today falls after the scheduled day of week
     * in the current ISO week — meaning the completion is "late" and incurs a penalty.
     * Always returns false for DAILY, CUSTOM, and MULTI_DAY habits.
     */
    public boolean isLateCompletion(HabitEntity habit, LocalDate today) {
        if (habit.getFrequency() != HabitFrequency.WEEKLY) return false;
        int todayIso = today.getDayOfWeek().getValue(); // 1=Mon … 7=Sun
        return todayIso > habit.getScheduledDayOfWeek();
    }

    /**
     * Returns a list of {@code [periodStart, periodEnd]} pairs for every expected completion
     * slot whose anchor date falls within {@code yearMonth}. The length of this list equals
     * the number of circle segments shown in the frontend.
     *
     * <ul>
     *   <li>DAILY — one pair per calendar day in the month</li>
     *   <li>WEEKLY — one pair per occurrence of {@code scheduledDayOfWeek} in the month;
     *       each pair spans the full ISO week containing that day</li>
     *   <li>CUSTOM — one pair per interval period whose start date falls in the month</li>
     * </ul>
     */
    public List<LocalDate[]> periodsForMonth(HabitEntity habit, YearMonth yearMonth) {
        return switch (habit.getFrequency()) {
            case DAILY -> dailyPeriodsForMonth(yearMonth);
            case WEEKLY -> weeklyPeriodsForMonth(habit, yearMonth);
            case CUSTOM -> customPeriodsForMonth(habit, yearMonth);
            // MULTI_DAY: one [day, day] segment for every occurrence of every selected weekday in the month.
            // Days within a week are emitted in day-of-month order (Monday before Wednesday, etc.).
            case MULTI_DAY -> multiDayPeriodsForMonth(habit, yearMonth);
        };
    }

    // ── private helpers ────────────────────────────────────────────────────────

    private LocalDate[] customPeriodContaining(HabitEntity habit, LocalDate today) {
        LocalDate anchor = createdDate(habit);
        long daysSince = ChronoUnit.DAYS.between(anchor, today);
        long periodIndex = daysSince / habit.getCustomIntervalDays();
        LocalDate start = anchor.plusDays(periodIndex * habit.getCustomIntervalDays());
        LocalDate end = start.plusDays(habit.getCustomIntervalDays() - 1);
        return new LocalDate[]{start, end};
    }

    private List<LocalDate[]> dailyPeriodsForMonth(YearMonth yearMonth) {
        List<LocalDate[]> result = new ArrayList<>(yearMonth.lengthOfMonth());
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate d = yearMonth.atDay(day);
            result.add(new LocalDate[]{d, d});
        }
        return result;
    }

    private List<LocalDate[]> weeklyPeriodsForMonth(HabitEntity habit, YearMonth yearMonth) {
        List<LocalDate[]> result = new ArrayList<>(5);
        DayOfWeek targetDay = DayOfWeek.of(habit.getScheduledDayOfWeek());
        // First occurrence of the scheduled day in the month
        LocalDate first = yearMonth.atDay(1).with(TemporalAdjusters.nextOrSame(targetDay));
        while (!first.isAfter(yearMonth.atEndOfMonth())) {
            LocalDate monday = first.with(DayOfWeek.MONDAY);
            result.add(new LocalDate[]{monday, monday.plusDays(6)});
            first = first.plusWeeks(1);
        }
        return result;
    }

    private List<LocalDate[]> customPeriodsForMonth(HabitEntity habit, YearMonth yearMonth) {
        List<LocalDate[]> result = new ArrayList<>(4);
        LocalDate anchor = createdDate(habit);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        // Advance anchor by multiples of intervalDays until we reach periods in the month
        long intervalDays = habit.getCustomIntervalDays();
        long startIndex = 0;
        if (anchor.isBefore(monthStart)) {
            long daysBehind = ChronoUnit.DAYS.between(anchor, monthStart);
            startIndex = daysBehind / intervalDays;
        }

        LocalDate periodStart = anchor.plusDays(startIndex * intervalDays);
        while (!periodStart.isAfter(monthEnd)) {
            LocalDate periodEnd = periodStart.plusDays(intervalDays - 1);
            result.add(new LocalDate[]{periodStart, periodEnd});
            periodStart = periodStart.plusDays(intervalDays);
        }
        return result;
    }

    private List<LocalDate[]> multiDayPeriodsForMonth(HabitEntity habit, YearMonth yearMonth) {
        List<Integer> scheduledDays = habit.getScheduledDaysOfWeek();
        if (scheduledDays == null || scheduledDays.isEmpty()) return Collections.emptyList();

        List<LocalDate[]> result = new ArrayList<>();
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            if (scheduledDays.contains(date.getDayOfWeek().getValue())) {
                result.add(new LocalDate[]{date, date});
            }
        }
        return result;
    }

    private LocalDate createdDate(HabitEntity habit) {
        Instant createdAt = habit.getCreatedAt() != null ? habit.getCreatedAt() : Instant.now();
        return createdAt.atZone(ZoneOffset.UTC).toLocalDate();
    }
}
