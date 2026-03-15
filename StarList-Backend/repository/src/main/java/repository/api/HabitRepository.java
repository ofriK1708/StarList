package repository.api;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import repository.entity.HabitEntity;

public interface HabitRepository extends JpaRepository<HabitEntity, Long> {

    List<HabitEntity> findAllByUser_IdAndDeletedAtIsNull(Long userId);
}
