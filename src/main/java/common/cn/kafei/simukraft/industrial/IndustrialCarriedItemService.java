package common.cn.kafei.simukraft.industrial;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import common.cn.kafei.simukraft.SimuKraft;
import common.cn.kafei.simukraft.material.GenericContainerAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("null")
public final class IndustrialCarriedItemService {
    private static final String ITEMS_KEY = "items";

    private IndustrialCarriedItemService() {
    }

    public static boolean hasItems(IndustrialBoxData data) {
        return data != null && !data.workState().isBlank();
    }

    public static List<ItemStack> items(IndustrialBoxData data, HolderLookup.Provider registries) {
        if (data == null || data.workState().isBlank()) {
            return List.of();
        }
        try {
            JsonObject root = JsonParser.parseString(data.workState()).getAsJsonObject();
            JsonArray array = root.has(ITEMS_KEY) && root.get(ITEMS_KEY).isJsonArray()
                    ? root.getAsJsonArray(ITEMS_KEY)
                    : new JsonArray();
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                if (!array.get(i).isJsonObject()) {
                    continue;
                }
                ItemStack stack = stackFromJson(array.get(i).getAsJsonObject(), registries);
                if (!stack.isEmpty()) {
                    addStack(items, stack);
                }
            }
            return List.copyOf(items);
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    public static int stackCount(IndustrialBoxData data, HolderLookup.Provider registries) {
        return items(data, registries).size();
    }

    public static boolean addItems(IndustrialBoxManager manager, IndustrialBoxData data, List<ItemStack> additions, HolderLookup.Provider registries) {
        if (data == null || additions == null || additions.isEmpty()) {
            return false;
        }
        List<ItemStack> merged = new ArrayList<>(items(data, registries));
        boolean changed = false;
        for (ItemStack addition : additions) {
            if (addition != null && !addition.isEmpty()) {
                addStack(merged, addition.copy());
                changed = true;
            }
        }
        return changed && setItems(manager, data, merged, registries);
    }

    public static boolean consumeFirstMatching(IndustrialBoxManager manager,
                                              IndustrialBoxData data,
                                              IndustrialItemStackSpec spec,
                                              HolderLookup.Provider registries) {
        return extractFirstMatching(manager, data, spec, registries).isPresent();
    }

    public static java.util.Optional<ItemStack> extractFirstMatching(IndustrialBoxManager manager,
                                                                     IndustrialBoxData data,
                                                                     IndustrialItemStackSpec spec,
                                                                     HolderLookup.Provider registries) {
        if (data == null || spec == null || spec.isEmpty()) {
            return java.util.Optional.empty();
        }
        List<ItemStack> current = new ArrayList<>(items(data, registries));
        for (int i = 0; i < current.size(); i++) {
            ItemStack stack = current.get(i);
            if (!spec.matches(stack, registries)) {
                continue;
            }
            ItemStack extracted = stack.copyWithCount(1);
            stack.shrink(1);
            current.removeIf(ItemStack::isEmpty);
            if (setItems(manager, data, current, registries)) {
                return java.util.Optional.of(extracted);
            }
            return java.util.Optional.empty();
        }
        return java.util.Optional.empty();
    }

    public static DepositResult depositToContainers(ServerLevel level,
                                                    IndustrialBoxManager manager,
                                                    IndustrialBoxData data,
                                                    List<BlockPos> containers) {
        List<ItemStack> current = new ArrayList<>(items(data, level.registryAccess()));
        if (current.isEmpty()) {
            clear(manager, data);
            return DepositResult.SUCCESS;
        }
        if (containers == null || containers.isEmpty()) {
            return DepositResult.MISSING_CONTAINER;
        }
        List<ItemStack> remainingItems = new ArrayList<>();
        for (ItemStack stack : current) {
            ItemStack remaining = stack.copy();
            for (BlockPos container : containers) {
                if (remaining.isEmpty()) {
                    break;
                }
                remaining = GenericContainerAccess.insert(level, container, remaining);
            }
            if (!remaining.isEmpty()) {
                addStack(remainingItems, remaining);
            }
        }
        setItems(manager, data, remainingItems, level.registryAccess());
        return remainingItems.isEmpty() ? DepositResult.SUCCESS : DepositResult.OUTPUT_FULL;
    }

    public static void dropAndClear(ServerLevel level, IndustrialBoxManager manager, IndustrialBoxData data, BlockPos fallbackPos) {
        if (level == null || data == null || fallbackPos == null) {
            return;
        }
        for (ItemStack stack : items(data, level.registryAccess())) {
            if (!stack.isEmpty()) {
                Block.popResource(level, fallbackPos, stack.copy());
            }
        }
        clear(manager, data);
    }

    public static void clear(IndustrialBoxManager manager, IndustrialBoxData data) {
        if (data == null || data.workState().isBlank()) {
            return;
        }
        data.setWorkState("");
        if (manager != null) {
            manager.persist(data);
        }
    }

    private static boolean setItems(IndustrialBoxManager manager, IndustrialBoxData data, List<ItemStack> items, HolderLookup.Provider registries) {
        if (items == null || items.isEmpty()) {
            clear(manager, data);
            return true;
        }
        if (registries == null) {
            return false;
        }
        JsonObject root = new JsonObject();
        JsonArray array = new JsonArray();
        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            Optional<JsonObject> encoded = stackToJson(stack, registries);
            if (encoded.isEmpty()) {
                return false;
            }
            array.add(encoded.get());
        }
        if (array.isEmpty()) {
            clear(manager, data);
            return true;
        }
        root.add(ITEMS_KEY, array);
        data.setWorkState(root.toString());
        if (manager != null) {
            manager.persist(data);
        }
        return true;
    }

    private static Optional<JsonObject> stackToJson(ItemStack stack, HolderLookup.Provider registries) {
        if (stack == null || stack.isEmpty() || registries == null) {
            return Optional.empty();
        }
        try {
            JsonObject object = new JsonObject();
            object.addProperty("nbt", stack.saveOptional(registries).toString());
            return Optional.of(object);
        } catch (RuntimeException exception) {
            SimuKraft.LOGGER.warn("Simukraft: Failed to serialize carried item {}", stack, exception);
            return Optional.empty();
        }
    }

    private static ItemStack stackFromJson(JsonObject object, HolderLookup.Provider registries) {
        if (object == null || registries == null || !object.has("nbt")) {
            return ItemStack.EMPTY;
        }
        try {
            CompoundTag tag = TagParser.parseTag(object.get("nbt").getAsString());
            return ItemStack.parseOptional(registries, tag);
        } catch (Exception exception) {
            return ItemStack.EMPTY;
        }
    }

    private static void addStack(List<ItemStack> items, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (ItemStack existing : items) {
            if (remaining.isEmpty() || !ItemStack.isSameItemSameComponents(existing, remaining)) {
                continue;
            }
            int movable = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
            if (movable > 0) {
                existing.grow(movable);
                remaining.shrink(movable);
            }
        }
        while (!remaining.isEmpty()) {
            int amount = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            items.add(remaining.copyWithCount(amount));
            remaining.shrink(amount);
        }
    }

    public enum DepositResult {
        SUCCESS,
        MISSING_CONTAINER,
        OUTPUT_FULL
    }
}
