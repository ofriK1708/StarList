package repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import model.enums.ItemType;
import model.enums.RarityLevel;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "item_catalog", indexes = {
        @Index(name = "idx_item_catalog_type", columnList = "item_type"),
        @Index(name = "idx_item_catalog_available", columnList = "is_available"),
        @Index(name = "idx_item_catalog_unlock_requirement", columnList = "unlock_requirement")
})
public class ItemCatalogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_name", nullable = false, unique = true)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 16)
    private ItemType itemType;

    @Column(name = "description", length = 2000)
    private String description;

    @Builder.Default
    @Column(name = "cost_coins", nullable = false)
    private Integer costCoins = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "rarity", length = 16)
    private RarityLevel rarity;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "position_x", precision = 5, scale = 2)
    private BigDecimal positionX;

    @Column(name = "position_y", precision = 5, scale = 2)
    private BigDecimal positionY;

    @Column(name = "scale", precision = 6, scale = 2)
    private BigDecimal scale;

    @Column(name = "rotation", precision = 6, scale = 2)
    private BigDecimal rotation;

    @Builder.Default
    @Column(name = "unlock_requirement")
    private Integer unlockRequirement = 0;

    @Builder.Default
    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = Boolean.TRUE;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
