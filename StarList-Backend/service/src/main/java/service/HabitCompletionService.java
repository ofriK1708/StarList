package service;

import java.time.LocalDate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import repository.api.HabitCompletionRepository;
import repository.entity.HabitCompletionEntity;
import repository.entity.HabitEntity;
import repository.entity.UserEntity;

@Slf4j
@Service
public class HabitCompletionService {

    private final HabitCompletionRepository habitCompletionRepository;

    public HabitCompletionService(HabitCompletionRepository habitCompletionRepository) {
        this.habitCompletionRepository = habitCompletionRepository;
    }

    /** Returns true if the habit already has a completion record for today. */
    public boolean existsToday(Long habitId) {
        return habitCompletionRepository.existsByHabit_IdAndCompletedDate(habitId, LocalDate.now());
    }

    /** Persists a new {@link HabitCompletionEntity} for the given habit and user. */
    public void record(HabitEntity habit, UserEntity user, LocalDate date, int coinsEarned, int streak) {
        log.info("Recording completion for habit {} on {}", habit.getId(), date);
        log.debug("Completion detail: habit={}, user={}, streak={}, coinsEarned={}", habit.getId(), user.getId(), streak, coinsEarned);
        habitCompletionRepository.save(
                HabitCompletionEntity.builder()
                        .habit(habit)
                        .user(user)
                        .completedDate(date)
                        .coinsEarned(coinsEarned)
                        .streakAtCompletion(streak)
                        .build());
    }
}
