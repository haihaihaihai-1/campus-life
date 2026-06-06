package common.cn.kafei.simukraft.commercial;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommercialDefinitionLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsNewInfiniteFoodOffer() throws Exception {
        CommercialDefinition definition = load("""
                {
                  "id": "snack_shop",
                  "name": "Snack Shop",
                  "job": { "id": "snack_worker", "name": "Snack Worker", "heldItem": "minecraft:bread" },
                  "offers": [
                    {
                      "id": "buy_bread",
                      "visibleTo": "mixed",
                      "cost": [{ "money": 1.0 }],
                      "result": [{ "item": "minecraft:bread", "count": 1 }]
                    }
                  ]
                }
                """);

        CommercialOffer offer = definition.offerById("buy_bread");
        assertNotNull(offer);
        assertTrue(offer.visibleToNpc());
        assertTrue(offer.visibleToPlayer());
        assertNull(offer.stock());
    }

    @Test
    void loadsNewMaterialBackedStock() throws Exception {
        CommercialDefinition definition = load("""
                {
                  "id": "bread_shop",
                  "name": "Bread Shop",
                  "offers": [
                    {
                      "id": "buy_bread",
                      "visibleTo": "npc",
                      "cost": [{ "money": 0.25 }],
                      "result": [{ "item": "minecraft:bread", "count": 1 }],
                      "stock": {
                        "item": "minecraft:bread",
                        "max": 64,
                        "materials": [{ "item": "minecraft:wheat", "count": 3 }]
                      }
                    }
                  ]
                }
                """);

        CommercialOffer offer = definition.offerById("buy_bread");
        assertNotNull(offer);
        assertNotNull(offer.stock());
        assertTrue(offer.stock().materialBacked());
        assertFalse(offer.stock().sqliteBacked());
        assertEquals("minecraft:wheat", offer.stock().materials().getFirst().itemId());
        assertEquals(3, offer.stock().materials().getFirst().count());
    }

    @Test
    void convertsLegacyRequiredMaterials() throws Exception {
        CommercialDefinition definition = load("""
                {
                  "buildingId": "breadShop",
                  "buildingName": "面包店",
                  "jobType": "breadShopOwner",
                  "jobName": "面包店老板",
                  "shopMode": "NPC_SELL",
                  "heldItem": "minecraft:bread",
                  "workTime": { "start": 100, "end": 12000 },
                  "trades": [
                    {
                      "item": "minecraft:bread",
                      "sellPrice": 0.25,
                      "maxStock": 64,
                      "restockAmount": 32,
                      "requiredMaterials": [{ "item": "minecraft:wheat", "count": 3 }]
                    }
                  ],
                  "buyTrades": []
                }
                """);

        assertEquals("breadShop", definition.id());
        assertEquals("breadShopOwner", definition.job().id());
        assertTrue(definition.workTime().openAt(100));
        assertFalse(definition.workTime().openAt(13000));
        CommercialOffer offer = definition.offers().getFirst();
        assertTrue(offer.visibleToNpc());
        assertEquals(0.25D, offer.cost().getFirst().money());
        assertEquals("minecraft:bread", offer.result().getFirst().itemId());
        assertTrue(offer.stock().materialBacked());
        assertEquals("minecraft:wheat", offer.stock().materials().getFirst().itemId());
    }

    @Test
    void infersLegacyCookedFoodMaterialsFromGlobalList() throws Exception {
        CommercialDefinition definition = load("""
                {
                  "buildingId": "seaboatShoping",
                  "buildingName": "海船之商",
                  "shopMode": "MIXED",
                  "requireMaterialsForSale": true,
                  "materials": [
                    { "item": "minecraft:cod", "count": 1 },
                    { "item": "minecraft:salmon", "count": 1 }
                  ],
                  "trades": [
                    { "item": "minecraft:cooked_cod", "sellPrice": 0.5, "maxStock": 64 },
                    { "item": "minecraft:cooked_salmon", "sellPrice": 0.5, "maxStock": 64 }
                  ],
                  "buyTrades": []
                }
                """);

        CommercialOffer cod = definition.offers().get(0);
        CommercialOffer salmon = definition.offers().get(1);
        assertEquals("minecraft:cod", cod.stock().materials().getFirst().itemId());
        assertEquals("minecraft:salmon", salmon.stock().materials().getFirst().itemId());
        assertTrue(cod.visibleToNpc());
        assertTrue(cod.visibleToPlayer());
    }

    @Test
    void configuredFoodSalesUseMaterialBackedStock() throws Exception {
        assertMaterial("breadShop.json", "shop_sells_bread", "minecraft:wheat", 3);
        assertMaterial("fruitShop.json", "shop_sells_melon_slice", "minecraft:melon_slice", 1);
        assertMaterial("fruitShop.json", "shop_sells_pumpkin", "minecraft:pumpkin", 1);
        assertMaterial("meatShop.json", "shop_sells_cooked_beef", "minecraft:beef", 1);
        assertMaterial("meatShop.json", "shop_sells_cooked_porkchop", "minecraft:porkchop", 1);
        assertMaterial("meatStand.json", "shop_sells_cooked_chicken", "minecraft:chicken", 1);
        assertMaterial("seaboatShoping.json", "shop_sells_cooked_cod", "minecraft:cod", 1);
        assertMaterial("seaboatShoping.json", "shop_sells_cooked_salmon", "minecraft:salmon", 1);
        assertMaterial("SimuFriedChicken.json", "shop_sells_cooked_chicken", "minecraft:chicken", 1);
        assertMaterial("SimuFriedChicken.json", "shop_sells_bread", "minecraft:wheat", 3);
        assertMaterial("SimuFriedChicken.json", "shop_sells_baked_potato", "minecraft:potato", 1);

        CommercialOffer cake = load(commercialResource("breadShop.json")).offerById("shop_sells_cake");
        assertNotNull(cake);
        assertNotNull(cake.stock());
        assertFalse(cake.stock().materialBacked());
    }

    private void assertMaterial(String fileName, String offerId, String itemId, int count) throws Exception {
        CommercialOffer offer = load(commercialResource(fileName)).offerById(offerId);
        assertNotNull(offer);
        assertNotNull(offer.stock());
        assertTrue(offer.stock().materialBacked(), offerId);
        assertEquals(itemId, offer.stock().materials().getFirst().itemId());
        assertEquals(count, offer.stock().materials().getFirst().count());
    }

    private Path commercialResource(String fileName) throws Exception {
        var resource = CommercialDefinitionLoaderTest.class.getResource("/assets/simukraft/building/commercial/" + fileName);
        assertNotNull(resource);
        return Path.of(resource.toURI());
    }

    private CommercialDefinition load(String json) throws Exception {
        Path file = tempDir.resolve("commercial_" + System.nanoTime() + ".json");
        Files.writeString(file, json, StandardCharsets.UTF_8);
        return load(file);
    }

    private CommercialDefinition load(Path file) throws Exception {
        CommercialDefinitionLoader.LoadResult result = CommercialDefinitionLoader.load(file);
        assertTrue(result.valid(), () -> "Loader errors: " + result.errors());
        return result.definition();
    }
}
