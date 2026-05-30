package repository.api;

import java.util.List;
import model.enums.AchievementType;
import org.springframework.data.jpa.repository.JpaRepository;
import repository.entity.AchievementEntity;

public interface AchievementRepository extends JpaRepository<AchievementEntity, Long> {

    List<AchievementEntity> findAllByUser_Id(Long userId);

    boolean existsByUser_IdAndType(Long userId, AchievementType type);
}
