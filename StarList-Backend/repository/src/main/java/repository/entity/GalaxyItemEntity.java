package repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "galaxy_items", indexes = {
        @Index(name = "idx_galaxy_items_user_id", columnList = "user_id"),
        @Index(name = "idx_galaxy_items_catalog_item_id", columnList = "catalog_item_id"),
        @Index(name = "idx_galaxy_items_purchase_date", columnList = "purchase_date")
})
public class GalaxyItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "catalog_item_id", nullable = false)
    private ItemCatalogEntity catalogItem;

    @Builder.Default
    @Column(name = "purchase_date", nullable = false, updatable = false)
    private Instant purchaseDate = Instant.now();

    @Builder.Default
    @Column(name = "times_selected", nullable = false)
    private Integer timesSelected = 1;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;
}
