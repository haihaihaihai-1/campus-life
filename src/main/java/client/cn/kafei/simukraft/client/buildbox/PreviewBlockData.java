package client.cn.kafei.simukraft.client.buildbox;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

@OnlyIn(Dist.CLIENT)
public record PreviewBlockData(BlockPos pos, BlockState state, int packedLight) {
}
