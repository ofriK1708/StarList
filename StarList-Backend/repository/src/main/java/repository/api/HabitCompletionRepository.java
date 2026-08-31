package repository.api;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import repository.entity.HabitCompletionEntity;

public interface HabitCompletionRepository extends JpaRepository<HabitCompletionEntity, Long> {

    boolean existsByHabit_IdAndCompletedDate(Long habitId, LocalDate completedDate);

    /**
     * Returns true if the habit has any completion record with a date
     * in the inclusive range [{@code start}, {@code end}].
     * Used for WEEKLY and CUSTOM period duplicate checks and miss-penalty detection.
     */
    boolean existsByHabit_IdAndCompletedDateBetween(Long habitId, LocalDate start, LocalDate end);

    /**
     * Returns all completion records for the given habits within an inclusive date range.
     * Used to compute month-level completion status for one or many habits in a single query.
     */
    List<HabitCompletionEntity> findAllByHabit_IdInAndCompletedDateBetween(
            List<Long> habitIds, LocalDate start, LocalDate end);
}
