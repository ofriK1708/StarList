package repository.dal;

import model.enums.ItemType;
import model.enums.RarityLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import repository.api.GalaxyItemRepository;
import repository.api.ItemCatalogRepository;
import repository.api.UserRepository;
import repository.entity.GalaxyItemEntity;
import repository.entity.ItemCatalogEntity;
import repository.entity.UserEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class ItemCatalogRepositoryTest {

    @Autowired
    private ItemCatalogRepository itemCatalogRepository;

    @Autowired
    private GalaxyItemRepository galaxyItemRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity savedUser;

    @BeforeEach
    void setUp() {
        savedUser = userRepository.save(UserEntity.builder()
                .email("storeuser@example.com")
                .cognitoUserId("cog-storeuser")
                .displayName("Store User")
                .build());
    }

    // ── findAllByIsAvailableTrue ──────────────────────────────────────────────

    @Test
    void findAllByIsAvailableTrue_allAvailable_returnsAll() {
        itemCatalogRepository.save(item("Planet A", true));
        itemCatalogRepository.save(item("Planet B", true));

        List<ItemCatalogEntity> result = itemCatalogRepository.findAllByIsAvailableTrue();

        assertThat(result).hasSize(2)
                .extracting(ItemCatalogEntity::getItemName)
                .containsExactlyInAnyOrder("Planet A", "Planet B");
    }

    @Test
    void findAllByIsAvailableTrue_someUnavailable_excludesUnavailableItems() {
        itemCatalogRepository.save(item("Available Planet", true));
        itemCatalogRepository.save(item("Unavailable Planet", false));

        List<ItemCatalogEntity> result = itemCatalogRepository.findAllByIsAvailableTrue();

        assertThat(result).hasSize(1)
                .extracting(ItemCatalogEntity::getItemName)
                .containsExactly("Available Planet");
    }

    @Test
    void findAllByIsAvailableTrue_noneAvailable_returnsEmptyList() {
        itemCatalogRepository.save(item("Gone Planet", false));

        List<ItemCatalogEntity> result = itemCatalogRepository.findAllByIsAvailableTrue();

        assertThat(result).isEmpty();
    }

    // ── existsByUser_IdAndCatalogItem_Id ──────────────────────────────────────

    @Test
    void existsByUserIdAndCatalogItemId_ownedItem_returnsTrue() {
        ItemCatalogEntity catalogItem = itemCatalogRepository.save(item("Star Alpha", true));
        galaxyItemRepository.save(GalaxyItemEntity.builder()
                .user(savedUser)
                .catalogItem(catalogItem)
                .build());

        boolean owned = galaxyItemRepository.existsByUser_IdAndCatalogItem_Id(
                savedUser.getId(), catalogItem.getId());

        assertThat(owned).isTrue();
    }

    @Test
    void existsByUserIdAndCatalogItemId_notOwned_returnsFalse() {
        ItemCatalogEntity catalogItem = itemCatalogRepository.save(item("Star Beta", true));

        boolean owned = galaxyItemRepository.existsByUser_IdAndCatalogItem_Id(
                savedUser.getId(), catalogItem.getId());

        assertThat(owned).isFalse();
    }

    @Test
    void existsByUserIdAndCatalogItemId_differentUser_returnsFalse() {
        UserEntity otherUser = userRepository.save(UserEntity.builder()
                .email("other@example.com")
                .cognitoUserId("cog-other")
                .displayName("Other")
                .build());
        ItemCatalogEntity catalogItem = itemCatalogRepository.save(item("Star Gamma", true));
        galaxyItemRepository.save(GalaxyItemEntity.builder()
                .user(otherUser)
                .catalogItem(catalogItem)
                .build());

        // savedUser did not buy it — should return false even though otherUser owns it
        boolean owned = galaxyItemRepository.existsByUser_IdAndCatalogItem_Id(
                savedUser.getId(), catalogItem.getId());

        assertThat(owned).isFalse();
    }

    /** Returns a minimal {@link ItemCatalogEntity} builder pre-filled for tests. */
    private ItemCatalogEntity item(String name, boolean available) {
        return ItemCatalogEntity.builder()
                .itemName(name)
                .itemType(ItemType.PLANET)
                .rarity(RarityLevel.COMMON)
                .costCoins(100)
                .isAvailable(available)
                .build();
    }
}
