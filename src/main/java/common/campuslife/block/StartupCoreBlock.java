package common.campuslife.block;

import common.cn.kafei.simukraft.SimuKraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 创业核心方块。
 * 
 * 玩家放置后解锁"课桌创业"阶段，右键打开工作台GUI。
 * 这是整个游戏循环的起点。
 * 
 * 功能：
 * - 放置：解锁Lv0课桌创业阶段
 * - 右键：打开产品创造GUI
 * - 升级：满足阶段条件后可手动升级
 * - 破坏：需要确认，已绑定玩家的核心受保护（不能直接破坏）
 * 
 * 建筑升级：
 * - 课桌(Lv0) -> 宿舍工作室(Lv1) -> 校园孵化器(Lv2) -> 
 *   独立公司(Lv3) -> 企业总部(Lv4) -> 商业帝国(Lv5)
 */
public final class StartupCoreBlock extends Block {

    public StartupCoreBlock() {
        super(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(1.0F, 3600000.0F) // 防爆
            .sound(SoundType.METAL));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.sidedSuccess(true);
        }

        // TODO: 打开GUI
        // 当前简化版本：发送消息
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("创业核心 - 右键打开产品创造界面"),
            true
        );

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.METAL_PLACE, 
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }
}
