package repository.api;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import repository.entity.ItemCatalogEntity;

public interface ItemCatalogRepository extends JpaRepository<ItemCatalogEntity, Long> {

    List<ItemCatalogEntity> findAllByIsAvailableTrue();
}
