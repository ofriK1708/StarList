package repository.api;

import org.springframework.data.jpa.repository.JpaRepository;
import repository.entity.HabitCompletionEntity;

public interface HabitCompletionRepository extends JpaRepository<HabitCompletionEntity, Long> {}
