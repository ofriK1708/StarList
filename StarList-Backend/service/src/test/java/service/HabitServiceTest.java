package service;

import model.enums.DifficultyLevel;
import model.enums.HabitFrequency;
import model.enums.ReferenceType;
import model.enums.TransactionType;
import java.time.DayOfWeek;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.api.HabitRepository;
import repository.entity.HabitEntity;
import repository.entity.UserEntity;
import repository.mapper.HabitMapper;
import service.dto.MarkHabitDoneResponse;
import service.exceptions.HabitAlreadyCompletedTodayException;
import service.exceptions.HabitNotFoundException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import model.domain.Habit;
import model.enums.CompletionStatus;
import service.dto.HabitResponse;

import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HabitServiceTest {

    @Mock HabitRepository habitRepository;
    @Mock HabitCompletionService habitCompletionService;
    @Mock CoinTransactionService coinTransactionService;
    @Mock AchievementService achievementService;
    @Mock UserService userService;
    @Mock HabitMapper habitMapper;
    @Mock CoinCalculator coinCalculator;
    @Mock HabitPeriodCalculator habitPeriodCalculator;

    @InjectMocks HabitService habitService;

    // ── completeHabit happy path — streak starts from scratch ────────────────

    @Test
    void completeHabit_noPriorCompletion_streakStartsAtOne() {
        UserEntity user = UserEntity.builder().id(1L).totalCoins(0).lifetimeCoinsEarned(0).build();
        HabitEntity habit = HabitEntity.builder()
                .id(7L)
                .title("Read daily")
                .difficultyLevel(DifficultyLevel.EASY)
                .frequency(HabitFrequency.DAILY)
                .currentStreak(0)
                .bestStreak(0)
                .totalCompletions(0)
                .lastCompletedDate(null) // never completed before
                .user(user)
                .build();

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        when(habitRepository.findById(7L)).thenReturn(Optional.of(habit));
        when(habitPeriodCalculator.currentPeriod(habit, today)).thenReturn(new LocalDate[]{today, today});
        when(habitCompletionService.existsToday(7L)).thenReturn(false);
        when(coinCalculator.computeHabitCompletionReward(DifficultyLevel.EASY, 1)).thenReturn(10);

        MarkHabitDoneResponse response = habitService.completeHabit(7L, null);

        assertThat(response.habitId()).isEqualTo(7L);
        assertThat(response.currentStreak()).isEqualTo(1);
        assertThat(response.bestStreak()).isEqualTo(1);
        assertThat(response.coinsEarned()).isEqualTo(10);

        assertThat(habit.getCurrentStreak()).isEqualTo(1);
        assertThat(habit.getBestStreak()).isEqualTo(1);
        assertThat(habit.getTotalCompletions()).isEqualTo(1);
        assertThat(habit.getLastCompletedDate()).isEqualTo(LocalDate.now(ZoneOffset.UTC));
    }

    @Test
    void completeHabit_completedYesterday_streakContinues() {
        UserEntity user = UserEntity.builder().id(1L).totalCoins(0).lifetimeCoinsEarned(0).build();
        HabitEntity habit = HabitEntity.builder()
                .id(7L)
                .title("Read daily")
                .difficultyLevel(DifficultyLevel.MEDIUM)
                .frequency(HabitFrequency.DAILY)
                .currentStreak(5)
                .bestStreak(5)
                .totalCompletions(5)
                .lastCompletedDate(LocalDate.now(ZoneOffset.UTC).minusDays(1)) // completed yesterday
                .user(user)
                .build();

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        when(habitRepository.findById(7L)).thenReturn(Optional.of(habit));
        when(habitPeriodCalculator.currentPeriod(habit, today)).thenReturn(new LocalDate[]{today, today});
        when(habitCompletionService.existsToday(7L)).thenReturn(false);
        when(coinCalculator.computeHabitCompletionReward(DifficultyLevel.MEDIUM, 6)).thenReturn(25);

        MarkHabitDoneResponse response = habitService.completeHabit(7L, null);

        assertThat(response.currentStreak()).isEqualTo(6);
        assertThat(response.bestStreak()).isEqualTo(6); // beats previous best of 5
        assertThat(habit.getTotalCompletions()).isEqualTo(6);
    }

    @Test
    void completeHabit_streakBroken_resetsToOne() {
        UserEntity user = UserEntity.builder().id(1L).totalCoins(0).lifetimeCoinsEarned(0).build();
        HabitEntity habit = HabitEntity.builder()
                .id(7L)
                .title("Read daily")
                .difficultyLevel(DifficultyLevel.EASY)
                .frequency(HabitFrequency.DAILY)
                .currentStreak(10)
                .bestStreak(10)
                .totalCompletions(10)
                .lastCompletedDate(LocalDate.now(ZoneOffset.UTC).minusDays(5)) // missed 4 days
                .user(user)
                .build();

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        when(habitRepository.findById(7L)).thenReturn(Optional.of(habit));
        when(habitPeriodCalculator.currentPeriod(habit, today)).thenReturn(new LocalDate[]{today, today});
        when(habitCompletionService.existsToday(7L)).thenReturn(false);
        when(coinCalculator.computeHabitCompletionReward(DifficultyLevel.EASY, 1)).thenReturn(10);

        MarkHabitDoneResponse response = habitService.completeHabit(7L, null);

        assertThat(response.currentStreak()).isEqualTo(1);
        assertThat(response.bestStreak()).isEqualTo(10); // best streak preserved
    }

    @Test
    void completeHabit_recordsCompletionAndCoinTransaction() {
        UserEntity user = UserEntity.builder().id(1L).totalCoins(50).lifetimeCoinsEarned(0).build();
        HabitEntity habit = HabitEntity.builder()
                .id(7L)
                .title("Read daily")
                .difficultyLevel(DifficultyLevel.EASY)
                .frequency(HabitFrequency.DAILY)
                .currentStreak(0).bestStreak(0).totalCompletions(0)
                .lastCompletedDate(null)
                .user(user)
                .build();

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        when(habitRepository.findById(7L)).thenReturn(Optional.of(habit));
        when(habitPeriodCalculator.currentPeriod(habit, today)).thenReturn(new LocalDate[]{today, today});
        when(habitCompletionService.existsToday(7L)).thenReturn(false);
        when(coinCalculator.computeHabitCompletionReward(DifficultyLevel.EASY, 1)).thenReturn(10);

        habitService.completeHabit(7L, null);

        verify(habitCompletionService).record(eq(habit), eq(user), eq(LocalDate.now(ZoneOffset.UTC)), eq(10), eq(1));
        verify(coinTransactionService).record(
                eq(user), eq(10), eq(TransactionType.HABIT_COMPLETION),
                eq(ReferenceType.HABIT), eq(7L), any(String.class));
        verify(userService).addCoins(user, 10);
    }

    // ── completeHabit sad paths ───────────────────────────────────────────────

    @Test
    void completeHabit_concurrentDuplicate_dbConstraintViolationTranslatedToHabitAlreadyCompletedTodayException() {
        // Simulates the race: existsToday passes (false), but the DB rejects the INSERT due to a unique constraint violation
        UserEntity user = UserEntity.builder().id(1L).totalCoins(0).lifetimeCoinsEarned(0).build();
        HabitEntity habit = HabitEntity.builder()
                .id(7L).title("Read daily").difficultyLevel(DifficultyLevel.EASY)
                .frequency(HabitFrequency.DAILY).currentStreak(0).bestStreak(0)
                .totalCompletions(0).lastCompletedDate(null).user(user).build();

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        when(habitRepository.findById(7L)).thenReturn(Optional.of(habit));
        when(habitPeriodCalculator.currentPeriod(habit, today)).thenReturn(new LocalDate[]{today, today});
        when(habitCompletionService.existsToday(7L)).thenReturn(false);
        when(coinCalculator.computeHabitCompletionReward(DifficultyLevel.EASY, 1)).thenReturn(10);
        doThrow(new DataIntegrityViolationException("unique constraint"))
                .when(habitCompletionService).record(any(), any(), any(), any(int.class), any(int.class));

        assertThatThrownBy(() -> habitService.completeHabit(7L, null))
                .isInstanceOf(HabitAlreadyCompletedTodayException.class);
    }

    @Test
    void completeHabit_concurrentDuplicate_habitEntityNotMutatedOnFailure() {
        // Verifies fail-fast ordering: habit entity must not be saved if the completion record INSERT fails
        UserEntity user = UserEntity.builder().id(1L).totalCoins(0).lifetimeCoinsEarned(0).build();
        HabitEntity habit = HabitEntity.builder()
                .id(7L).title("Read daily").difficultyLevel(DifficultyLevel.EASY)
                .frequency(HabitFrequency.DAILY).currentStreak(3).bestStreak(5)
                .totalCompletions(3).lastCompletedDate(LocalDate.now(ZoneOffset.UTC).minusDays(1)).user(user).build();

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        when(habitRepository.findById(7L)).thenReturn(Optional.of(habit));
        when(habitPeriodCalculator.currentPeriod(habit, today)).thenReturn(new LocalDate[]{today, today});
        when(habitCompletionService.existsToday(7L)).thenReturn(false);
        when(coinCalculator.computeHabitCompletionReward(DifficultyLevel.EASY, 4)).thenReturn(15);
        doThrow(new DataIntegrityViolationException("unique constraint"))
                .when(habitCompletionService).record(any(), any(), any(), any(int.class), any(int.class));

        assertThatThrownBy(() -> habitService.completeHabit(7L, null))
                .isInstanceOf(HabitAlreadyCompletedTodayException.class);

        verify(habitRepository, never()).save(any());
        verifyNoInteractions(coinTransactionService, userService);
        assertThat(habit.getCurrentStreak()).isEqualTo(3); // streak unchanged
        assertThat(habit.getTotalCompletions()).isEqualTo(3); // completions unchanged
    }

    @Test
    void completeHabit_alreadyCompletedToday_throwsHabitAlreadyCompletedTodayException() {
        HabitEntity habit = HabitEntity.builder()
                .id(7L).frequency(HabitFrequency.DAILY).currentStreak(1).bestStreak(1).totalCompletions(1)
                .build();

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        when(habitRepository.findById(7L)).thenReturn(Optional.of(habit));
        when(habitPeriodCalculator.currentPeriod(habit, today)).thenReturn(new LocalDate[]{today, today});
        when(habitCompletionService.existsToday(7L)).thenReturn(true);

        assertThatThrownBy(() -> habitService.completeHabit(7L, null))
                .isInstanceOf(HabitAlreadyCompletedTodayException.class);

        verifyNoInteractions(coinTransactionService, userService);
    }

    @Test
    void completeHabit_habitNotFound_throwsHabitNotFoundException() {
        when(habitRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> habitService.completeHabit(99L, null))
                .isInstanceOf(HabitNotFoundException.class);
    }

    // ── getHabit / monthCompletions ──

    @Test
    void getHabit_daysBeforeCreation_markedNA() {
        YearMonth march2026 = YearMonth.of(2026, 3);
        Instant createdAtDay10 = LocalDate.of(2026, 3, 10).atStartOfDay().toInstant(ZoneOffset.UTC);
        HabitEntity entity = HabitEntity.builder().id(1L).frequency(HabitFrequency.DAILY)
                .createdAt(createdAtDay10).build();
        Habit habit = Habit.builder().id(1L).createdAt(createdAtDay10).build();

        when(habitRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(habitMapper.toDomain(entity)).thenReturn(habit);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(1L), march2026))
                .thenReturn(Map.of());
        when(habitPeriodCalculator.periodsForMonth(entity, march2026)).thenReturn(dailyPeriods(march2026));

        HabitResponse response = habitService.getHabit(1L, march2026, null);

        List<CompletionStatus> mc = response.monthCompletions();
        for (int i = 0; i < 9; i++) {
            assertThat(mc.get(i)).as("day %d should be NA (before creation)", i + 1)
                    .isEqualTo(CompletionStatus.NA);
        }
    }

    @Test
    void getHabit_pastDayWithCompletion_markedDone() {
        YearMonth march2026 = YearMonth.of(2026, 3);
        Instant createdAtDay1 = LocalDate.of(2026, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
        LocalDate day5 = LocalDate.of(2026, 3, 5);
        HabitEntity entity = HabitEntity.builder().id(1L).frequency(HabitFrequency.DAILY)
                .createdAt(createdAtDay1).build();
        Habit habit = Habit.builder().id(1L).createdAt(createdAtDay1).build();

        when(habitRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(habitMapper.toDomain(entity)).thenReturn(habit);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(1L), march2026))
                .thenReturn(Map.of(1L, Set.of(day5)));
        when(habitPeriodCalculator.periodsForMonth(entity, march2026)).thenReturn(dailyPeriods(march2026));

        HabitResponse response = habitService.getHabit(1L, march2026, null);

        assertThat(response.monthCompletions().get(4)).isEqualTo(CompletionStatus.DONE);
    }

    @Test
    void getHabit_todayWithoutCompletion_markedNA() {
        YearMonth currentMonth = YearMonth.now();
        Instant createdAtDay1 = currentMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        HabitEntity entity = HabitEntity.builder().id(1L).frequency(HabitFrequency.DAILY)
                .createdAt(createdAtDay1).build();
        Habit habit = Habit.builder().id(1L).createdAt(createdAtDay1).build();

        when(habitRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(habitMapper.toDomain(entity)).thenReturn(habit);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(1L), currentMonth))
                .thenReturn(Map.of());
        when(habitPeriodCalculator.periodsForMonth(entity, currentMonth)).thenReturn(dailyPeriods(currentMonth));

        HabitResponse response = habitService.getHabit(1L, currentMonth, null);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        int todayIndex = today.getDayOfMonth() - 1;
        assertThat(response.monthCompletions().get(todayIndex)).isEqualTo(CompletionStatus.NA);
    }

    @Test
    void getHabit_todayWithCompletion_markedDone(){
        YearMonth currentMonth = YearMonth.now();
        Instant createdAtDay1 = currentMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        HabitEntity entity = HabitEntity.builder().id(1L).frequency(HabitFrequency.DAILY)
                .createdAt(createdAtDay1).build();
        Habit habit = Habit.builder().id(1L).createdAt(createdAtDay1).build();

        when(habitRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(habitMapper.toDomain(entity)).thenReturn(habit);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(1L), currentMonth))
                .thenReturn(Map.of(1L, Set.of(today)));
        when(habitPeriodCalculator.periodsForMonth(entity, currentMonth)).thenReturn(dailyPeriods(currentMonth));

        HabitResponse response = habitService.getHabit(1L, currentMonth, null);

        int todayIndex = today.getDayOfMonth() - 1;
        assertThat(response.monthCompletions().get(todayIndex)).isEqualTo(CompletionStatus.DONE);
    }

    @Test
    void getHabit_pastDayWithoutCompletion_markedMissed() {
        YearMonth currentMonth = YearMonth.now();
        Instant createdAtDay1 = currentMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        HabitEntity entity = HabitEntity.builder().id(1L).frequency(HabitFrequency.DAILY)
                .createdAt(createdAtDay1).build();
        Habit habit = Habit.builder().id(1L).createdAt(createdAtDay1).build();

        when(habitRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(habitMapper.toDomain(entity)).thenReturn(habit);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(1L), currentMonth))
                .thenReturn(Map.of());
        when(habitPeriodCalculator.periodsForMonth(entity, currentMonth)).thenReturn(dailyPeriods(currentMonth));

        HabitResponse response = habitService.getHabit(1L, currentMonth, null);

        List<CompletionStatus> mc = response.monthCompletions();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
            if (currentMonth.atDay(day).isBefore(today)) {
                assertThat(mc.get(day - 1)).as("day %d should be MISSED", day)
                        .isEqualTo(CompletionStatus.MISSED);
            }
        }
    }

    @Test
    void getHabit_todayAndFutureDays_markedNA() {
        YearMonth currentMonth = YearMonth.now();
        Instant createdAtDay1 = currentMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        HabitEntity entity = HabitEntity.builder().id(1L).frequency(HabitFrequency.DAILY)
                .createdAt(createdAtDay1).build();
        Habit habit = Habit.builder().id(1L).createdAt(createdAtDay1).build();

        when(habitRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(habitMapper.toDomain(entity)).thenReturn(habit);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(1L), currentMonth))
                .thenReturn(Map.of());
        when(habitPeriodCalculator.periodsForMonth(entity, currentMonth)).thenReturn(dailyPeriods(currentMonth));

        HabitResponse response = habitService.getHabit(1L, currentMonth, null);

        List<CompletionStatus> mc = response.monthCompletions();
        int todayIndex = LocalDate.now(ZoneOffset.UTC).getDayOfMonth() - 1;
        for (int i = todayIndex; i < mc.size(); i++) {
            assertThat(mc.get(i)).as("day %d should be NA (today/future)", i + 1)
                    .isEqualTo(CompletionStatus.NA);
        }
    }

    @Test
    void getHabit_habitCreatedToday_allDaysNA() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant createdAtToday = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        HabitEntity entity = HabitEntity.builder().id(1L).frequency(HabitFrequency.DAILY)
                .createdAt(createdAtToday).build();
        Habit habit = Habit.builder().id(1L).createdAt(createdAtToday).build();

        when(habitRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(habitMapper.toDomain(entity)).thenReturn(habit);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(1L), currentMonth))
                .thenReturn(Map.of());
        when(habitPeriodCalculator.periodsForMonth(entity, currentMonth)).thenReturn(dailyPeriods(currentMonth));

        HabitResponse response = habitService.getHabit(1L, currentMonth, null);

        assertThat(response.monthCompletions())
                .hasSize(currentMonth.lengthOfMonth())
                .allMatch(s -> s == CompletionStatus.NA);
    }

    @Test
    void getHabit_arrayLength_matchesMonthDays() {
        YearMonth feb2026 = YearMonth.of(2026, 2);
        Instant createdAtDay1 = LocalDate.of(2026, 2, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
        HabitEntity entity = HabitEntity.builder().id(1L).frequency(HabitFrequency.DAILY)
                .createdAt(createdAtDay1).build();
        Habit habit = Habit.builder().id(1L).createdAt(createdAtDay1).build();

        when(habitRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(habitMapper.toDomain(entity)).thenReturn(habit);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(1L), feb2026))
                .thenReturn(Map.of());
        when(habitPeriodCalculator.periodsForMonth(entity, feb2026)).thenReturn(dailyPeriods(feb2026));

        HabitResponse response = habitService.getHabit(1L, feb2026, null);

        assertThat(response.monthCompletions()).hasSize(28);
    }

    @Test
    void getUserHabits_multipleHabits_eachGetsIndependentCompletions() {
        YearMonth march2026 = YearMonth.of(2026, 3);
        Instant createdAtDay1 = LocalDate.of(2026, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
        LocalDate day3 = LocalDate.of(2026, 3, 3);
        LocalDate day7 = LocalDate.of(2026, 3, 7);
        UserEntity user = UserEntity.builder().id(1L).build();
        HabitEntity entity1 = HabitEntity.builder().id(10L).user(user).frequency(HabitFrequency.DAILY)
                .createdAt(createdAtDay1).build();
        HabitEntity entity2 = HabitEntity.builder().id(20L).user(user).frequency(HabitFrequency.DAILY)
                .createdAt(createdAtDay1).build();
        Habit habit1 = Habit.builder().id(10L).createdAt(createdAtDay1).build();
        Habit habit2 = Habit.builder().id(20L).createdAt(createdAtDay1).build();

        when(habitRepository.findAllByUser_IdAndDeletedAtIsNull(1L))
                .thenReturn(List.of(entity1, entity2));
        when(habitMapper.toDomain(entity1)).thenReturn(habit1);
        when(habitMapper.toDomain(entity2)).thenReturn(habit2);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(10L, 20L), march2026))
                .thenReturn(Map.of(10L, Set.of(day3), 20L, Set.of(day7)));
        when(habitPeriodCalculator.periodsForMonth(entity1, march2026)).thenReturn(dailyPeriods(march2026));
        when(habitPeriodCalculator.periodsForMonth(entity2, march2026)).thenReturn(dailyPeriods(march2026));

        List<HabitResponse> responses = habitService.getUserHabits(1L, march2026, null);

        HabitResponse r1 = responses.stream().filter(r -> r.habitId().equals(10L)).findFirst().orElseThrow();
        HabitResponse r2 = responses.stream().filter(r -> r.habitId().equals(20L)).findFirst().orElseThrow();

        assertThat(r1.monthCompletions().get(2)).isEqualTo(CompletionStatus.DONE);   // day 3 completed for habit1
        assertThat(r1.monthCompletions().get(6)).isEqualTo(CompletionStatus.MISSED); // day 7 not completed for habit1
        assertThat(r2.monthCompletions().get(6)).isEqualTo(CompletionStatus.DONE);   // day 7 completed for habit2
        assertThat(r2.monthCompletions().get(2)).isEqualTo(CompletionStatus.MISSED); // day 3 not completed for habit2
    }

    // ── periodStatus ──────────────────────────────────────────────────────────

    @Test
    void getUserHabits_weeklyPastDueDate_periodStatusReportsDaysLate() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        YearMonth month = YearMonth.from(today);
        LocalDate periodStart = today.with(DayOfWeek.MONDAY);
        LocalDate periodEnd = periodStart.plusDays(6);
        LocalDate due = today.minusDays(2);

        UserEntity user = UserEntity.builder().id(1L).build();
        HabitEntity entity = HabitEntity.builder().id(10L).user(user)
                .frequency(HabitFrequency.WEEKLY).scheduledDayOfWeek(1)
                .createdAt(periodStart.atStartOfDay().toInstant(ZoneOffset.UTC)).build();
        Habit habit = Habit.builder().id(10L).createdAt(entity.getCreatedAt()).build();

        when(habitRepository.findAllByUser_IdAndDeletedAtIsNull(1L)).thenReturn(List.of(entity));
        when(habitMapper.toDomain(entity)).thenReturn(habit);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(10L), month)).thenReturn(Map.of());
        when(habitPeriodCalculator.periodsForMonth(entity, month)).thenReturn(List.of());
        when(habitPeriodCalculator.currentPeriod(entity, today))
                .thenReturn(new LocalDate[]{periodStart, periodEnd});
        when(habitPeriodCalculator.dueDate(entity, today)).thenReturn(due);

        HabitResponse response = habitService.getUserHabits(1L, month, null).get(0);

        assertThat(response.periodStatus().scheduledToday()).isTrue();
        assertThat(response.periodStatus().completedThisPeriod()).isFalse();
        assertThat(response.periodStatus().dueDate()).isEqualTo(due);
        assertThat(response.periodStatus().daysLate()).isEqualTo(2);
        assertThat(response.periodStatus().daysUntilDue()).isEqualTo(-2);
    }

    @Test
    void getUserHabits_multiDayOffScheduledDay_periodStatusNotScheduledToday() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        YearMonth month = YearMonth.from(today);

        UserEntity user = UserEntity.builder().id(1L).build();
        HabitEntity entity = HabitEntity.builder().id(10L).user(user)
                .frequency(HabitFrequency.MULTI_DAY).scheduledDaysOfWeek(List.of(1, 3))
                .createdAt(today.atStartOfDay().toInstant(ZoneOffset.UTC)).build();
        Habit habit = Habit.builder().id(10L).createdAt(entity.getCreatedAt()).build();

        when(habitRepository.findAllByUser_IdAndDeletedAtIsNull(1L)).thenReturn(List.of(entity));
        when(habitMapper.toDomain(entity)).thenReturn(habit);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(10L), month)).thenReturn(Map.of());
        when(habitPeriodCalculator.periodsForMonth(entity, month)).thenReturn(List.of());
        when(habitPeriodCalculator.currentPeriod(entity, today)).thenReturn(null); // off-schedule today

        HabitResponse response = habitService.getUserHabits(1L, month, null).get(0);

        assertThat(response.periodStatus().scheduledToday()).isFalse();
        assertThat(response.periodStatus().periodStart()).isNull();
        assertThat(response.periodStatus().daysLate()).isZero();
    }

    @Test
    void getUserHabits_dailyCompletedToday_periodStatusCompletedThisPeriod() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        YearMonth month = YearMonth.from(today);

        UserEntity user = UserEntity.builder().id(1L).build();
        HabitEntity entity = HabitEntity.builder().id(10L).user(user)
                .frequency(HabitFrequency.DAILY)
                .createdAt(today.atStartOfDay().toInstant(ZoneOffset.UTC)).build();
        Habit habit = Habit.builder().id(10L).createdAt(entity.getCreatedAt()).build();

        when(habitRepository.findAllByUser_IdAndDeletedAtIsNull(1L)).thenReturn(List.of(entity));
        when(habitMapper.toDomain(entity)).thenReturn(habit);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(10L), month))
                .thenReturn(Map.of(10L, Set.of(today)));
        when(habitPeriodCalculator.periodsForMonth(entity, month)).thenReturn(List.of());
        when(habitPeriodCalculator.currentPeriod(entity, today)).thenReturn(new LocalDate[]{today, today});
        when(habitPeriodCalculator.dueDate(entity, today)).thenReturn(today);

        HabitResponse response = habitService.getUserHabits(1L, month, null).get(0);

        assertThat(response.periodStatus().completedThisPeriod()).isTrue();
        assertThat(response.periodStatus().daysUntilDue()).isZero();
        assertThat(response.periodStatus().daysLate()).isZero();
    }

    @Test
    void completeHabit_softDeletedHabit_throwsHabitNotFoundException() {
        HabitEntity deleted = HabitEntity.builder()
                .id(7L)
                .deletedAt(java.time.Instant.now())
                .build();

        when(habitRepository.findById(7L)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> habitService.completeHabit(7L, null))
                .isInstanceOf(HabitNotFoundException.class);
    }

    // ── completeHabit timezone ────────────────────────────────────────────────

    @Test
    void completeHabit_userTimezoneAheadOfUtc_usesUserLocalDate() {
        // Kiritimati is UTC+14 — the furthest-forward zone, so for 10 hours of every UTC day
        // its calendar date is already tomorrow. The completion must be dated in the user's zone.
        LocalDate localToday = LocalDate.now(ZoneId.of("Pacific/Kiritimati"));

        UserEntity user = UserEntity.builder().id(1L).totalCoins(0).lifetimeCoinsEarned(0).build();
        HabitEntity habit = HabitEntity.builder()
                .id(7L).title("Read daily").difficultyLevel(DifficultyLevel.EASY)
                .frequency(HabitFrequency.DAILY)
                .currentStreak(0).bestStreak(0).totalCompletions(0)
                .lastCompletedDate(null).user(user).build();

        when(habitRepository.findById(7L)).thenReturn(Optional.of(habit));
        when(habitPeriodCalculator.currentPeriod(habit, localToday))
                .thenReturn(new LocalDate[]{localToday, localToday});
        when(habitCompletionService.existsToday(7L)).thenReturn(false);
        when(coinCalculator.computeHabitCompletionReward(DifficultyLevel.EASY, 1)).thenReturn(10);

        habitService.completeHabit(7L, "Pacific/Kiritimati");

        assertThat(habit.getLastCompletedDate()).isEqualTo(localToday);
        verify(habitCompletionService).record(eq(habit), eq(user), eq(localToday), anyInt(), anyInt());
    }

    // ── completeHabit WEEKLY ──────────────────────────────────────────────────

    @Test
    void completeHabit_weekly_onTime_fullReward() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate periodStart = today.with(DayOfWeek.MONDAY);
        LocalDate periodEnd = periodStart.plusDays(6);

        UserEntity user = UserEntity.builder().id(1L).totalCoins(0).lifetimeCoinsEarned(0).build();
        HabitEntity habit = HabitEntity.builder()
                .id(10L).title("Weekly run").difficultyLevel(DifficultyLevel.MEDIUM)
                .frequency(HabitFrequency.WEEKLY).scheduledDayOfWeek(5) // Friday
                .currentStreak(0).bestStreak(0).totalCompletions(0)
                .lastCompletedDate(null).user(user).build();

        when(habitRepository.findById(10L)).thenReturn(Optional.of(habit));
        when(habitPeriodCalculator.currentPeriod(habit, today)).thenReturn(new LocalDate[]{periodStart, periodEnd});
        when(habitCompletionService.existsForPeriod(10L, periodStart, periodEnd)).thenReturn(false);
        when(coinCalculator.computeHabitCompletionReward(DifficultyLevel.MEDIUM, 1)).thenReturn(20);

        MarkHabitDoneResponse response = habitService.completeHabit(10L, null);

        assertThat(response.coinsEarned()).isEqualTo(20); // no penalty
        assertThat(response.currentStreak()).isEqualTo(1);
    }

    @Test
    void completeHabit_weekly_completedAfterDueDay_streakResetsToOne() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate periodStart = today.with(DayOfWeek.MONDAY);
        LocalDate periodEnd = periodStart.plusDays(6);

        UserEntity user = UserEntity.builder().id(1L).totalCoins(0).lifetimeCoinsEarned(0).build();
        HabitEntity habit = HabitEntity.builder()
                .id(10L).title("Weekly run").difficultyLevel(DifficultyLevel.MEDIUM)
                .frequency(HabitFrequency.WEEKLY).scheduledDayOfWeek(1) // Monday
                .currentStreak(4).bestStreak(4).totalCompletions(4)
                // Completed in the previous ISO week, so isStreakContinued would otherwise extend it.
                .lastCompletedDate(periodStart.minusDays(3))
                .user(user).build();

        when(habitRepository.findById(10L)).thenReturn(Optional.of(habit));
        when(habitPeriodCalculator.currentPeriod(habit, today)).thenReturn(new LocalDate[]{periodStart, periodEnd});
        when(habitCompletionService.existsForPeriod(10L, periodStart, periodEnd)).thenReturn(false);
        when(habitPeriodCalculator.daysLate(habit, today)).thenReturn(2);
        when(coinCalculator.computeHabitCompletionReward(DifficultyLevel.MEDIUM, 1)).thenReturn(20);

        MarkHabitDoneResponse response = habitService.completeHabit(10L, null);

        assertThat(response.currentStreak()).isEqualTo(1);
        assertThat(habit.getCurrentStreak()).isEqualTo(1);
    }

    @Test
    void completeHabit_weekly_completedAfterDueDay_noCoinsDeducted() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate periodStart = today.with(DayOfWeek.MONDAY);
        LocalDate periodEnd = periodStart.plusDays(6);

        UserEntity user = UserEntity.builder().id(1L).totalCoins(0).lifetimeCoinsEarned(0).build();
        HabitEntity habit = HabitEntity.builder()
                .id(10L).title("Weekly run").difficultyLevel(DifficultyLevel.MEDIUM)
                .frequency(HabitFrequency.WEEKLY).scheduledDayOfWeek(3) // Wednesday
                .coinPenalty(999) // legacy column — must be ignored entirely
                .currentStreak(0).bestStreak(0).totalCompletions(0)
                .lastCompletedDate(null).user(user).build();

        when(habitRepository.findById(10L)).thenReturn(Optional.of(habit));
        when(habitPeriodCalculator.currentPeriod(habit, today)).thenReturn(new LocalDate[]{periodStart, periodEnd});
        when(habitCompletionService.existsForPeriod(10L, periodStart, periodEnd)).thenReturn(false);
        when(habitPeriodCalculator.daysLate(habit, today)).thenReturn(4);
        when(coinCalculator.computeHabitCompletionReward(DifficultyLevel.MEDIUM, 1)).thenReturn(20);

        MarkHabitDoneResponse response = habitService.completeHabit(10L, null);

        assertThat(response.coinsEarned()).isEqualTo(20); // full reward, nothing deducted
        verify(coinTransactionService).record(eq(user), eq(20), eq(TransactionType.HABIT_COMPLETION),
                eq(ReferenceType.HABIT), eq(10L), any());
        verify(userService).addCoins(user, 20);
        verify(userService, never()).spendCoins(any(), anyInt());
    }

    @Test
    void completeHabit_weekly_alreadyDoneThisPeriod_throws() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate periodStart = today.with(DayOfWeek.MONDAY);
        LocalDate periodEnd = periodStart.plusDays(6);

        HabitEntity habit = HabitEntity.builder()
                .id(10L).frequency(HabitFrequency.WEEKLY).currentStreak(1).bestStreak(1).totalCompletions(1).build();

        when(habitRepository.findById(10L)).thenReturn(Optional.of(habit));
        when(habitPeriodCalculator.currentPeriod(habit, today)).thenReturn(new LocalDate[]{periodStart, periodEnd});
        when(habitCompletionService.existsForPeriod(10L, periodStart, periodEnd)).thenReturn(true);

        assertThatThrownBy(() -> habitService.completeHabit(10L, null))
                .isInstanceOf(HabitAlreadyCompletedTodayException.class);
        verifyNoInteractions(coinTransactionService, userService);
    }

    // ── getHabit WEEKLY / CUSTOM monthCompletions ────────────────────────────

    @Test
    void getHabit_weekly_segmentCountEqualsOccurrencesOfDayInMonth() {
        // Mondays in August 2026: 3, 10, 17, 24, 31 → 5 periods
        YearMonth aug2026 = YearMonth.of(2026, 8);
        Instant createdAt = LocalDate.of(2026, 8, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
        HabitEntity entity = HabitEntity.builder().id(1L).frequency(HabitFrequency.WEEKLY)
                .scheduledDayOfWeek(1).createdAt(createdAt).build();
        Habit habit = Habit.builder().id(1L).createdAt(createdAt).frequency(HabitFrequency.WEEKLY).build();

        // Periods: Aug 3-9, Aug 10-16, Aug 17-23, Aug 24-30, Aug 31-Sep 6
        List<LocalDate[]> weeklyPeriods = List.of(
                new LocalDate[]{LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 9)},
                new LocalDate[]{LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 16)},
                new LocalDate[]{LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 23)},
                new LocalDate[]{LocalDate.of(2026, 8, 24), LocalDate.of(2026, 8, 30)},
                new LocalDate[]{LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 6)}
        );

        when(habitRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(habitMapper.toDomain(entity)).thenReturn(habit);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(1L), aug2026))
                .thenReturn(Map.of(1L, Set.of(LocalDate.of(2026, 8, 10)))); // completed on 2nd Monday
        when(habitPeriodCalculator.periodsForMonth(entity, aug2026)).thenReturn(weeklyPeriods);

        HabitResponse response = habitService.getHabit(1L, aug2026, null);

        assertThat(response.monthCompletions()).hasSize(5);
        // Period 0 (Aug 3-9): no completion, ended in past → MISSED
        assertThat(response.monthCompletions().get(0)).isEqualTo(CompletionStatus.MISSED);
        // Period 1 (Aug 10-16): completed Aug 10 → DONE
        assertThat(response.monthCompletions().get(1)).isEqualTo(CompletionStatus.DONE);
    }

    @Test
    void getHabit_custom_30day_twoSegmentsInAugust() {
        YearMonth aug2026 = YearMonth.of(2026, 8);
        Instant createdAt = LocalDate.of(2026, 8, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
        HabitEntity entity = HabitEntity.builder().id(1L).frequency(HabitFrequency.CUSTOM)
                .customIntervalDays(30).createdAt(createdAt).build();
        Habit habit = Habit.builder().id(1L).createdAt(createdAt).frequency(HabitFrequency.CUSTOM).build();

        List<LocalDate[]> customPeriods = List.of(
                new LocalDate[]{LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 30)},
                new LocalDate[]{LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 29)}
        );

        when(habitRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(habitMapper.toDomain(entity)).thenReturn(habit);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(1L), aug2026))
                .thenReturn(Map.of(1L, Set.of(LocalDate.of(2026, 8, 15)))); // completed in first period
        when(habitPeriodCalculator.periodsForMonth(entity, aug2026)).thenReturn(customPeriods);

        HabitResponse response = habitService.getHabit(1L, aug2026, null);

        assertThat(response.monthCompletions()).hasSize(2);
        // Period 0 (Aug 1–30): completed Aug 15 → DONE
        assertThat(response.monthCompletions().get(0)).isEqualTo(CompletionStatus.DONE);
        // Period 1 (Aug 31–Sep 29): ongoing → NA
        assertThat(response.monthCompletions().get(1)).isEqualTo(CompletionStatus.NA);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Generates one [day, day] period per calendar day in the month, mirroring DAILY frequency logic. */
    private List<LocalDate[]> dailyPeriods(YearMonth yearMonth) {
        List<LocalDate[]> periods = new ArrayList<>(yearMonth.lengthOfMonth());
        for (int d = 1; d <= yearMonth.lengthOfMonth(); d++) {
            LocalDate date = yearMonth.atDay(d);
            periods.add(new LocalDate[]{date, date});
        }
        return periods;
    }

    @Test
    void completeHabit_custom_withinPeriod_allowed() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate periodStart = today.withDayOfMonth(1);
        LocalDate periodEnd = periodStart.plusDays(29);

        UserEntity user = UserEntity.builder().id(1L).totalCoins(0).lifetimeCoinsEarned(0).build();
        HabitEntity habit = HabitEntity.builder()
                .id(20L).title("Monthly review").difficultyLevel(DifficultyLevel.HARD)
                .frequency(HabitFrequency.CUSTOM).customIntervalDays(30)
                .currentStreak(0).bestStreak(0).totalCompletions(0)
                .lastCompletedDate(null).user(user).build();

        when(habitRepository.findById(20L)).thenReturn(Optional.of(habit));
        when(habitPeriodCalculator.currentPeriod(habit, today)).thenReturn(new LocalDate[]{periodStart, periodEnd});
        when(habitCompletionService.existsForPeriod(20L, periodStart, periodEnd)).thenReturn(false);
        when(coinCalculator.computeHabitCompletionReward(DifficultyLevel.HARD, 1)).thenReturn(40);

        MarkHabitDoneResponse response = habitService.completeHabit(20L, null);

        assertThat(response.coinsEarned()).isEqualTo(40);
        assertThat(response.currentStreak()).isEqualTo(1);
    }
}
