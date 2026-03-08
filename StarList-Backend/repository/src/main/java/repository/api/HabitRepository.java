package repository.api;

import org.springframework.data.jpa.repository.JpaRepository;
import repository.entity.HabitEntity;

public interface HabitRepository extends JpaRepository<HabitEntity, Long> {}
