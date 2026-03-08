package repository.api;

import org.springframework.data.jpa.repository.JpaRepository;
import repository.entity.TaskEntity;

public interface
TaskRepository extends JpaRepository<TaskEntity, Long> {}
