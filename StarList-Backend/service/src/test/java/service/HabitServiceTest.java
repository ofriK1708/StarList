package service;

import model.enums.DifficultyLevel;
import model.enums.HabitFrequency;
import model.enums.ReferenceType;
import model.enums.TransactionType;
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
import java.time.YearMonth;
import java.time.ZoneOffset;
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
    @Mock UserService userService;
    @Mock HabitMapper habitMapper;
    @Mock CoinCalculator coinCalculator;

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

        when(habitRepository.findById(7L)).thenReturn(Optional.of(habit));
        when(habitCompletionService.existsToday(7L)).thenReturn(false);
        when(coinCalculator.computeHabitCompletionReward(DifficultyLevel.EASY, 1)).thenReturn(10);

        MarkHabitDoneResponse response = habitService.completeHabit(7L);

        assertThat(response.habitId()).isEqualTo(7L);
        assertThat(response.currentStreak()).isEqualTo(1);
        assertThat(response.bestStreak()).isEqualTo(1);
        assertThat(response.coinsEarned()).isEqualTo(10);

        assertThat(habit.getCurrentStreak()).isEqualTo(1);
        assertThat(habit.getBestStreak()).isEqualTo(1);
        assertThat(habit.getTotalCompletions()).isEqualTo(1);
        assertThat(habit.getLastCompletedDate()).isEqualTo(LocalDate.now());
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
                .lastCompletedDate(LocalDate.now().minusDays(1)) // completed yesterday
                .user(user)
                .build();

        when(habitRepository.findById(7L)).thenReturn(Optional.of(habit));
        when(habitCompletionService.existsToday(7L)).thenReturn(false);
        when(coinCalculator.computeHabitCompletionReward(DifficultyLevel.MEDIUM, 6)).thenReturn(25);

        MarkHabitDoneResponse response = habitService.completeHabit(7L);

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
                .lastCompletedDate(LocalDate.now().minusDays(5)) // missed 4 days
                .user(user)
                .build();

        when(habitRepository.findById(7L)).thenReturn(Optional.of(habit));
        when(habitCompletionService.existsToday(7L)).thenReturn(false);
        when(coinCalculator.computeHabitCompletionReward(DifficultyLevel.EASY, 1)).thenReturn(10);

        MarkHabitDoneResponse response = habitService.completeHabit(7L);

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

        when(habitRepository.findById(7L)).thenReturn(Optional.of(habit));
        when(habitCompletionService.existsToday(7L)).thenReturn(false);
        when(coinCalculator.computeHabitCompletionReward(DifficultyLevel.EASY, 1)).thenReturn(10);

        habitService.completeHabit(7L);

        verify(habitCompletionService).record(eq(habit), eq(user), eq(LocalDate.now()), eq(10), eq(1));
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

        when(habitRepository.findById(7L)).thenReturn(Optional.of(habit));
        when(habitCompletionService.existsToday(7L)).thenReturn(false);
        when(coinCalculator.computeHabitCompletionReward(DifficultyLevel.EASY, 1)).thenReturn(10);
        doThrow(new DataIntegrityViolationException("unique constraint"))
                .when(habitCompletionService).record(any(), any(), any(), any(int.class), any(int.class));

        assertThatThrownBy(() -> habitService.completeHabit(7L))
                .isInstanceOf(HabitAlreadyCompletedTodayException.class);
    }

    @Test
    void completeHabit_concurrentDuplicate_habitEntityNotMutatedOnFailure() {
        // Verifies fail-fast ordering: habit entity must not be saved if the completion record INSERT fails
        UserEntity user = UserEntity.builder().id(1L).totalCoins(0).lifetimeCoinsEarned(0).build();
        HabitEntity habit = HabitEntity.builder()
                .id(7L).title("Read daily").difficultyLevel(DifficultyLevel.EASY)
                .frequency(HabitFrequency.DAILY).currentStreak(3).bestStreak(5)
                .totalCompletions(3).lastCompletedDate(LocalDate.now().minusDays(1)).user(user).build();

        when(habitRepository.findById(7L)).thenReturn(Optional.of(habit));
        when(habitCompletionService.existsToday(7L)).thenReturn(false);
        when(coinCalculator.computeHabitCompletionReward(DifficultyLevel.EASY, 4)).thenReturn(15);
        doThrow(new DataIntegrityViolationException("unique constraint"))
                .when(habitCompletionService).record(any(), any(), any(), any(int.class), any(int.class));

        assertThatThrownBy(() -> habitService.completeHabit(7L))
                .isInstanceOf(HabitAlreadyCompletedTodayException.class);

        verify(habitRepository, never()).save(any());
        verifyNoInteractions(coinTransactionService, userService);
        assertThat(habit.getCurrentStreak()).isEqualTo(3); // streak unchanged
        assertThat(habit.getTotalCompletions()).isEqualTo(3); // completions unchanged
    }

    @Test
    void completeHabit_alreadyCompletedToday_throwsHabitAlreadyCompletedTodayException() {
        HabitEntity habit = HabitEntity.builder()
                .id(7L).currentStreak(1).bestStreak(1).totalCompletions(1)
                .build();

        when(habitRepository.findById(7L)).thenReturn(Optional.of(habit));
        when(habitCompletionService.existsToday(7L)).thenReturn(true);

        assertThatThrownBy(() -> habitService.completeHabit(7L))
                .isInstanceOf(HabitAlreadyCompletedTodayException.class);

        verifyNoInteractions(coinTransactionService, userService);
    }

    @Test
    void completeHabit_habitNotFound_throwsHabitNotFoundException() {
        when(habitRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> habitService.completeHabit(99L))
                .isInstanceOf(HabitNotFoundException.class);
    }

    // ── getHabit / monthCompletions ──

    @Test
    void getHabit_daysBeforeCreation_markedNA() {
        YearMonth march2026 = YearMonth.of(2026, 3);
        Instant createdAtDay10 = LocalDate.of(2026, 3, 10).atStartOfDay().toInstant(ZoneOffset.UTC);
        HabitEntity entity = HabitEntity.builder().id(1L).build();
        Habit habit = Habit.builder().id(1L).createdAt(createdAtDay10).build();

        when(habitRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(habitMapper.toDomain(entity)).thenReturn(habit);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(1L), march2026))
                .thenReturn(Map.of());

        HabitResponse response = habitService.getHabit(1L, march2026);

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
        HabitEntity entity = HabitEntity.builder().id(1L).build();
        Habit habit = Habit.builder().id(1L).createdAt(createdAtDay1).build();

        when(habitRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(habitMapper.toDomain(entity)).thenReturn(habit);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(1L), march2026))
                .thenReturn(Map.of(1L, Set.of(day5)));

        HabitResponse response = habitService.getHabit(1L, march2026);

        assertThat(response.monthCompletions().get(4)).isEqualTo(CompletionStatus.DONE);
    }

    @Test
    void getHabit_todayWithoutCompletion_markedNA() {
        YearMonth currentMonth = YearMonth.now();
        Instant createdAtDay1 = currentMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        HabitEntity entity = HabitEntity.builder().id(1L).build();
        Habit habit = Habit.builder().id(1L).createdAt(createdAtDay1).build();

        when(habitRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(habitMapper.toDomain(entity)).thenReturn(habit);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(1L), currentMonth))
                .thenReturn(Map.of());

        HabitResponse response = habitService.getHabit(1L, currentMonth);

        LocalDate today = LocalDate.now();
        int todayIndex = today.getDayOfMonth() - 1;
        assertThat(response.monthCompletions().get(todayIndex)).isEqualTo(CompletionStatus.NA);
    }

    @Test
    void getHabit_todayWithCompletion_markedDone(){
        YearMonth currentMonth = YearMonth.now();
        Instant createdAtDay1 = currentMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        LocalDate today = LocalDate.now();
        HabitEntity entity = HabitEntity.builder().id(1L).build();
        Habit habit = Habit.builder().id(1L).createdAt(createdAtDay1).build();

        when(habitRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(habitMapper.toDomain(entity)).thenReturn(habit);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(1L), currentMonth))
                .thenReturn(Map.of(1L, Set.of(today)));

        HabitResponse response = habitService.getHabit(1L, currentMonth);

        int todayIndex = today.getDayOfMonth() - 1;
        assertThat(response.monthCompletions().get(todayIndex)).isEqualTo(CompletionStatus.DONE);
    }

    @Test
    void getHabit_pastDayWithoutCompletion_markedMissed() {
        YearMonth currentMonth = YearMonth.now();
        Instant createdAtDay1 = currentMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        HabitEntity entity = HabitEntity.builder().id(1L).build();
        Habit habit = Habit.builder().id(1L).createdAt(createdAtDay1).build();

        when(habitRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(habitMapper.toDomain(entity)).thenReturn(habit);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(1L), currentMonth))
                .thenReturn(Map.of());

        HabitResponse response = habitService.getHabit(1L, currentMonth);

        List<CompletionStatus> mc = response.monthCompletions();
        LocalDate today = LocalDate.now();
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
        HabitEntity entity = HabitEntity.builder().id(1L).build();
        Habit habit = Habit.builder().id(1L).createdAt(createdAtDay1).build();

        when(habitRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(habitMapper.toDomain(entity)).thenReturn(habit);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(1L), currentMonth))
                .thenReturn(Map.of());

        HabitResponse response = habitService.getHabit(1L, currentMonth);

        List<CompletionStatus> mc = response.monthCompletions();
        int todayIndex = LocalDate.now().getDayOfMonth() - 1;
        for (int i = todayIndex; i < mc.size(); i++) {
            assertThat(mc.get(i)).as("day %d should be NA (today/future)", i + 1)
                    .isEqualTo(CompletionStatus.NA);
        }
    }

    @Test
    void getHabit_habitCreatedToday_allDaysNA() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate today = LocalDate.now();
        Instant createdAtToday = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        HabitEntity entity = HabitEntity.builder().id(1L).build();
        Habit habit = Habit.builder().id(1L).createdAt(createdAtToday).build();

        when(habitRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(habitMapper.toDomain(entity)).thenReturn(habit);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(1L), currentMonth))
                .thenReturn(Map.of());

        HabitResponse response = habitService.getHabit(1L, currentMonth);

        assertThat(response.monthCompletions())
                .hasSize(currentMonth.lengthOfMonth())
                .allMatch(s -> s == CompletionStatus.NA);
    }

    @Test
    void getHabit_arrayLength_matchesMonthDays() {
        YearMonth feb2026 = YearMonth.of(2026, 2);
        Instant createdAtDay1 = LocalDate.of(2026, 2, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
        HabitEntity entity = HabitEntity.builder().id(1L).build();
        Habit habit = Habit.builder().id(1L).createdAt(createdAtDay1).build();

        when(habitRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(habitMapper.toDomain(entity)).thenReturn(habit);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(1L), feb2026))
                .thenReturn(Map.of());

        HabitResponse response = habitService.getHabit(1L, feb2026);

        assertThat(response.monthCompletions()).hasSize(28);
    }

    @Test
    void getUserHabits_multipleHabits_eachGetsIndependentCompletions() {
        YearMonth march2026 = YearMonth.of(2026, 3);
        Instant createdAtDay1 = LocalDate.of(2026, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
        LocalDate day3 = LocalDate.of(2026, 3, 3);
        LocalDate day7 = LocalDate.of(2026, 3, 7);
        UserEntity user = UserEntity.builder().id(1L).build();
        HabitEntity entity1 = HabitEntity.builder().id(10L).user(user).build();
        HabitEntity entity2 = HabitEntity.builder().id(20L).user(user).build();
        Habit habit1 = Habit.builder().id(10L).createdAt(createdAtDay1).build();
        Habit habit2 = Habit.builder().id(20L).createdAt(createdAtDay1).build();

        when(habitRepository.findAllByUser_IdAndDeletedAtIsNull(1L))
                .thenReturn(List.of(entity1, entity2));
        when(habitMapper.toDomain(entity1)).thenReturn(habit1);
        when(habitMapper.toDomain(entity2)).thenReturn(habit2);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(10L, 20L), march2026))
                .thenReturn(Map.of(10L, Set.of(day3), 20L, Set.of(day7)));

        List<HabitResponse> responses = habitService.getUserHabits(1L, march2026);

        HabitResponse r1 = responses.stream().filter(r -> r.habitId().equals(10L)).findFirst().orElseThrow();
        HabitResponse r2 = responses.stream().filter(r -> r.habitId().equals(20L)).findFirst().orElseThrow();

        assertThat(r1.monthCompletions().get(2)).isEqualTo(CompletionStatus.DONE);   // day 3 completed for habit1
        assertThat(r1.monthCompletions().get(6)).isEqualTo(CompletionStatus.MISSED); // day 7 not completed for habit1
        assertThat(r2.monthCompletions().get(6)).isEqualTo(CompletionStatus.DONE);   // day 7 completed for habit2
        assertThat(r2.monthCompletions().get(2)).isEqualTo(CompletionStatus.MISSED); // day 3 not completed for habit2
    }

    @Test
    void completeHabit_softDeletedHabit_throwsHabitNotFoundException() {
        HabitEntity deleted = HabitEntity.builder()
                .id(7L)
                .deletedAt(java.time.Instant.now())
                .build();

        when(habitRepository.findById(7L)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> habitService.completeHabit(7L))
                .isInstanceOf(HabitNotFoundException.class);
    }
}
