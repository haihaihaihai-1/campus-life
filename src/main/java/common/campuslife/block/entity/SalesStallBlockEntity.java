package common.campuslife.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import common.campuslife.system.PaymentSystem;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 销售摊位BlockEntity。
 *
 * 玩家放产品 -> 定时卖出 -> 给owner硬币
 */
public class SalesStallBlockEntity extends BlockEntity implements Container, MenuProvider {

    private ItemStack productSlot = ItemStack.EMPTY;
    private int price = 5; // 默认5硬币
    private int totalSales = 0;
    private int totalRevenue = 0;
    private int pendingCoins = 0; // 离线待发硬币
    @Nullable
    private UUID ownerUUID = null;
    private String ownerName = "";

    // 销售间隔（tick）：每100tick尝试卖出
    private static final int SELL_INTERVAL = 100;
    // 卖出概率
    private static final float SELL_CHANCE = 0.3f;

    public SalesStallBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SALES_STALL.get(), pos, state);
    }

    // === Ticker and Selling ===

    /**
     * 每100tick（5秒）尝试卖出。
     * 只在产品槽不为空、且有owner时才尝试。
     */
    public static void tick(ServerLevel level, BlockPos pos, BlockState state, SalesStallBlockEntity entity) {
        if (entity.productSlot.isEmpty()) return;
        if (entity.ownerUUID == null) return; // 无人摊位不卖
        if (level.getGameTime() % SELL_INTERVAL != 0) return;

        // 价格影响卖出概率：价格越低卖得越快
        // 基础概率30%，价格每增加1降低1%，最低5%
        float chance = Math.max(0.05f, SELL_CHANCE - entity.price * 0.01f);

        if (level.random.nextFloat() < chance) {
            ItemStack sold = entity.productSlot.copy();
            entity.productSlot.shrink(1);
            if (entity.productSlot.isEmpty()) entity.productSlot = ItemStack.EMPTY;
            entity.totalSales++;
            entity.totalRevenue += entity.price;

            // 给在线owner硬币
            Player owner = level.getServer().getPlayerList().getPlayer(entity.ownerUUID);
            if (owner != null) {
                PaymentSystem.giveCoins(owner, entity.price);
                owner.displayClientMessage(Component.literal(
                    "§a卖出: " + sold.getHoverName().getString() +
                    " §7x1 | §e+" + entity.price + " 硬币" +
                    " §7| §a总收入: " + entity.totalRevenue), true);
            } else {
                // owner离线，累积待发
                entity.pendingCoins += entity.price;
            }
            entity.setChanged();
        }
    }

    // === Container Interface ===

    @Override
    public int getContainerSize() { return 1; }

    @Override
    public boolean isEmpty() { return productSlot.isEmpty(); }

    @Override
    public ItemStack getItem(int slot) { return productSlot; }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (productSlot.isEmpty()) return ItemStack.EMPTY;
        ItemStack split = productSlot.split(amount);
        if (productSlot.isEmpty()) productSlot = ItemStack.EMPTY;
        setChanged();
        return split;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = productSlot;
        productSlot = ItemStack.EMPTY;
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.productSlot = stack;
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null) return true;
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() { this.productSlot = ItemStack.EMPTY; }

    // === MenuProvider ===

    @Override
    public Component getDisplayName() {
        return Component.literal("销售摊位");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // 发放pending coins
        if (player.getUUID().equals(ownerUUID) && pendingCoins > 0) {
            PaymentSystem.giveCoins(player, pendingCoins);
            player.displayClientMessage(Component.literal("§7补发离线期间收益: " + pendingCoins + " 硬币"), false);
            pendingCoins = 0;
            setChanged();
        }
        return new common.campuslife.menu.SalesStallMenu(containerId, playerInventory, this);
    }

    // === NBT ===

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!productSlot.isEmpty()) {
            tag.put("product", productSlot.saveOptional(registries));
        }
        tag.putInt("price", price);
        tag.putInt("totalSales", totalSales);
        tag.putInt("totalRevenue", totalRevenue);
        tag.putInt("pendingCoins", pendingCoins);
        if (ownerUUID != null) tag.putUUID("owner", ownerUUID);
        tag.putString("ownerName", ownerName);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.productSlot = tag.contains("product")
            ? ItemStack.parseOptional(registries, tag.getCompound("product"))
            : ItemStack.EMPTY;
        this.price = tag.getInt("price");
        this.totalSales = tag.getInt("totalSales");
        this.totalRevenue = tag.getInt("totalRevenue");
        this.pendingCoins = tag.getInt("pendingCoins");
        this.ownerUUID = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        this.ownerName = tag.getString("ownerName");
    }

    // === Getters/Setters ===

    public ItemStack getProduct() { return productSlot; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = Math.max(1, Math.min(999, price)); setChanged(); }
    public int getTotalSales() { return totalSales; }
    public int getTotalRevenue() { return totalRevenue; }
    public int getPendingCoins() { return pendingCoins; }
    @Nullable
    public UUID getOwnerUUID() { return ownerUUID; }
    public void setOwnerUUID(@Nullable UUID uuid) { this.ownerUUID = uuid; setChanged(); }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String name) { this.ownerName = name; setChanged(); }
}
