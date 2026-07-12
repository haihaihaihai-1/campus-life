package common.campuslife.system;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 支付系统。
 *
 * 发放硬币给玩家。优先尝试LC硬币，不存在则用自带金硬币。
 */
public final class PaymentSystem {

    private PaymentSystem() {}

    /**
     * 给玩家发放指定数量的硬币。
     * 先尝试加到背包，背包满了则掉落在地。
     */
    public static void giveCoins(Player player, int amount) {
        if (amount <= 0) return;

        // 使用自带金硬币（已注册，100%可用）
        // GOLD_COIN = 1个金硬币 = 10硬币单位（1金=10铜）
        int goldCoins = amount / 10;
        int copperCoins = amount % 10;

        Level level = player.level();

        if (goldCoins > 0) {
            ItemStack goldStack = new ItemStack(
                common.cn.kafei.simukraft.registry.ModItems.GOLD_COIN.get(),
                goldCoins
            );
            if (!goldStack.isEmpty()) {
                if (!player.getInventory().add(goldStack)) {
                    level.addFreshEntity(new ItemEntity(level,
                        player.getX(), player.getY(), player.getZ(), goldStack));
                }
            }
        }

        if (copperCoins > 0) {
            ItemStack copperStack = new ItemStack(
                net.minecraft.world.item.Items.GOLD_NUGGET,
                copperCoins
            );
            if (!copperStack.isEmpty()) {
                if (!player.getInventory().add(copperStack)) {
                    level.addFreshEntity(new ItemEntity(level,
                        player.getX(), player.getY(), player.getZ(), copperStack));
                }
            }
        }
    }
}
