package common.campuslife.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * 创业方块基类。
 * 
 * 所有创业相关方块共享的属性：
 * - 金属材质
 * - 防爆（防止被破坏后丢失游戏进程）
 * - 需要特定工具采集
 */
public abstract class BaseStartupBlock extends Block {

    protected BaseStartupBlock(MapColor color) {
        super(BlockBehaviour.Properties.of()
            .mapColor(color)
            .strength(1.0F, 3600000.0F)
            .sound(SoundType.METAL));
    }
}
