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

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
