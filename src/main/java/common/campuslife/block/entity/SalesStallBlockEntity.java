package common.campuslife.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 销售摊位BlockEntity。
 *
 * 这是玩家赚钱的上游：
 * - 上方1格：放要卖的产品
 * - 中间1格：收取硬币（留给LC ATM逻辑）
 * - 显示：当前价格、累计收入
 *
 * 玩家放产品+右键打开GUI设置价格 -> NPC自动过来购买
 */
public class SalesStallBlockEntity extends BlockEntity implements Container, MenuProvider {

    // 1格：产品展示/出售
    private ItemStack productSlot = ItemStack.EMPTY;
    private int price = 10; // 默认10硬币
    private int totalSales = 0;
    private int totalRevenue = 0;
    @Nullable
    private UUID ownerUUID = null;
    private String ownerName = "";
    @Nullable
    private UUID npcBuyingCooldown = null; // 当前正在购买的NPC，防止重复

    public SalesStallBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SALES_STALL.get(), pos, state);
    }

    // === 核心逻辑 ===

    /**
     * 尝试让NPC购买。
     * @return true if purchase succeeded
     */
    public boolean tryPurchase(ServerLevel level) {
        if (this.productSlot.isEmpty()) return false;
        if (level.getGameTime() % 100 != 0) return false; // 限制频率

        // 检查附近是否有NPC玩家（模拟购买）
        // 简单实现：直接减少产品，增加收入
        // 实际应该检查NPC实体距离+意愿

        // 暂时用简化的销售逻辑：每100tick有概率卖出
        if (level.random.nextFloat() < 0.3f) {
            // 卖出
            this.productSlot.shrink(1);
            this.totalSales++;
            this.totalRevenue += this.price;
            this.setChanged();
            return true;
        }
        return false;
    }

    /**
     * 玩家手动销售（右键NPC或靠近时卖出）。
     */
    public boolean manualSell(Player player) {
        if (this.productSlot.isEmpty()) return false;
        this.productSlot.shrink(1);
        this.totalSales++;
        this.totalRevenue += this.price;
        this.setChanged();
        return true;
    }

    // === Container接口 ===

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
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.literal("销售摊位");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return common.campuslife.menu.SalesStallMenu.SALES_STALL != null
            ? new common.campuslife.menu.SalesStallMenu(containerId, playerInventory, this)
            : null;
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
        this.ownerUUID = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        this.ownerName = tag.getString("ownerName");
    }

    // === Getters/Setters ===

    public ItemStack getProduct() { return productSlot; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = Math.max(1, price); setChanged(); }
    public int getTotalSales() { return totalSales; }
    public int getTotalRevenue() { return totalRevenue; }
    @Nullable
    public UUID getOwnerUUID() { return ownerUUID; }
    public void setOwnerUUID(@Nullable UUID uuid) { this.ownerUUID = uuid; setChanged(); }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String name) { this.ownerName = name; setChanged(); }
}
