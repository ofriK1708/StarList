package service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import model.domain.GalaxyItem;
import model.domain.ItemCatalog;
import model.enums.ReferenceType;
import model.enums.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Slf4j
@Service
public class StoreService {

    private final ItemCatalogRepository itemCatalogRepository;
    private final GalaxyItemRepository galaxyItemRepository;
    private final ItemCatalogMapper itemCatalogMapper;
    private final GalaxyItemMapper galaxyItemMapper;
    private final UserService userService;
    private final CoinTransactionService coinTransactionService;

    public StoreService(ItemCatalogRepository itemCatalogRepository,
                        GalaxyItemRepository galaxyItemRepository,
                        ItemCatalogMapper itemCatalogMapper,
                        GalaxyItemMapper galaxyItemMapper,
                        UserService userService,
                        CoinTransactionService coinTransactionService) {
        this.itemCatalogRepository = itemCatalogRepository;
        this.galaxyItemRepository = galaxyItemRepository;
        this.itemCatalogMapper = itemCatalogMapper;
        this.galaxyItemMapper = galaxyItemMapper;
        this.userService = userService;
        this.coinTransactionService = coinTransactionService;
    }

    /**
     * Returns all catalog items that are currently marked as available for purchase.
     */
    @Transactional(readOnly = true)
    public List<ItemCatalogResponse> getAvailableItems() {
        log.info("Fetching all available catalog items");
        return itemCatalogRepository.findAllByIsAvailableTrue()
                .stream()
                .map(itemCatalogMapper::toDomain)
                .map(ItemCatalogResponse::from)
                .toList();
    }

    /**
     * Returns all items owned by a specific user.
     */
    @Transactional(readOnly = true)
    public List<GalaxyItem> getMyItems(Long userId) {
        log.info("Fetching galaxy items for user {}", userId);
        return galaxyItemRepository.findAllByUser_Id(userId)
                .stream()
                .map(galaxyItemMapper::toDomain)
                .toList();
    }

    /**
     * Purchases a catalog item for the given user: validates the purchase, deducts coins,
     * creates a {@link GalaxyItemEntity}, and records the coin transaction.
     *
     * @throws service.exceptions.UserNotFoundException      if the user does not exist
     * @throws ItemNotFoundException                         if the catalog item does not exist
     * @throws ItemNotAvailableException                     if the item is not currently available
     * @throws ItemAlreadyOwnedException                     if the user already owns this item
     * @throws InsufficientCoinsException                    if the user's balance is too low
     */
    @Transactional
    public BuyItemResponse buyItem(Long userId, Long itemId) {
        log.info("User {} attempting to purchase item {}", userId, itemId);

        UserEntity user = userService.findEntityById(userId);

        ItemCatalogEntity catalogEntity = itemCatalogRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        if (!catalogEntity.getIsAvailable()) {
            throw new ItemNotAvailableException(itemId);
        }
        if (galaxyItemRepository.existsByUser_IdAndCatalogItem_Id(userId, itemId)) {
            throw new ItemAlreadyOwnedException(itemId);
        }
        if (user.getTotalCoins() < catalogEntity.getCostCoins()) {
            throw new InsufficientCoinsException(catalogEntity.getCostCoins(), user.getTotalCoins());
        }

        GalaxyItemEntity savedEntity = galaxyItemRepository.save(
                GalaxyItemEntity.builder()
                        .user(user)
                        .catalogItem(catalogEntity)
                        .build());

        userService.spendCoins(user, catalogEntity.getCostCoins());

        ItemCatalog catalog = itemCatalogMapper.toDomain(catalogEntity);
        GalaxyItem savedItem = galaxyItemMapper.toDomain(savedEntity);

        coinTransactionService.record(
                user,
                -catalogEntity.getCostCoins(),
                TransactionType.ITEM_PURCHASE,
                ReferenceType.GALAXY_ITEM,
                savedEntity.getId(),
                "Purchased: " + catalogEntity.getItemName());

        log.info("User {} purchased item {} (galaxyItemId={})", userId, itemId, savedEntity.getId());
        return BuyItemResponse.from(savedItem, catalog, user.getTotalCoins());
    }
}
