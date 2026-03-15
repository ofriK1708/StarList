package repository.api;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import repository.entity.HabitCompletionEntity;

public interface HabitCompletionRepository extends JpaRepository<HabitCompletionEntity, Long> {

    boolean existsByHabit_IdAndCompletedDate(Long habitId, LocalDate completedDate);
}
