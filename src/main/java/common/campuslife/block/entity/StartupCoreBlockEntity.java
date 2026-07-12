package common.campuslife.block.entity;

import common.cn.kafei.simukraft.SimuKraft;
import common.campuslife.product.Product;
import common.campuslife.product.ProductRegistry;
import common.campuslife.startup.BusinessStageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;
import net.neoforged.neoforge.registries.DeferredHolder;

import javax.annotation.Nullable;

/**
 * 创业核心方块的BlockEntity。
 * 
 * 存储以下数据：
 * - 容器（27格，存放原料和产品）
 * - 所有者UUID
 * - 当前生产中的产品ID
 * - 生产进度（剩余tick）
 * - 总生产数量
 * 
 * 这是玩家与创业系统交互的核心入口。
 */
public class StartupCoreBlockEntity extends BlockEntity implements Container, MenuProvider {

    private final ItemStack[] items = new ItemStack[27];
    private UUID ownerUUID = null;
    private String ownerName = "";
    private String currentProductId = "";
    private int craftProgress = 0;
    private int craftTotalTime = 0;
    private int totalCrafted = 0;

    public StartupCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STARTUP_CORE.get(), pos, state);
        for (int i = 0; i < 27; i++) {
            items[i] = ItemStack.EMPTY;
        }
    }

    // === 生产逻辑 ===

    /**
     * 每5tick（0.25秒）更新一次生产进度。
     */
    public static void tick(net.minecraft.server.level.ServerLevel level, BlockPos pos, BlockState state, StartupCoreBlockEntity entity) {
        if (entity.craftProgress > 0) {
            entity.craftProgress--;
            if (entity.craftProgress <= 0) {
                entity.completeCraft();
            }
            entity.setChanged();
        }
    }

    /**
     * 开始生产一个产品。
     */
    public boolean startCraft(String productId, Player player) {
        Product product = ProductRegistry.get(productId);
        if (product == null) return false;

        // 检查原料
        if (!common.campuslife.product.CraftProductionService.hasIngredients(this, product)) {
            player.displayClientMessage(Component.literal("c原料不足！"), true);
            return false;
        }

        // 消耗原料
        common.campuslife.product.CraftProductionService.consumeIngredients(this, product);

        // 开始生产
        this.currentProductId = productId;
        this.craftProgress = product.getCraftTime() * 20; // 秒->tick
        this.craftTotalTime = product.getCraftTime() * 20;
        this.setChanged();

        player.displayClientMessage(Component.literal("a正在生产: " + product.getName()), true);
        return true;
    }

    /**
     * 完成生产，产出产品。
     */
    private void completeCraft() {
        Product product = ProductRegistry.get(currentProductId);
        if (product == null) {
            currentProductId = "";
            return;
        }

        // 创建产出物品（纸+自定义名称，代表产品）
        ItemStack result = new ItemStack(net.minecraft.world.item.Items.PAPER);
        result.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
            Component.literal(product.getName()));

        // 添加到容器
        for (int i = 0; i < 27; i++) {
            if (items[i].isEmpty()) {
                items[i] = result;
                break;
            }
        }

        totalCrafted++;
        currentProductId = "";
        craftTotalTime = 0;
    }

    // === Container接口 ===

    @Override
    public int getContainerSize() { return 27; }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) { return items[slot]; }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ItemStack.EMPTY; // TODO: 实现
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = items[slot];
        items[slot] = ItemStack.EMPTY;
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items[slot] = stack;
        setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null) return true;
        return player.distanceToSqr(getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < 27; i++) items[i] = ItemStack.EMPTY;
    }

    // === MenuProvider接口 ===

    @Override
    public Component getDisplayName() {
        return Component.literal("创业核心");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new common.campuslife.menu.WorkstationMenu(containerId, playerInventory, this);
    }

    // === NBT序列化 ===

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < 27; i++) {
            if (!items[i].isEmpty()) {
                tag.put("item_" + i, items[i].saveOptional(registries));
            }
        }
        if (ownerUUID != null) tag.putUUID("OwnerUUID", ownerUUID);
        tag.putString("OwnerName", ownerName);
        tag.putString("CurrentProduct", currentProductId);
        tag.putInt("CraftProgress", craftProgress);
        tag.putInt("CraftTotal", craftTotalTime);
        tag.putInt("TotalCrafted", totalCrafted);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < 27; i++) {
            String key = "item_" + i;
            if (tag.contains(key)) {
                items[i] = ItemStack.parseOptional(registries, tag.getCompound(key));
            } else {
                items[i] = ItemStack.EMPTY;
            }
        }
        if (tag.hasUUID("OwnerUUID")) ownerUUID = tag.getUUID("OwnerUUID");
        ownerName = tag.getString("OwnerName");
        currentProductId = tag.getString("CurrentProduct");
        craftProgress = tag.getInt("CraftProgress");
        craftTotalTime = tag.getInt("CraftTotal");
        totalCrafted = tag.getInt("TotalCrafted");
    }

    // === Getters ===

    public String getCurrentProductId() { return currentProductId; }
    public int getCraftProgress() { return craftProgress; }
    public int getCraftTotalTime() { return craftTotalTime; }
    public int getTotalCrafted() { return totalCrafted; }

    public float getCraftProgressPercent() {
        if (craftTotalTime <= 0) return 0f;
        return 1f - (float) craftProgress / craftTotalTime;
    }
}
