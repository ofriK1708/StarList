package app.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    // Meaningful names and short descriptions keyed by original generic name.
    private static final Map<String, String[]> CATALOG_DETAILS = Map.ofEntries(
        // Stars
        Map.entry("Star 1",    new String[]{ "Solara",    "Ancient yellow dwarf, steady and enduring." }),
        Map.entry("Star 2",    new String[]{ "Aethon",    "Blazing blue giant, fierce and powerful." }),
        Map.entry("Star 3",    new String[]{ "Veyra",     "Luminous wanderer, guiding explorers home." }),
        Map.entry("Star 4",    new String[]{ "Noctis",    "Crimson subgiant, ancient and formidable." }),
        Map.entry("Star 5",    new String[]{ "Astralis",  "Legendary giant, crown jewel of the galaxy." }),
        // Planets
        Map.entry("Planet 1",  new String[]{ "Terranova", "A lush world teeming with life." }),
        Map.entry("Planet 2",  new String[]{ "Pyraxis",   "A scorched volcanic world of fire." }),
        Map.entry("Planet 3",  new String[]{ "Glacius",   "A frozen giant in eternal winter." }),
        Map.entry("Planet 4",  new String[]{ "Verdania",  "Covered in endless emerald forests." }),
        Map.entry("Planet 5",  new String[]{ "Caelum",    "A serene world of floating islands." }),
        Map.entry("Planet 6",  new String[]{ "Duskara",   "Shrouded in perpetual twilight." }),
        Map.entry("Planet 7",  new String[]{ "Aqualis",   "An ocean world with no land in sight." }),
        Map.entry("Planet 8",  new String[]{ "Umbros",    "A shadowy world far from any star." }),
        Map.entry("Planet 9",  new String[]{ "Solitus",   "Bathed in golden light all day." }),
        Map.entry("Planet 10", new String[]{ "Ferrum",    "A dense metallic world rich in ore." }),
        Map.entry("Planet 11", new String[]{ "Nimbus",    "Wrapped in endless swirling storms." }),
        Map.entry("Planet 12", new String[]{ "Crestfall", "Where towering mountains never end." }),
        Map.entry("Planet 13", new String[]{ "Miraxis",   "A mirrored world reflecting the cosmos." }),
        Map.entry("Planet 14", new String[]{ "Vortexia",  "Caught in a permanent magnetic storm." }),
        Map.entry("Planet 15", new String[]{ "Aurantis",  "Glowing amber, warmed by twin suns." }),
        Map.entry("Planet 16", new String[]{ "Nebulos",   "Born from the heart of a nebula." }),
        Map.entry("Planet 17", new String[]{ "Crystallus","Its surface shimmers like pure crystal." }),
        Map.entry("Planet 18", new String[]{ "Ignareth",  "A smoldering remnant of a dead star." }),
        Map.entry("Planet 19", new String[]{ "Aethoria",  "Drifting peacefully at the galaxy's edge." }),
        Map.entry("Planet 20", new String[]{ "Solanum",   "The final world, mysterious and ancient." }),
        Map.entry("Planet 21", new String[]{ "Eclipsis",  "Forever caught between light and shadow." }),
        Map.entry("Planet 22", new String[]{ "Novaris",   "A newborn world still cooling from its birth." })
    );

    @Override
    public void run(ApplicationArguments args) {
        if (itemCatalogRepository.count() > 0) {
            log.info("CatalogSeeder: catalog already populated, skipping seed — running name patch");
            patchCatalogDetails();
            return;
        }

        List<ItemCatalogEntity> items = new ArrayList<>();
        items.addAll(buildPlanets());
        items.addAll(buildStars());

        itemCatalogRepository.saveAll(items);
        log.info("CatalogSeeder: seeded {} catalog items", items.size());
        patchCatalogDetails();
    }

    private void patchCatalogDetails() {
        itemCatalogRepository.findAll().forEach(item -> {
            String[] details = CATALOG_DETAILS.get(item.getItemName());
            if (details != null) {
                String originalName = item.getItemName();
                item.setItemName(details[0]);
                item.setDescription(details[1]);
                itemCatalogRepository.save(item);
                log.info("CatalogSeeder: renamed '{}' → '{}'", originalName, details[0]);
            }
        });
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
