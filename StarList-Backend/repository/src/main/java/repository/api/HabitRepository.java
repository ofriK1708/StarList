package repository.api;

import java.util.List;
import model.enums.HabitFrequency;
import org.springframework.data.jpa.repository.JpaRepository;
import repository.entity.HabitEntity;

public interface HabitRepository extends JpaRepository<HabitEntity, Long> {

    List<HabitEntity> findAllByUser_IdAndDeletedAtIsNull(Long userId);

    long countByUser_IdAndDeletedAtIsNull(Long userId);

    List<HabitEntity> findAllByUser_IdAndFrequencyAndDeletedAtIsNull(Long userId, HabitFrequency frequency);

    /** Returns all active (non-deleted) habits across all users — used by the miss-penalty scheduler. */
    List<HabitEntity> findAllByDeletedAtIsNull();
}
