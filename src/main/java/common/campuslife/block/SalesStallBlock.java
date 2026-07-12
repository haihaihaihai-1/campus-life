package common.campuslife.block;

import common.campuslife.block.entity.SalesStallBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 销售摊位方块。
 *
 * 右键：打开GUI（放产品+设价格）
 * Shift+右键：回收摊位
 */
public class SalesStallBlock extends Block implements EntityBlock {

    public SalesStallBlock() {
        super(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE)
            .strength(1.0F)
            .sound(SoundType.WOOD));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SalesStallBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return type == common.campuslife.block.entity.ModBlockEntities.SALES_STALL.get()
            ? (l, p, s, be) -> SalesStallBlockEntity.tick((net.minecraft.server.level.ServerLevel) l, p, s, (SalesStallBlockEntity) be)
            : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.sidedSuccess(true);
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SalesStallBlockEntity stall)) {
            return InteractionResult.FAIL;
        }

        if (player.isShiftKeyDown()) {
            if (stall.getOwnerUUID() == null || stall.getOwnerUUID().equals(player.getUUID())) {
                level.destroyBlock(pos, true);
                player.displayClientMessage(Component.literal("摊位已回收"), true);
                return InteractionResult.SUCCESS;
            } else {
                player.displayClientMessage(Component.literal("这是别人的摊位"), true);
                return InteractionResult.FAIL;
            }
        }

        if (stall.getOwnerUUID() == null) {
            stall.setOwnerUUID(player.getUUID());
            stall.setOwnerName(player.getScoreboardName());
        }

        player.openMenu(stall, buf -> buf.writeBlockPos(pos));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SalesStallBlockEntity stall) {
                net.minecraft.world.Containers.dropContents(level, pos, stall);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.WOOD_PLACE,
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }
}
