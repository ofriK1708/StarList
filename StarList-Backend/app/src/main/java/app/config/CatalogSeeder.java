package app.config;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import model.enums.ItemType;
import model.enums.RarityLevel;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import repository.api.ItemCatalogRepository;
import repository.entity.ItemCatalogEntity;

/**
 * Seeds the {@code item_catalog} table with planet and star cosmetics on every startup.
 * Skips seeding if any catalog items already exist, so it is safe to run in all environments.
 *
 * <p>Planet images are served from {@code /images/Plantes/planet{n}.png}.
 * Star images are served from {@code /images/stars/star{n}.png} — place the images
 * under {@code app/src/main/resources/static/images/stars/} before enabling stars.
 */
@Slf4j
@Component
@Order(2) // run after DataInitializer (order 1) when both are active
public class CatalogSeeder implements ApplicationRunner {

    private static final int PLANET_COUNT = 20;
    private static final int STAR_COUNT   = 5;

    private final ItemCatalogRepository itemCatalogRepository;

    public CatalogSeeder(ItemCatalogRepository itemCatalogRepository) {
        this.itemCatalogRepository = itemCatalogRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (itemCatalogRepository.count() > 0) {
            log.info("CatalogSeeder: catalog already populated, skipping");
            return;
        }

        List<ItemCatalogEntity> items = new ArrayList<>();
        items.addAll(buildPlanets());
        items.addAll(buildStars());

        itemCatalogRepository.saveAll(items);
        log.info("CatalogSeeder: seeded {} catalog items", items.size());
    }

    private List<ItemCatalogEntity> buildPlanets() {
        List<ItemCatalogEntity> planets = new ArrayList<>();
        for (int i = 1; i <= PLANET_COUNT; i++) {
            planets.add(ItemCatalogEntity.builder()
                    .itemName("Planet " + i)
                    .itemType(ItemType.PLANET)
                    .description("A mysterious planet waiting to join your galaxy.")
                    .costCoins(planetCost(i))
                    .rarity(planetRarity(i))
                    .imageUrl("/images/Plantes/planet" + i + ".png")
                    .build());
        }
        return planets;
    }

    private List<ItemCatalogEntity> buildStars() {
        List<ItemCatalogEntity> stars = new ArrayList<>();
        for (int i = 1; i <= STAR_COUNT; i++) {
            stars.add(ItemCatalogEntity.builder()
                    .itemName("Star " + i)
                    .itemType(ItemType.STAR)
                    .description("A radiant star to light up your galaxy.")
                    .costCoins(starCost(i))
                    .rarity(starRarity(i))
                    .imageUrl("/images/stars/star" + i + ".png")
                    .build());
        }
        return stars;
    }

    /** Planets 1-5 COMMON, 6-10 RARE, 11-15 EPIC, 16-20 LEGENDARY. */
    private RarityLevel planetRarity(int n) {
        if (n <= 5)  return RarityLevel.COMMON;
        if (n <= 10) return RarityLevel.RARE;
        if (n <= 15) return RarityLevel.EPIC;
        return RarityLevel.LEGENDARY;
    }

    private int planetCost(int n) {
        if (n <= 5)  return 50;
        if (n <= 10) return 150;
        if (n <= 15) return 400;
        return 1000;
    }

    /** Stars 1-2 COMMON, 3-4 RARE, 5 EPIC. */
    private RarityLevel starRarity(int n) {
        if (n <= 2) return RarityLevel.COMMON;
        if (n <= 4) return RarityLevel.RARE;
        return RarityLevel.EPIC;
    }

    private int starCost(int n) {
        if (n <= 2) return 75;
        if (n <= 4) return 200;
        return 500;
    }
}
