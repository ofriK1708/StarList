package repository.api;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import repository.entity.HabitCompletionEntity;

public interface HabitCompletionRepository extends JpaRepository<HabitCompletionEntity, Long> {

    boolean existsByHabit_IdAndCompletedDate(Long habitId, LocalDate completedDate);

    /**
     * Returns all completion records for the given habits within an inclusive date range.
     * Used to compute month-level completion status for one or many habits in a single query.
     */
    List<HabitCompletionEntity> findAllByHabit_IdInAndCompletedDateBetween(
            List<Long> habitIds, LocalDate start, LocalDate end);
}
