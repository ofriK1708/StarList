package service.dto;

import java.time.Instant;
import lombok.Builder;
import model.domain.GalaxyItem;
import model.domain.ItemCatalog;

@Builder
public record BuyItemResponse(
        Long galaxyItemId,
        Long catalogItemId,
        String itemName,
        Instant purchaseDate,
        Integer coinsSpent,
        Integer newTotalCoins
) {
    public static BuyItemResponse from(GalaxyItem item, ItemCatalog catalog, int newTotalCoins) {
        return BuyItemResponse.builder()
                .galaxyItemId(item.getId())
                .catalogItemId(catalog.getId())
                .itemName(catalog.getItemName())
                .purchaseDate(item.getPurchaseDate())
                .coinsSpent(catalog.getCostCoins())
                .newTotalCoins(newTotalCoins)
                .build();
    }
}
