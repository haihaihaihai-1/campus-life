package common.campuslife.product;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * 产品生产服务。
 * 
 * 核心玩法循环：
 * 1. 玩家在工作台GUI选择产品
 * 2. 服务检查附近箱子的原料
 * 3. 原料足够 -> 开始生产 -> 等待craftTime秒 -> 产出到玩家Inventory或箱子
 * 4. 原料不足 -> 显示缺什么
 */
public final class CraftProductionService {

    private CraftProductionService() {}

    /**
     * 检查容器中是否有足够的原料。
     * 
     * @param container 要检查的容器（如箱子）
     * @param product 要生产的产品
     * @return true 如果原料充足
     */
    public static boolean hasIngredients(Container container, Product product) {
        for (ItemStack required : product.getIngredients()) {
            int needed = required.getCount();
            int found = 0;

            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack slot = container.getItem(i);
                if (slot.is(required.getItem())) {
                    found += slot.getCount();
                }
            }

            if (found < needed) {
                return false;
            }
        }
        return true;
    }

    /**
     * 消耗容器中的原料。
     * 
     * @param container 要消耗的容器
     * @param product 要消耗的产品配方
     * @return true 如果成功消耗（调用前应先检查hasIngredients）
     */
    public static boolean consumeIngredients(Container container, Product product) {
        if (!hasIngredients(container, product)) {
            return false;
        }

        for (ItemStack required : product.getIngredients()) {
            int remaining = required.getCount();

            for (int i = 0; i < container.getContainerSize() && remaining > 0; i++) {
                ItemStack slot = container.getItem(i);
                if (slot.is(required.getItem())) {
                    int toRemove = Math.min(slot.getCount(), remaining);
                    slot.shrink(toRemove);
                    remaining -= toRemove;
                }
            }
        }

        return true;
    }

    /**
     * 产出产品到容器中。
     * 
     * @param container 目标容器
     * @param product 要产出的产品
     * @return 剩余的ItemStack（如果容器满了）
     */
    public static ItemStack produceItem(Container container, Product product) {
        // 创建产出物品（简化版，产出为纸，带NBT标记产品名称）
        ItemStack result = new ItemStack(net.minecraft.world.item.Items.PAPER);
        result.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
            net.minecraft.network.chat.Component.literal(product.getName()));
        
        // 添加到容器
        return addToContainer(container, result);
    }

    /**
     * 尝试将物品添加到容器。
     */
    private static ItemStack addToContainer(Container container, ItemStack stack) {
        // 先尝试堆叠到已有槽位
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty()) {
                container.setItem(i, stack);
                return ItemStack.EMPTY;
            }
            if (ItemStack.isSameItemSameComponents(slot, stack) && slot.getCount() < slot.getMaxStackSize()) {
                int canAdd = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
                slot.grow(canAdd);
                stack.shrink(canAdd);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        return stack; // 容器满了，返回剩余
    }
}
