package repository.mapper;

import model.domain.GalaxyItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import repository.entity.GalaxyItemEntity;
import repository.entity.ItemCatalogEntity;
import repository.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface GalaxyItemMapper {

    @Mapping(target = "userId",        source = "entity.user.id")
    @Mapping(target = "catalogItemId", source = "entity.catalogItem.id")
    GalaxyItem toDomain(GalaxyItemEntity entity);

    /**
     * Maps a domain {@link GalaxyItem} to a JPA entity, wiring in the owning {@link UserEntity}
     * and {@link ItemCatalogEntity} that cannot be resolved from IDs alone.
     */
    @Mapping(target = "user",        source = "user")
    @Mapping(target = "catalogItem", source = "catalogItem")
    @Mapping(target = "id",          source = "domain.id")
    GalaxyItemEntity fromDomain(GalaxyItem domain, UserEntity user, ItemCatalogEntity catalogItem);
}
