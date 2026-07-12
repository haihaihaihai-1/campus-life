package common.campuslife.system;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 支付系统。
 *
 * 发放硬币给玩家。
 * 优先使用LC铜币(COIN_COPPER)和金币(COIN_GOLD)。
 * 如果LC不可用，回退到自带gold_coin + gold_nugget。
 */
public final class PaymentSystem {

    private PaymentSystem() {}

    /**
     * 给玩家发放指定数量的硬币。
     * 1硬币 = 1个铜币；10硬币 = 1个金币。
     * 先尝试加到背包，背包满了则掉落在地。
     */
    public static void giveCoins(Player player, int amount) {
        if (amount <= 0) return;

        Level level = player.level();

        int goldCoins = amount / 10;
        int copperCoins = amount % 10;

        // 尝试使用LC硬币
        ItemStack goldStack = tryCreateLCCoin(goldCoins, true);
        ItemStack copperStack = tryCreateLCCoin(copperCoins, false);

        // LC不可用时回退到自带物品
        if (goldStack == null && goldCoins > 0) {
            goldStack = new ItemStack(common.cn.kafei.simukraft.registry.ModItems.GOLD_COIN.get(), goldCoins);
        }
        if (copperStack == null && copperCoins > 0) {
            copperStack = new ItemStack(net.minecraft.world.item.Items.GOLD_NUGGET, copperCoins);
        }

        if (goldStack != null && !goldStack.isEmpty()) {
            giveItem(player, goldStack);
        }
        if (copperStack != null && !copperStack.isEmpty()) {
            giveItem(player, copperStack);
        }
    }

    /**
     * 尝试创建LC硬币ItemStack。
     * @return LC硬币ItemStack，如果LC不可用返回null
     */
    private static ItemStack tryCreateLCCoin(int count, boolean gold) {
        if (count <= 0) return null;
        try {
            if (gold) {
                return new ItemStack(io.github.lightman314.lightmanscurrency.common.core.ModItems.COIN_GOLD.get(), count);
            } else {
                return new ItemStack(io.github.lightman314.lightmanscurrency.common.core.ModItems.COIN_COPPER.get(), count);
            }
        } catch (Throwable t) {
            // LC不可用，返回null让调用方回退
            return null;
        }
    }

    /**
     * 给玩家物品，背包满则掉落。
     */
    private static void giveItem(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.level().addFreshEntity(new ItemEntity(
                player.level(),
                player.getX(), player.getY() + 0.5, player.getZ(),
                stack
            ));
        }
    }
}
