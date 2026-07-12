package common.cn.kafei.simukraft.registry;

import common.cn.kafei.simukraft.SimuKraft;
import common.cn.kafei.simukraft.item.ManifestItem;
import common.cn.kafei.simukraft.item.PortableCityCoreItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("null")
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SimuKraft.MOD_ID);

    public static final DeferredHolder<Item, Item> MANIFEST = ITEMS.register("manifest", ManifestItem::new);
    public static final DeferredHolder<Item, Item> PORTABLE_CITY_CORE = ITEMS.register("portable_city_core", PortableCityCoreItem::new);
    public static final DeferredHolder<Item, Item> GOLD_COIN = ITEMS.register("gold_coin", () -> new Item(new Item.Properties()));
    // Campus Life product items
    public static final DeferredHolder<Item, Item> COFFEE_BASIC = ITEMS.register("coffee_basic", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> BREAD_BASIC = ITEMS.register("bread_basic", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> MILK_TEA = ITEMS.register("milk_tea", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> STUDY_NOTES = ITEMS.register("study_notes", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CALCULATOR = ITEMS.register("calculator", () -> new Item(new Item.Properties()));

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
