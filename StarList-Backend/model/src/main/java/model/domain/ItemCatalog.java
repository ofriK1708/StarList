package model.domain;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.enums.ItemType;
import model.enums.RarityLevel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemCatalog {

    private Long id;
    private String itemName;
    private ItemType itemType;
    private String description;
    private Integer costCoins;
    private RarityLevel rarity;
    private String imageUrl;
    private BigDecimal positionX;
    private BigDecimal positionY;
    private BigDecimal scale;
    private BigDecimal rotation;
    private Integer unlockRequirement;
    private Boolean isAvailable;
    private Instant createdAt;
}
