package service.dto;

import java.math.BigDecimal;
import lombok.Builder;
import model.domain.ItemCatalog;
import model.enums.ItemType;
import model.enums.RarityLevel;

@Builder
public record ItemCatalogResponse(
        Long id,
        String itemName,
        ItemType itemType,
        String description,
        Integer costCoins,
        RarityLevel rarity,
        String imageUrl,
        BigDecimal positionX,
        BigDecimal positionY,
        BigDecimal scale,
        BigDecimal rotation,
        Integer unlockRequirement,
        Boolean isAvailable
) {
    public static ItemCatalogResponse from(ItemCatalog item) {
        return ItemCatalogResponse.builder()
                .id(item.getId())
                .itemName(item.getItemName())
                .itemType(item.getItemType())
                .description(item.getDescription())
                .costCoins(item.getCostCoins())
                .rarity(item.getRarity())
                .imageUrl(item.getImageUrl())
                .positionX(item.getPositionX())
                .positionY(item.getPositionY())
                .scale(item.getScale())
                .rotation(item.getRotation())
                .unlockRequirement(item.getUnlockRequirement())
                .isAvailable(item.getIsAvailable())
                .build();
    }
}
