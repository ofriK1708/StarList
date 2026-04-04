package repository.api;

import org.springframework.data.jpa.repository.JpaRepository;
import repository.entity.GalaxyItemEntity;

public interface GalaxyItemRepository extends JpaRepository<GalaxyItemEntity, Long> {

    boolean existsByUser_IdAndCatalogItem_Id(Long userId, Long catalogItemId);
}
