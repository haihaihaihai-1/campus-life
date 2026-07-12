package common.campuslife.menu;

import common.campuslife.block.entity.SalesStallBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import common.cn.kafei.simukraft.SimuKraft;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 销售摊位容器菜单。
 * 
 * 布局：
 * - 上方1格：产品槽
 * - 中间4格：价格上下调整（+1/-1/+5/-5）
 * - 下方27格：玩家背包
 */
public class SalesStallMenu extends AbstractContainerMenu {

    private final SalesStallBlockEntity blockEntity;

    public static final DeferredRegister<MenuType<?>> SALES_STALL_MENUS =
        DeferredRegister.create(Registries.MENU, SimuKraft.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<SalesStallMenu>> SALES_STALL =
        SALES_STALL_MENUS.register("sales_stall", () -> IMenuTypeExtension.create(SalesStallMenu::fromNetwork));

    public SalesStallMenu(int containerId, Inventory playerInventory, SalesStallBlockEntity blockEntity) {
        super(SALES_STALL.get(), containerId);
        this.blockEntity = blockEntity;

        // 产品槽 1格
        this.addSlot(new Slot(blockEntity, 0, 80, 20));

        // 玩家背包 3行x9列
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, row * 9 + col + 9, 8 + col * 18, 51 + row * 18));
            }
        }

        // 玩家快捷栏 9格
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 109));
        }
    }

    public static SalesStallMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        var pos = extraData.readBlockPos();
        var be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof SalesStallBlockEntity stall) {
            return new SalesStallMenu(containerId, playerInventory, stall);
        }
        // BlockEntity不存在时创建临时实例防止NPE崩溃
        var level = playerInventory.player.level();
        var state = level.getBlockState(pos);
        var fallback = new SalesStallBlockEntity(pos, state);
        return new SalesStallMenu(containerId, playerInventory, fallback);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack original = slot.getItem().copy();
        ItemStack stack = slot.getItem();

        if (index == 0) {
            // 从产品槽移到玩家背包
            if (!this.moveItemStackTo(stack, 1, 37, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 从玩家背包移到产品槽
            if (!this.moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.stillValid(player);
    }

    public SalesStallBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public static void register(IEventBus modEventBus) {
        SALES_STALL_MENUS.register(modEventBus);
    }
}
