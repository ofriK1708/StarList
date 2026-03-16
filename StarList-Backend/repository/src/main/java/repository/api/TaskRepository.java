package repository.api;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import repository.entity.TaskEntity;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

    List<TaskEntity> findAllByUser_IdAndDeletedAtIsNull(Long userId);
}
