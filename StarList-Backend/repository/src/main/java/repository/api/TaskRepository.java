package repository.api;

import java.time.Instant;
import java.util.List;
import model.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import repository.entity.TaskEntity;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

    List<TaskEntity> findAllByUser_IdAndDeletedAtIsNull(Long userId);

    long countByUser_IdAndDeletedAtIsNull(Long userId);

    long countByUser_IdAndStatusAndDeletedAtIsNull(Long userId, TaskStatus status);

    @Query("SELECT t FROM TaskEntity t WHERE t.user.id = :userId AND t.dueDate >= :from AND t.dueDate < :to AND t.deletedAt IS NULL")
    List<TaskEntity> findDueTodayByUserId(@Param("userId") Long userId, @Param("from") Instant from, @Param("to") Instant to);
}
