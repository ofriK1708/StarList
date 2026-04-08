package service;

import model.domain.GalaxyItem;
import model.domain.ItemCatalog;
import model.enums.ItemType;
import model.enums.RarityLevel;
import model.enums.ReferenceType;
import model.enums.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.api.GalaxyItemRepository;
import repository.api.ItemCatalogRepository;
import repository.entity.GalaxyItemEntity;
import repository.entity.ItemCatalogEntity;
import repository.entity.UserEntity;
import repository.mapper.GalaxyItemMapper;
import repository.mapper.ItemCatalogMapper;
import service.dto.BuyItemResponse;
import service.dto.ItemCatalogResponse;
import service.exceptions.InsufficientCoinsException;
import service.exceptions.ItemAlreadyOwnedException;
import service.exceptions.ItemNotAvailableException;
import service.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock ItemCatalogRepository itemCatalogRepository;
    @Mock GalaxyItemRepository  galaxyItemRepository;
    @Mock ItemCatalogMapper     itemCatalogMapper;
    @Mock GalaxyItemMapper      galaxyItemMapper;
    @Mock UserService           userService;
    @Mock CoinTransactionService coinTransactionService;

    @InjectMocks
    StoreService storeService;

    private static final long USER_ID = 1L;
    private static final long ITEM_ID = 10L;

    // ── getAvailableItems ─────────────────────────────────────────────────────

    @Test
    void getAvailableItems_twoAvailable_returnsMappedResponses() {
        ItemCatalogEntity e1 = catalogEntity(1L, "Planet A", 50, true);
        ItemCatalogEntity e2 = catalogEntity(2L, "Planet B", 100, true);
        ItemCatalog d1 = catalogDomain(1L, "Planet A", 50);
        ItemCatalog d2 = catalogDomain(2L, "Planet B", 100);

        when(itemCatalogRepository.findAllByIsAvailableTrue()).thenReturn(List.of(e1, e2));
        when(itemCatalogMapper.toDomain(e1)).thenReturn(d1);
        when(itemCatalogMapper.toDomain(e2)).thenReturn(d2);

        List<ItemCatalogResponse> result = storeService.getAvailableItems();

        assertThat(result).hasSize(2)
                .extracting(ItemCatalogResponse::itemName)
                .containsExactlyInAnyOrder("Planet A", "Planet B");
    }

    @Test
    void getAvailableItems_noneAvailable_returnsEmptyList() {
        when(itemCatalogRepository.findAllByIsAvailableTrue()).thenReturn(List.of());

        assertThat(storeService.getAvailableItems()).isEmpty();
        verifyNoInteractions(itemCatalogMapper);
    }

    // ── buyItem happy path ────────────────────────────────────────────────────

    @Test
    void buyItem_sufficientCoins_deductsAndCreatesGalaxyItem() {
        UserEntity user            = user(USER_ID, 500);
        ItemCatalogEntity entity   = catalogEntity(ITEM_ID, "Star Alpha", 100, true);
        GalaxyItemEntity saved     = GalaxyItemEntity.builder().id(99L).user(user).catalogItem(entity).build();
        ItemCatalog domain         = catalogDomain(ITEM_ID, "Star Alpha", 100);
        GalaxyItem savedDomain     = GalaxyItem.builder().id(99L).userId(USER_ID).catalogItemId(ITEM_ID).build();

        when(userService.findEntityById(USER_ID)).thenReturn(user);
        when(itemCatalogRepository.findById(ITEM_ID)).thenReturn(Optional.of(entity));
        when(galaxyItemRepository.existsByUser_IdAndCatalogItem_Id(USER_ID, ITEM_ID)).thenReturn(false);
        when(galaxyItemRepository.save(any())).thenReturn(saved);
        when(itemCatalogMapper.toDomain(entity)).thenReturn(domain);
        when(galaxyItemMapper.toDomain(saved)).thenReturn(savedDomain);

        BuyItemResponse response = storeService.buyItem(USER_ID, ITEM_ID);

        assertThat(response.galaxyItemId()).isEqualTo(99L);
        assertThat(response.catalogItemId()).isEqualTo(ITEM_ID);
        assertThat(response.coinsSpent()).isEqualTo(100);

        verify(userService).spendCoins(user, 100);
        verify(coinTransactionService).record(
                eq(user), eq(-100), eq(TransactionType.ITEM_PURCHASE),
                eq(ReferenceType.GALAXY_ITEM), eq(99L), any(String.class));
    }

    // ── buyItem sad paths ─────────────────────────────────────────────────────

    @Test
    void buyItem_itemNotFound_throwsItemNotFoundException() {
        when(userService.findEntityById(USER_ID)).thenReturn(user(USER_ID, 500));
        when(itemCatalogRepository.findById(ITEM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeService.buyItem(USER_ID, ITEM_ID))
                .isInstanceOf(ItemNotFoundException.class);

        verifyNoInteractions(galaxyItemRepository, coinTransactionService);
        verify(userService, never()).spendCoins(any(), anyInt());
    }

    @Test
    void buyItem_itemNotAvailable_throwsItemNotAvailableException() {
        when(userService.findEntityById(USER_ID)).thenReturn(user(USER_ID, 500));
        when(itemCatalogRepository.findById(ITEM_ID)).thenReturn(Optional.of(
                catalogEntity(ITEM_ID, "Hidden Planet", 100, false)));

        assertThatThrownBy(() -> storeService.buyItem(USER_ID, ITEM_ID))
                .isInstanceOf(ItemNotAvailableException.class);

        verifyNoInteractions(galaxyItemRepository, coinTransactionService);
    }

    @Test
    void buyItem_alreadyOwned_throwsItemAlreadyOwnedException() {
        when(userService.findEntityById(USER_ID)).thenReturn(user(USER_ID, 500));
        when(itemCatalogRepository.findById(ITEM_ID)).thenReturn(Optional.of(
                catalogEntity(ITEM_ID, "Star Beta", 100, true)));
        when(galaxyItemRepository.existsByUser_IdAndCatalogItem_Id(USER_ID, ITEM_ID)).thenReturn(true);

        assertThatThrownBy(() -> storeService.buyItem(USER_ID, ITEM_ID))
                .isInstanceOf(ItemAlreadyOwnedException.class);

        verify(galaxyItemRepository, never()).save(any());
        verify(userService, never()).spendCoins(any(), anyInt());
    }

    @Test
    void buyItem_insufficientCoins_throwsInsufficientCoinsException() {
        when(userService.findEntityById(USER_ID)).thenReturn(user(USER_ID, 50)); // only 50 coins
        when(itemCatalogRepository.findById(ITEM_ID)).thenReturn(Optional.of(
                catalogEntity(ITEM_ID, "Expensive Planet", 200, true)));
        when(galaxyItemRepository.existsByUser_IdAndCatalogItem_Id(USER_ID, ITEM_ID)).thenReturn(false);

        assertThatThrownBy(() -> storeService.buyItem(USER_ID, ITEM_ID))
                .isInstanceOf(InsufficientCoinsException.class);

        verify(galaxyItemRepository, never()).save(any());
        verify(userService, never()).spendCoins(any(), anyInt());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private UserEntity user(long id, int coins) {
        return UserEntity.builder().id(id).totalCoins(coins).lifetimeCoinsEarned(coins).build();
    }

    private ItemCatalogEntity catalogEntity(long id, String name, int cost, boolean available) {
        return ItemCatalogEntity.builder()
                .id(id)
                .itemName(name)
                .itemType(ItemType.PLANET)
                .rarity(RarityLevel.COMMON)
                .costCoins(cost)
                .isAvailable(available)
                .build();
    }

    private ItemCatalog catalogDomain(long id, String name, int cost) {
        return ItemCatalog.builder()
                .id(id)
                .itemName(name)
                .itemType(ItemType.PLANET)
                .rarity(RarityLevel.COMMON)
                .costCoins(cost)
                .isAvailable(true)
                .build();
    }
}
