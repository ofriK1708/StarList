package repository.mapper;

import model.domain.ItemCatalog;
import org.mapstruct.Mapper;
import repository.entity.ItemCatalogEntity;

@Mapper(componentModel = "spring")
public interface ItemCatalogMapper {

    ItemCatalog toDomain(ItemCatalogEntity entity);

    ItemCatalogEntity fromDomain(ItemCatalog domain);
}
