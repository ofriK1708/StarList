package service;

import model.enums.HabitFrequency;
import org.junit.jupiter.api.Test;
import repository.entity.HabitEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HabitPeriodCalculatorTest {

    private final HabitPeriodCalculator calc = new HabitPeriodCalculator();

    // ── currentPeriod — DAILY ──────────────────────────────────────────────────

    @Test
    void currentPeriod_daily_returnsTodayToToday() {
        HabitEntity h = dailyHabit();
        LocalDate today = LocalDate.of(2026, 8, 15);

        LocalDate[] period = calc.currentPeriod(h, today);

        assertThat(period[0]).isEqualTo(today);
        assertThat(period[1]).isEqualTo(today);
    }

    // ── currentPeriod — WEEKLY ─────────────────────────────────────────────────

    @Test
    void currentPeriod_weekly_returnsMondayToSundayOfCurrentWeek() {
        // Aug 10 2026 is a Monday; Aug 16 is Sunday
        HabitEntity h = weeklyHabit(3); // Wednesday
        LocalDate wednesday = LocalDate.of(2026, 8, 12);

        LocalDate[] period = calc.currentPeriod(h, wednesday);

        assertThat(period[0]).isEqualTo(LocalDate.of(2026, 8, 10)); // Monday
        assertThat(period[1]).isEqualTo(LocalDate.of(2026, 8, 16)); // Sunday
    }

    @Test
    void currentPeriod_weekly_onSundayReturnsCorrectWeek() {
        HabitEntity h = weeklyHabit(7); // Sunday
        LocalDate sunday = LocalDate.of(2026, 8, 16);

        LocalDate[] period = calc.currentPeriod(h, sunday);

        assertThat(period[0]).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(period[1]).isEqualTo(LocalDate.of(2026, 8, 16));
    }

    // ── currentPeriod — CUSTOM ─────────────────────────────────────────────────

    @Test
    void currentPeriod_custom14days_period0() {
        // createdAt Aug 1; interval 14; today Aug 10 → period 0: Aug 1–14
        HabitEntity h = customHabit(14, LocalDate.of(2026, 8, 1));
        LocalDate today = LocalDate.of(2026, 8, 10);

        LocalDate[] period = calc.currentPeriod(h, today);

        assertThat(period[0]).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(period[1]).isEqualTo(LocalDate.of(2026, 8, 14));
    }

    @Test
    void currentPeriod_custom14days_period1() {
        // createdAt Aug 1; interval 14; today Aug 20 → period 1: Aug 15–28
        HabitEntity h = customHabit(14, LocalDate.of(2026, 8, 1));
        LocalDate today = LocalDate.of(2026, 8, 20);

        LocalDate[] period = calc.currentPeriod(h, today);

        assertThat(period[0]).isEqualTo(LocalDate.of(2026, 8, 15));
        assertThat(period[1]).isEqualTo(LocalDate.of(2026, 8, 28));
    }

    @Test
    void currentPeriod_custom30days_onlyOnePeriodInMonth() {
        HabitEntity h = customHabit(30, LocalDate.of(2026, 8, 1));
        LocalDate today = LocalDate.of(2026, 8, 25);

        LocalDate[] period = calc.currentPeriod(h, today);

        assertThat(period[0]).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(period[1]).isEqualTo(LocalDate.of(2026, 8, 30));
    }

    // ── daysLate ──────────────────────────────────────────────────────────────
    // Reference week: Aug 10 2026 is a Monday, Aug 16 is the Sunday that ends it.

    @Test
    void daysLate_daily_returnsZero() {
        HabitEntity h = dailyHabit();
        assertThat(calc.daysLate(h, LocalDate.of(2026, 8, 15))).isZero();
    }

    @Test
    void daysLate_multiDay_onScheduledDay_returnsZero() {
        HabitEntity h = multiDayHabit(List.of(1, 3, 5)); // Mon, Wed, Fri
        LocalDate wednesday = LocalDate.of(2026, 8, 12);
        assertThat(calc.daysLate(h, wednesday)).isZero();
    }

    @Test
    void daysLate_multiDay_offScheduledDay_returnsZero() {
        HabitEntity h = multiDayHabit(List.of(1, 3, 5)); // Mon, Wed, Fri
        LocalDate tuesday = LocalDate.of(2026, 8, 11);
        assertThat(calc.daysLate(h, tuesday)).isZero();
    }

    @Test
    void daysLate_weekly_onScheduledDay_returnsZero() {
        HabitEntity h = weeklyHabit(3); // Wednesday
        LocalDate wednesday = LocalDate.of(2026, 8, 12);
        assertThat(calc.daysLate(h, wednesday)).isZero();
    }

    @Test
    void daysLate_weekly_beforeScheduledDay_returnsZero() {
        HabitEntity h = weeklyHabit(5); // Friday
        LocalDate tuesday = LocalDate.of(2026, 8, 11);
        assertThat(calc.daysLate(h, tuesday)).isZero();
    }

    @Test
    void daysLate_weekly_twoDaysAfterScheduledDay_returnsTwo() {
        HabitEntity h = weeklyHabit(3); // Wednesday
        LocalDate friday = LocalDate.of(2026, 8, 14);
        assertThat(calc.daysLate(h, friday)).isEqualTo(2);
    }

    @Test
    void daysLate_weekly_scheduledSunday_neverLate() {
        // Sunday closes the ISO week, so there is no day left to be late on.
        HabitEntity h = weeklyHabit(7);
        LocalDate sunday = LocalDate.of(2026, 8, 16);
        assertThat(calc.daysLate(h, sunday)).isZero();
    }

    @Test
    void daysLate_weekly_nullScheduledDay_returnsZero() {
        HabitEntity h = weeklyHabit(null);
        LocalDate friday = LocalDate.of(2026, 8, 14);
        assertThat(calc.daysLate(h, friday)).isZero();
    }

    @Test
    void daysLate_custom_beforeDueWeekdayInWindow_returnsZero() {
        // Period is Aug 1–Aug 30; due = last Wednesday on/before Aug 30 = Aug 26.
        HabitEntity h = customHabit(30, LocalDate.of(2026, 8, 1), 3);
        assertThat(calc.daysLate(h, LocalDate.of(2026, 8, 20))).isZero();
    }

    @Test
    void daysLate_custom_afterDueWeekdayInWindow_returnsDaysSinceDue() {
        // Due Aug 26, completing Aug 29 → 3 days late.
        HabitEntity h = customHabit(30, LocalDate.of(2026, 8, 1), 3);
        assertThat(calc.daysLate(h, LocalDate.of(2026, 8, 29))).isEqualTo(3);
    }

    @Test
    void daysLate_custom_nullScheduledDay_returnsZero() {
        HabitEntity h = customHabit(30, LocalDate.of(2026, 8, 1), null);
        assertThat(calc.daysLate(h, LocalDate.of(2026, 8, 29))).isZero();
    }

    // ── periodsForMonth — DAILY ────────────────────────────────────────────────

    @Test
    void periodsForMonth_daily_returnsOnePeriodPerDay() {
        HabitEntity h = dailyHabit();
        YearMonth feb2026 = YearMonth.of(2026, 2);

        List<LocalDate[]> periods = calc.periodsForMonth(h, feb2026);

        assertThat(periods).hasSize(28);
        assertThat(periods.get(0)[0]).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(periods.get(0)[1]).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(periods.get(27)[0]).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    // ── periodsForMonth — WEEKLY ───────────────────────────────────────────────

    @Test
    void periodsForMonth_weekly_returnsOneEntryPerOccurrenceOfScheduledDayInMonth() {
        // Mondays in August 2026: 3, 10, 17, 24, 31 → 5 Mondays
        HabitEntity h = weeklyHabit(1); // Monday
        YearMonth aug2026 = YearMonth.of(2026, 8);

        List<LocalDate[]> periods = calc.periodsForMonth(h, aug2026);

        assertThat(periods).hasSize(5);
        // First Monday Aug 3 → ISO week: Aug 3 (Mon) – Aug 9 (Sun)
        assertThat(periods.get(0)[0]).isEqualTo(LocalDate.of(2026, 8, 3));
        assertThat(periods.get(0)[1]).isEqualTo(LocalDate.of(2026, 8, 9));
        // Second Monday Aug 10 → Aug 10–16
        assertThat(periods.get(1)[0]).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(periods.get(1)[1]).isEqualTo(LocalDate.of(2026, 8, 16));
    }

    @Test
    void periodsForMonth_weekly_4occurrencesWhenScheduledDayNotInFirstWeek() {
        // Sundays in Feb 2026: 1, 8, 15, 22 → 4 Sundays
        HabitEntity h = weeklyHabit(7); // Sunday
        YearMonth feb2026 = YearMonth.of(2026, 2);

        List<LocalDate[]> periods = calc.periodsForMonth(h, feb2026);

        assertThat(periods).hasSize(4);
    }

    // ── periodsForMonth — CUSTOM ───────────────────────────────────────────────

    @Test
    void periodsForMonth_custom14days_returnsThreePeriodsInAugust() {
        // createdAt Aug 1; interval 14 → periods starting Aug 1, Aug 15, Aug 29 (all start in August)
        HabitEntity h = customHabit(14, LocalDate.of(2026, 8, 1));
        YearMonth aug2026 = YearMonth.of(2026, 8);

        List<LocalDate[]> periods = calc.periodsForMonth(h, aug2026);

        assertThat(periods).hasSize(3);
        assertThat(periods.get(0)[0]).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(periods.get(0)[1]).isEqualTo(LocalDate.of(2026, 8, 14));
        assertThat(periods.get(1)[0]).isEqualTo(LocalDate.of(2026, 8, 15));
        assertThat(periods.get(1)[1]).isEqualTo(LocalDate.of(2026, 8, 28));
        assertThat(periods.get(2)[0]).isEqualTo(LocalDate.of(2026, 8, 29));
        assertThat(periods.get(2)[1]).isEqualTo(LocalDate.of(2026, 9, 11));
    }

    @Test
    void periodsForMonth_custom30days_returnsTwoPeriodsInAugust() {
        // createdAt Aug 1; interval 30 → periods starting Aug 1 and Aug 31 (both start in August)
        HabitEntity h = customHabit(30, LocalDate.of(2026, 8, 1));
        YearMonth aug2026 = YearMonth.of(2026, 8);

        List<LocalDate[]> periods = calc.periodsForMonth(h, aug2026);

        assertThat(periods).hasSize(2);
        assertThat(periods.get(0)[0]).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(periods.get(0)[1]).isEqualTo(LocalDate.of(2026, 8, 30));
        assertThat(periods.get(1)[0]).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(periods.get(1)[1]).isEqualTo(LocalDate.of(2026, 9, 29));
    }

    @Test
    void periodsForMonth_custom7days_returnsFivePeriods() {
        // createdAt Aug 1; interval 7 → periods starting Aug 1, 8, 15, 22, 29 (all in August)
        HabitEntity h = customHabit(7, LocalDate.of(2026, 8, 1));
        YearMonth aug2026 = YearMonth.of(2026, 8);

        List<LocalDate[]> periods = calc.periodsForMonth(h, aug2026);

        assertThat(periods).hasSize(5);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private HabitEntity dailyHabit() {
        return HabitEntity.builder()
                .id(1L)
                .frequency(HabitFrequency.DAILY)
                .createdAt(Instant.parse("2026-08-01T00:00:00Z"))
                .build();
    }

    private HabitEntity weeklyHabit(Integer scheduledDayOfWeek) {
        return HabitEntity.builder()
                .id(1L)
                .frequency(HabitFrequency.WEEKLY)
                .scheduledDayOfWeek(scheduledDayOfWeek)
                .createdAt(Instant.parse("2026-08-01T00:00:00Z"))
                .build();
    }

    private HabitEntity customHabit(int intervalDays, LocalDate createdDate) {
        return customHabit(intervalDays, createdDate, null);
    }

    private HabitEntity customHabit(int intervalDays, LocalDate createdDate, Integer scheduledDayOfWeek) {
        return HabitEntity.builder()
                .id(1L)
                .frequency(HabitFrequency.CUSTOM)
                .customIntervalDays(intervalDays)
                .scheduledDayOfWeek(scheduledDayOfWeek)
                .createdAt(createdDate.atStartOfDay().toInstant(ZoneOffset.UTC))
                .build();
    }

    private HabitEntity multiDayHabit(List<Integer> scheduledDaysOfWeek) {
        return HabitEntity.builder()
                .id(1L)
                .frequency(HabitFrequency.MULTI_DAY)
                .scheduledDaysOfWeek(scheduledDaysOfWeek)
                .createdAt(Instant.parse("2026-08-01T00:00:00Z"))
                .build();
    }
}
