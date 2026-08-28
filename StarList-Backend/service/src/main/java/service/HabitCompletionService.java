package service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    /** Returns true if the habit has any completion record in the inclusive date range [{@code start}, {@code end}]. */
    public boolean existsForPeriod(Long habitId, LocalDate start, LocalDate end) {
        return habitCompletionRepository.existsByHabit_IdAndCompletedDateBetween(habitId, start, end);
    }

    /**
     * Returns a map of habitId → set of completed dates within the given month.
     * Queries only the supplied habit IDs in a single DB call.
     */
    @Transactional(readOnly = true)
    public Map<Long, Set<LocalDate>> getCompletedDatesForHabits(List<Long> habitIds, YearMonth yearMonth) {
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        return habitCompletionRepository
                .findAllByHabit_IdInAndCompletedDateBetween(habitIds, start, end)
                .stream()
                .collect(Collectors.groupingBy(
                        e -> e.getHabit().getId(),
                        Collectors.mapping(HabitCompletionEntity::getCompletedDate, Collectors.toSet())));
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
