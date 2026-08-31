package service;

import model.enums.HabitFrequency;
import model.enums.ReferenceType;
import model.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.api.HabitCompletionRepository;
import repository.api.HabitRepository;
import repository.entity.HabitEntity;
import repository.entity.UserEntity;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HabitMissSchedulerTest {

    @Mock HabitRepository habitRepository;
    @Mock HabitCompletionRepository habitCompletionRepository;
    @Mock HabitPeriodCalculator habitPeriodCalculator;
    @Mock CoinTransactionService coinTransactionService;
    @Mock UserService userService;

    @InjectMocks HabitMissScheduler scheduler;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder().id(1L).totalCoins(200).build();
    }

    // ── DAILY ──────────────────────────────────────────────────────────────────

    @Test
    void dailyHabit_missedYesterday_appliesPenalty() {
        HabitEntity habit = dailyHabit(50);
        when(habitRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(habit));
        // No completion for yesterday
        when(habitCompletionRepository.existsByHabit_IdAndCompletedDateBetween(
                eq(habit.getId()), any(), any())).thenReturn(false);

        scheduler.processMisses();

        verify(userService).spendCoins(user, 50);
        verify(coinTransactionService).record(
                eq(user), eq(-50), eq(TransactionType.HABIT_MISS),
                eq(ReferenceType.HABIT), eq(habit.getId()), anyString());
    }

    @Test
    void dailyHabit_completedYesterday_noPenalty() {
        HabitEntity habit = dailyHabit(50);
        when(habitRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(habit));
        when(habitCompletionRepository.existsByHabit_IdAndCompletedDateBetween(
                eq(habit.getId()), any(), any())).thenReturn(true);

        scheduler.processMisses();

        verify(userService, never()).spendCoins(any(), anyInt());
    }

    @Test
    void dailyHabit_noPenaltyConfigured_skipsDeduction() {
        HabitEntity habit = dailyHabit(null); // no penalty set
        when(habitRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(habit));
        when(habitCompletionRepository.existsByHabit_IdAndCompletedDateBetween(
                eq(habit.getId()), any(), any())).thenReturn(false);

        scheduler.processMisses();

        verify(userService, never()).spendCoins(any(), anyInt());
        verify(coinTransactionService, never()).record(any(), anyInt(), any(), any(), any(), any());
    }

    // ── WEEKLY ─────────────────────────────────────────────────────────────────

    @Test
    void weeklyHabit_missedLastWeek_penaltyAppliedOnMonday() {
        // Make "today" a Monday so the weekly expiry check fires
        LocalDate monday = LocalDate.now(ZoneOffset.UTC).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        LocalDate lastMonday = monday.minusDays(7);
        LocalDate lastSunday = monday.minusDays(1);

        HabitEntity habit = weeklyHabit(30, 3); // scheduled Wednesday
        when(habitRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(habit));
        when(habitCompletionRepository.existsByHabit_IdAndCompletedDateBetween(
                habit.getId(), lastMonday, lastSunday)).thenReturn(false);

        // We need to simulate "today" being a Monday. Since LocalDate.now() is called inside
        // processMisses, we use MockedStatic to control the clock.
        try (MockedStatic<LocalDate> mocked = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> LocalDate.now(ZoneOffset.UTC)).thenReturn(monday);
            scheduler.processMisses();
        }

        verify(userService).spendCoins(user, 30);
    }

    @Test
    void weeklyHabit_checkedOnNonMonday_noPenalty() {
        LocalDate wednesday = LocalDate.now(ZoneOffset.UTC).with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));

        HabitEntity habit = weeklyHabit(30, 3);
        when(habitRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(habit));

        try (MockedStatic<LocalDate> mocked = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> LocalDate.now(ZoneOffset.UTC)).thenReturn(wednesday);
            scheduler.processMisses();
        }

        // Weekly check only fires on Monday — no completion check, no penalty
        verify(habitCompletionRepository, never()).existsByHabit_IdAndCompletedDateBetween(any(), any(), any());
        verify(userService, never()).spendCoins(any(), anyInt());
    }

    // ── CUSTOM ─────────────────────────────────────────────────────────────────

    @Test
    void customHabit_periodEndedYesterday_appliesPenalty() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate yesterday = today.minusDays(1);

        HabitEntity habit = customHabit(40, 7); // 7-day interval
        LocalDate periodStart = yesterday.minusDays(6);
        // Period ends yesterday → expired
        when(habitPeriodCalculator.currentPeriod(habit, yesterday))
                .thenReturn(new LocalDate[]{periodStart, yesterday});
        when(habitRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(habit));
        when(habitCompletionRepository.existsByHabit_IdAndCompletedDateBetween(
                habit.getId(), periodStart, yesterday)).thenReturn(false);

        scheduler.processMisses();

        verify(userService).spendCoins(user, 40);
    }

    @Test
    void customHabit_periodEndsInFuture_noPenalty() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate yesterday = today.minusDays(1);

        HabitEntity habit = customHabit(40, 7);
        // Period ends in 3 days — not expired yet
        when(habitPeriodCalculator.currentPeriod(habit, yesterday))
                .thenReturn(new LocalDate[]{yesterday.minusDays(3), yesterday.plusDays(3)});
        when(habitRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(habit));

        scheduler.processMisses();

        verify(userService, never()).spendCoins(any(), anyInt());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private HabitEntity dailyHabit(Integer penalty) {
        return HabitEntity.builder()
                .id(10L).user(user).title("Daily habit")
                .frequency(HabitFrequency.DAILY)
                .coinPenalty(penalty).createdAt(Instant.now()).build();
    }

    private HabitEntity weeklyHabit(int penalty, int scheduledDayOfWeek) {
        return HabitEntity.builder()
                .id(20L).user(user).title("Weekly habit")
                .frequency(HabitFrequency.WEEKLY)
                .scheduledDayOfWeek(scheduledDayOfWeek)
                .coinPenalty(penalty).createdAt(Instant.now()).build();
    }

    private HabitEntity customHabit(int penalty, int intervalDays) {
        return HabitEntity.builder()
                .id(30L).user(user).title("Custom habit")
                .frequency(HabitFrequency.CUSTOM)
                .customIntervalDays(intervalDays)
                .coinPenalty(penalty).createdAt(Instant.now()).build();
    }
}
