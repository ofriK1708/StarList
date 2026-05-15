package controller;

import java.util.List;

import model.domain.GalaxyItem;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import service.StoreService;
import service.UserService;
import service.dto.BuyItemResponse;
import service.dto.ItemCatalogResponse;

@RestController
@RequestMapping("/store")
public class StoreController {

    private final StoreService storeService;
    private final UserService userService;

    public StoreController(StoreService storeService, UserService userService) {
        this.storeService = storeService;
        this.userService = userService;
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
     * @param itemId the ID of the catalog item to purchase
     */
    @PostMapping("/buy/{itemId}")
    public ResponseEntity<BuyItemResponse> buyItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long itemId) {
        Long userId = userService.getUserByCognitoSub(jwt.getSubject()).id();
        return ResponseEntity.status(HttpStatus.CREATED).body(storeService.buyItem(userId, itemId));
    }

    /**
     * Returns all items owned by the authenticated user.
     */
    @GetMapping("/my-items")
    public ResponseEntity<List<GalaxyItem>> getMyItems(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = userService.getUserByCognitoSub(jwt.getSubject()).id();
        return ResponseEntity.ok(storeService.getMyItems(userId));
    }
}
