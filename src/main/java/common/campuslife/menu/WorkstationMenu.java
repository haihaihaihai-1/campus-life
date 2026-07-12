package common.campuslife.menu;

import common.campuslife.block.entity.StartupCoreBlockEntity;
import common.campuslife.product.Product;
import common.campuslife.product.ProductRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import common.cn.kafei.simukraft.SimuKraft;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 工作台菜单（服务端容器）。
 * 
 * 玩家右键创业核心方块时打开。
 * 上方3x9=27格为方块内部存储（放原料/产品）。
 * 下方4x9=36格为玩家背包。
 * 
 * 玩家可以在容器中放入原料，然后点击"生产"按钮。
 * 生产完成后产品出现在容器中。
 */
public class WorkstationMenu extends AbstractContainerMenu {

    private final StartupCoreBlockEntity blockEntity;

    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, SimuKraft.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<WorkstationMenu>> WORKSTATION =
        MENUS.register("workstation", () -> createMenuType());

    private static MenuType<WorkstationMenu> createMenuType() {
        return IMenuTypeExtension.create(WorkstationMenu::fromNetwork);
    }

    public WorkstationMenu(int containerId, Inventory playerInventory, StartupCoreBlockEntity blockEntity) {
        super(WORKSTATION.get(), containerId);
        this.blockEntity = blockEntity;

        // 方块内部存储：27格（3行x9列）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(blockEntity, row * 9 + col, 8 + col * 18, 18 + row * 18));
            }
        }

        // 玩家背包：27格（3行x9列）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, row * 9 + col + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // 玩家快捷栏：9格
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    /**
     * 客户端构造函数（从网络包创建）。
     * 加 null 检查防止区块未加载时 NPE。
     */
    public static WorkstationMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        var pos = extraData.readBlockPos();
        var be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof StartupCoreBlockEntity coreEntity) {
            return new WorkstationMenu(containerId, playerInventory, coreEntity);
        }
        // BlockEntity 不存在时返回一个空菜单，防止崩溃
        return new WorkstationMenu(containerId, playerInventory, new StartupCoreBlockEntity(pos, playerInventory.player.level().getBlockState(pos)));
    }

    /**
     * 玩家shift点击时移动物品的逻辑。
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack original = slot.getItem().copy();
        ItemStack stack = slot.getItem();

        if (index < 27) {
            // 从方块容器移到玩家背包
            if (!this.moveItemStackTo(stack, 27, 63, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 从玩家背包移到方块容器
            if (!this.moveItemStackTo(stack, 0, 27, false)) {
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

    /**
     * 玩家是否可以继续使用此菜单。
     */
    @Override
    public boolean stillValid(Player player) {
        return blockEntity.stillValid(player);
    }

    public StartupCoreBlockEntity getBlockEntity() {
        return blockEntity;
    }

    /**
     * 注册菜单类型。
     */
    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
