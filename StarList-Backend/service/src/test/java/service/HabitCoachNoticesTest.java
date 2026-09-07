package service;

import java.time.LocalDate;
import java.util.Optional;
import model.enums.HabitFrequency;
import org.junit.jupiter.api.Test;
import service.dto.HabitPeriodStatus;
import service.dto.HabitResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-function tests for the coach-notice policy — no Spring, no mocks.
 *
 * <p>Assertions deliberately constrain structure rather than exact wording, so the tone of the
 * assistant can be tuned without rewriting these tests.
 */
class HabitCoachNoticesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 12); // a Wednesday

    // ── silence ───────────────────────────────────────────────────────────────

    @Test
    void forHabit_completedThisPeriod_returnsEmpty() {
        HabitResponse habit = habit("Run", status(TODAY.minusDays(2), TODAY.plusDays(4), TODAY, true, true));
        assertThat(HabitCoachNotices.forHabit(habit, TODAY)).isEmpty();
    }

    @Test
    void forHabit_notScheduledToday_returnsEmpty() {
        HabitResponse habit = habit("Gym", HabitPeriodStatus.builder()
                .scheduledToday(false)
                .completedThisPeriod(false)
                .build());
        assertThat(HabitCoachNotices.forHabit(habit, TODAY)).isEmpty();
    }

    @Test
    void forHabit_missingPeriodStatus_returnsEmpty() {
        HabitResponse habit = habit("Stretch", null);
        assertThat(HabitCoachNotices.forHabit(habit, TODAY)).isEmpty();
    }

    @Test
    void forHabit_inactiveHabit_returnsEmpty() {
        HabitResponse habit = HabitResponse.builder()
                .habitId(1L).title("Meditate").frequency(HabitFrequency.DAILY)
                .currentStreak(3).isActive(false)
                .periodStatus(status(TODAY, TODAY, TODAY, true, false))
                .build();
        assertThat(HabitCoachNotices.forHabit(habit, TODAY)).isEmpty();
    }

    @Test
    void forHabit_midPeriodNotDueSoon_returnsEmpty() {
        // Period opened three days ago and is not due for another five — nothing to say yet.
        HabitResponse habit = habit("Deep clean",
                status(TODAY.minusDays(3), TODAY.plusDays(10), TODAY.plusDays(5), true, false));
        assertThat(HabitCoachNotices.forHabit(habit, TODAY)).isEmpty();
    }

    // ── nudges ────────────────────────────────────────────────────────────────

    @Test
    void forHabit_dueToday_returnsNoticeMentioningToday() {
        HabitResponse habit = habit("Run", status(TODAY, TODAY, TODAY, true, false));

        Optional<String> notice = HabitCoachNotices.forHabit(habit, TODAY);

        assertThat(notice).isPresent();
        assertThat(notice.get()).contains("Run").containsIgnoringCase("today");
    }

    @Test
    void forHabit_dueTomorrow_returnsNoticeMentioningTomorrow() {
        HabitResponse habit = habit("Weekly review",
                status(TODAY.minusDays(2), TODAY.plusDays(4), TODAY.plusDays(1), true, false));

        Optional<String> notice = HabitCoachNotices.forHabit(habit, TODAY);

        assertThat(notice).isPresent();
        assertThat(notice.get()).contains("Weekly review").containsIgnoringCase("tomorrow");
    }

    @Test
    void forHabit_onPeriodStartDay_returnsNotice() {
        // Period opens today but isn't due for four days — the "new period" reminder.
        HabitResponse habit = habit("Weekly review",
                status(TODAY, TODAY.plusDays(6), TODAY.plusDays(4), true, false));

        Optional<String> notice = HabitCoachNotices.forHabit(habit, TODAY);

        assertThat(notice).isPresent();
        assertThat(notice.get()).contains("Weekly review");
    }

    @Test
    void forHabit_oneDayLate_returnsNoticeStatingDaysLate() {
        HabitResponse habit = habit("Weekly review",
                status(TODAY.minusDays(2), TODAY.plusDays(4), TODAY.minusDays(1), true, false));

        Optional<String> notice = HabitCoachNotices.forHabit(habit, TODAY);

        assertThat(notice).isPresent();
        assertThat(notice.get()).contains("Weekly review").contains("1");
    }

    @Test
    void forHabit_threeDaysLate_wordingEscalatesBeyondOneDayLate() {
        HabitResponse oneDay = habit("Weekly review",
                status(TODAY.minusDays(4), TODAY.plusDays(2), TODAY.minusDays(1), true, false));
        HabitResponse threeDays = habit("Weekly review",
                status(TODAY.minusDays(4), TODAY.plusDays(2), TODAY.minusDays(3), true, false));

        String oneDayNotice = HabitCoachNotices.forHabit(oneDay, TODAY).orElseThrow();
        String threeDayNotice = HabitCoachNotices.forHabit(threeDays, TODAY).orElseThrow();

        assertThat(threeDayNotice).contains("3").isNotEqualTo(oneDayNotice);
    }

    @Test
    void forHabit_lateOnLastDayOfPeriod_wordingDiffersFromMidPeriodLateness() {
        HabitResponse midPeriod = habit("Weekly review",
                status(TODAY.minusDays(4), TODAY.plusDays(2), TODAY.minusDays(1), true, false));
        HabitResponse lastDay = habit("Weekly review",
                status(TODAY.minusDays(6), TODAY, TODAY.minusDays(1), true, false));

        String midNotice = HabitCoachNotices.forHabit(midPeriod, TODAY).orElseThrow();
        String lastDayNotice = HabitCoachNotices.forHabit(lastDay, TODAY).orElseThrow();

        assertThat(lastDayNotice).isNotEqualTo(midNotice);
    }

    @Test
    void forHabit_weeklyHabitDueToday_producesNotice() {
        // Regression guard for the original bug: only DAILY habits ever reached the briefing.
        HabitResponse habit = HabitResponse.builder()
                .habitId(1L).title("Weekly run").frequency(HabitFrequency.WEEKLY)
                .currentStreak(2).isActive(true)
                .periodStatus(status(TODAY.minusDays(2), TODAY.plusDays(4), TODAY, true, false))
                .build();

        assertThat(HabitCoachNotices.forHabit(habit, TODAY)).isPresent();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static HabitResponse habit(String title, HabitPeriodStatus periodStatus) {
        return HabitResponse.builder()
                .habitId(1L)
                .title(title)
                .frequency(HabitFrequency.WEEKLY)
                .currentStreak(2)
                .isActive(true)
                .periodStatus(periodStatus)
                .build();
    }

    private static HabitPeriodStatus status(LocalDate periodStart, LocalDate periodEnd, LocalDate dueDate,
                                            boolean scheduledToday, boolean completedThisPeriod) {
        int daysUntilDue = (int) java.time.temporal.ChronoUnit.DAYS.between(TODAY, dueDate);
        return HabitPeriodStatus.builder()
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .dueDate(dueDate)
                .scheduledToday(scheduledToday)
                .completedThisPeriod(completedThisPeriod)
                .daysUntilDue(daysUntilDue)
                .daysLate(Math.max(0, -daysUntilDue))
                .build();
    }
}
