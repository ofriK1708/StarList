package controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.StoreService;
import service.dto.BuyItemResponse;
import service.dto.ItemCatalogResponse;

@RestController
@RequestMapping("/api/store")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    /**
     * Returns all catalog items that are currently available for purchase.
     */
    @GetMapping("/items")
    public ResponseEntity<List<ItemCatalogResponse>> getAvailableItems() {
        return ResponseEntity.ok(storeService.getAvailableItems());
    }

    /**
     * Purchases a catalog item for the authenticated user.
     *
     * @param userId the ID of the purchasing user, supplied by the gateway via the {@code X-User-Id} header
     * @param itemId the ID of the catalog item to purchase
     */
    @PostMapping("/buy/{itemId}")
    public ResponseEntity<BuyItemResponse> buyItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long itemId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storeService.buyItem(userId, itemId));
    }
}
